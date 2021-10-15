package be.cytomine.bioformats

/*
 * Cytomine-Bioformats, a wrapper to link Bio-formats with Cytomine.
 * Copyright (C) 2015-2020 cytomine.org
 *
 * This file is highly inspired from loci.formats.tools.ImageConverter.java:
 * Bio-Formats command line tools for reading and converting files
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * https://github.com/ome/bioformats/blob/develop/components/bio-formats-tools/src/loci/formats/tools/ImageConverter.java
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

import loci.common.DataTools
import loci.common.image.IImageScaler
import loci.common.image.SimpleImageScaler
import loci.common.services.DependencyException
import loci.common.services.ServiceException
import loci.common.services.ServiceFactory
import loci.formats.FormatTools
import loci.formats.IFormatReader
import loci.formats.IFormatWriter
import loci.formats.ImageReader
import loci.formats.ImageWriter
import loci.formats.Memoizer
import loci.formats.MetadataTools
import loci.formats.MissingLibraryException
import loci.formats.gui.Index16ColorModel
import loci.formats.in.DynamicMetadataOptions
import loci.formats.meta.DummyMetadata
import loci.formats.meta.IMetadata
import loci.formats.meta.MetadataRetrieve
import loci.formats.meta.MetadataStore
import loci.formats.ome.OMEPyramidStore
import loci.formats.out.OMETiffWriter
import loci.formats.out.TiffWriter
import loci.formats.services.OMEXMLService
import loci.formats.services.OMEXMLServiceImpl
import loci.formats.tiff.IFD
import ome.xml.meta.OMEXMLMetadataRoot
import ome.xml.model.Image
import ome.xml.model.Pixels
import ome.xml.model.enums.DimensionOrder
import ome.xml.model.primitives.PositiveInteger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.image.IndexColorModel

class ImageConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(loci.formats.tools.ImageConverter.class)
    private static final String OUT_PATTERN = "_C%c_Z%z_T%t"

    File input
    List<CytomineFile> outputs = []
    String outputPattern
    String format

    IFormatReader reader
    TiffWriter writer
    String compression = "LZW"
    int tileWidth
    int tileHeight
    int series

    boolean keepOriginalMetadata
    boolean flatten

    int width = 0
    int height = 0
    int xCoordinate = 0
    int yCoordinate = 0
    int pyramidScale = 1, pyramidResolutions = 1
    boolean firstTile = true

    DynamicMetadataOptions options = new DynamicMetadataOptions()
    OMEXMLService service
    MetadataStore store
    HashMap<String, Integer> nextOutputIndex = new HashMap<String, Integer>()

    ImageConverter(File input, File target, Integer series, String compression,
                   Boolean keepOriginalMetadata, Boolean flatten, Integer nPyramidResolutions,
                   Integer pyramidScaleFactor, Integer tileSize = 256) {
        this.input = input
        this.series = series ?: -1
        this.tileWidth = tileSize
        this.tileHeight = tileSize
        this.compression = compression
        this.keepOriginalMetadata = keepOriginalMetadata
        this.flatten = flatten
        this.pyramidResolutions = nPyramidResolutions
        this.pyramidScale = pyramidScaleFactor

        if (target.isDirectory()) {
            String basePath = BioFormatsUtils.removeExtension(input.absolutePath - input.parent)
            this.outputPattern = new File(target, "${basePath}${OUT_PATTERN}.tif").absolutePath
            this.format = "TIFF"
        }
        else {
            this.outputPattern = target.absolutePath
            this.format = "OMETIFF"
        }
    }

    List<CytomineFile> convert() {
        firstTile = true
        outputs.clear()
        setupReader()
        setupWriter()

        int total = 0
        int num = writer.canDoStacks() ? reader.getSeriesCount() : 1
        long read = 0, write = 0
        int first = series == -1 ? 0 : series
        int last = series == -1 ? num : series + 1
        long timeLastLogged = System.currentTimeMillis()
        for (int q = first; q < last; q++) {
            reader.setSeries(q)
            MetadataRetrieve retrieve = store instanceof MetadataRetrieve ?
                    (MetadataRetrieve) store : new DummyMetadata()
            DimensionOrder order = retrieve.getPixelsDimensionOrder(q)
            int sizeC = retrieve.getChannelCount(q)
            int sizeT = retrieve.getPixelsSizeT(q).getValue()
            int sizeZ = retrieve.getPixelsSizeZ(q).getValue()

            // OutputIndex should be reset at the start of a new series
            nextOutputIndex.clear()
            boolean generatePyramid = pyramidResolutions > reader.getResolutionCount()
            int resolutionCount = generatePyramid ? pyramidResolutions : reader.getResolutionCount();
            for (int res = 0; res < resolutionCount; res++) {
                if (!generatePyramid) {
                    reader.setResolution(res);
                }
                firstTile = true

                if (generatePyramid && res > 0) {
                    int scale = (int) Math.pow(pyramidScale, res);
                    width /= scale;
                    height /= scale;
                }

                int writerSeries = series == -1 ? q : 0
                writer.setSeries(writerSeries)
                writer.setResolution(res)
                writer.setInterleaved(reader.isInterleaved())
                writer.setValidBitsPerPixel(reader.getBitsPerPixel())
                int numImages = writer.canDoStacks() ? reader.getImageCount() : 1

                int startPlane = 0
                int endPlane = numImages
                numImages = endPlane - startPlane
                total += numImages

                int count = 0
                for (int i = startPlane; i < endPlane; i++) {
                    String outputName = FormatTools.getFilename(q, i, reader, outputPattern, false)
                    if (outputName == FormatTools.getTileFilename(0, 0, 0, outputName)) {
                        int[] coordinates = FormatTools.getZCTCoords(order.getValue(), sizeZ, sizeC, sizeT,
                                sizeZ * sizeC * sizeT, i)
                        // For legacy mode
                        String channelName = retrieve.getChannelName(q, coordinates[1]) ?: coordinates[1]
                        outputs << new CytomineFile(outputName, coordinates[1], coordinates[0],
                                coordinates[2], channelName)
                        writer.setId(outputName)
                        if (compression != null) writer.setCompression(compression)
                    }

                    int outputIndex = 0
                    if (nextOutputIndex.containsKey(outputName)) {
                        outputIndex = nextOutputIndex.get(outputName)
                    }

                    long s = System.currentTimeMillis()
                    long m = convertPlane(writer, i, outputIndex, outputName)
                    long e = System.currentTimeMillis()
                    read += m - s
                    write += e - m

                    nextOutputIndex.put(outputName, outputIndex + 1)
                    if (i == endPlane - 1) {
                        nextOutputIndex.remove(outputName)
                    }

                    // log number of planes processed every second or so
                    if (count == numImages - 1 || (e - timeLastLogged) / 1000 > 0) {
                        int current = (count - startPlane) + 1
                        int percent = 100 * current / numImages
                        StringBuilder sb = new StringBuilder()
                        sb.append("\t")
                        int numSeries = last - first
                        if (numSeries > 1) {
                            sb.append("Series ")
                            sb.append(q)
                            sb.append(": converted ")
                        } else sb.append("Converted ")
                        LOGGER.info(sb.toString() + "$current/$numImages planes ($percent%)")
                        timeLastLogged = e
                    }
                    count++
                }
            }
        }
        writer.close()
        return outputs
    }

    private def setupWriter() {
        if (this.format == "OMETIFF") {
            writer = new OMETiffWriter()
        }
        else {
            writer = new TiffWriter()
        }

        writer.setMetadataOptions(options)
        writer.setCanDetectBigTiff(true)
        writer.setWriteSequentially(true)

        if (store instanceof MetadataRetrieve) {
            try {
                String xml = service.getOMEXML(service.asRetrieve(store))
                OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot()
                IMetadata meta = service.createOMEXMLMetadata(xml)
                if (series >= 0) {
                    Image exportImage = new Image(root.getImage(series))
                    Pixels exportPixels = new Pixels(root.getImage(series).getPixels())
                    exportImage.setPixels(exportPixels)
                    OMEXMLMetadataRoot newRoot = (OMEXMLMetadataRoot) meta.getRoot()
                    while (newRoot.sizeOfImageList() > 0) {
                        newRoot.removeImage(newRoot.getImage(0))
                    }
                    while (newRoot.sizeOfPlateList() > 0) {
                        newRoot.removePlate(newRoot.getPlate(0))
                    }
                    newRoot.addImage(exportImage)
                    meta.setRoot(newRoot)
                    meta.setPixelsSizeX(new PositiveInteger(width), 0)
                    meta.setPixelsSizeY(new PositiveInteger(height), 0)

                    setupResolutions(meta)
                    writer.setMetadataRetrieve((MetadataRetrieve) meta)
                } else {
                    for (int i = 0; i < reader.getSeriesCount(); i++) {
                        meta.setPixelsSizeX(new PositiveInteger(width), i)
                        meta.setPixelsSizeY(new PositiveInteger(height), i)
                    }

                    setupResolutions(meta)
                    writer.setMetadataRetrieve((MetadataRetrieve) meta)
                }
            }
            catch (ServiceException e) {
                throw new loci.formats.FormatException(e)
            }
        }
    }

    private def setupReader() {
        reader = new Memoizer(new ImageReader(), 0, new File(BioFormatsUtils.CACHE_DIRECTORY))
        reader.setMetadataOptions(options)
        reader.setGroupFiles(true)
        reader.setMetadataFiltered(true)
        reader.setOriginalMetadataPopulated(keepOriginalMetadata)
        reader.setFlattenedResolutions(flatten)

        try {
            ServiceFactory factory = new ServiceFactory()
            service = factory.getInstance(OMEXMLService.class)
            reader.setMetadataStore(service.createOMEXMLMetadata())
        }
        catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de)
        }
        catch (ServiceException se) {
            throw new loci.formats.FormatException(se)
        }

        reader.setId(input.absolutePath)

        store = reader.getMetadataStore()
        MetadataTools.populatePixels(store, reader, false, false)

        // only switch series if the '-series' flag was used;
        // otherwise default to series 0
        if (series >= 0) {
            reader.setSeries(series)
        }

        width = reader.getSizeX()
        height = reader.getSizeY()
    }

    // -- Helper methods --

    /**
     * Convert the specified plane using the given writer.
     * @param writer the {@link loci.formats.IFormatWriter} to use for writing the plane
     * @param index the index of the plane to convert in the input file
     * @param outputIndex the index of the plane to convert in the output file
     * @param currentFile the file name or pattern being written to
     * @return the time at which conversion started, in milliseconds
     * @throws loci.formats.FormatException* @throws IOException
     */
    private long convertPlane(IFormatWriter writer, int index, int outputIndex,
                              String currentFile)
            throws loci.formats.FormatException, IOException {
        if (DataTools.safeMultiply64(width, height) >=
                DataTools.safeMultiply64(4096, 4096) ||
                tileWidth > 0 || tileHeight > 0) {
            // this is a "big image" or an output tile size was set, so we will attempt
            // to convert it one tile at a time

            if ((writer instanceof TiffWriter) || ((writer instanceof ImageWriter) &&
                    (((ImageWriter) writer).getWriter(outputPattern) instanceof TiffWriter))) {
                return convertTilePlane(writer, index, outputIndex, currentFile)
            }
        }

        byte[] buf = getTile(reader, writer.getResolution(), index,
                xCoordinate, yCoordinate, width, height)

        applyLUT(writer)
        long m = System.currentTimeMillis()
        writer.saveBytes(outputIndex, buf)
        return m
    }

    /**
     * Convert the specified plane as a set of tiles, using the specified writer.
     * @param writer the {@link loci.formats.IFormatWriter} to use for writing the plane
     * @param index the index of the plane to convert in the input file
     * @param outputIndex the index of the plane to convert in the output file
     * @param currentFile the file name or pattern being written to
     * @return the time at which conversion started, in milliseconds
     * @throws loci.formats.FormatException* @throws IOException
     */
    private long convertTilePlane(IFormatWriter writer, int index, int outputIndex,
                                  String currentFile)
            throws loci.formats.FormatException, IOException {
        int w = Math.min(reader.getOptimalTileWidth(), width)
        int h = Math.min(reader.getOptimalTileHeight(), height)
        if (tileWidth > 0 && tileWidth <= width) {
            w = tileWidth
        }
        if (tileHeight > 0 && tileHeight <= height) {
            h = tileHeight
        }

        if (firstTile) {
            LOGGER.info("Tile size = {} x {}", w, h)
            firstTile = false
        }

        int nXTiles = width / w
        int nYTiles = height / h

        if (nXTiles * w != width) {
            nXTiles++
        }
        if (nYTiles * h != height) {
            nYTiles++
        }

        IFD ifd = new IFD()
        ifd.put(IFD.TILE_WIDTH, w)
        ifd.put(IFD.TILE_LENGTH, h)

        Long m = null
        for (int y = 0; y < nYTiles; y++) {
            for (int x = 0; x < nXTiles; x++) {
                int tileX = xCoordinate + x * w
                int tileY = yCoordinate + y * h
                int tileWidth = x < nXTiles - 1 ? w : width - (w * x)
                int tileHeight = y < nYTiles - 1 ? h : height - (h * y)
                byte[] buf = getTile(reader, writer.getResolution(),
                        index, tileX, tileY, tileWidth, tileHeight)

                String tileName =
                        FormatTools.getTileFilename(x, y, y * nXTiles + x, currentFile)
                if (currentFile != tileName) {
                    int nTileRows = getTileRows(currentFile)
                    int nTileCols = getTileColumns(currentFile)

                    int sizeX = nTileCols == 1 ? width : tileWidth
                    int sizeY = nTileRows == 1 ? height : tileHeight
                    MetadataRetrieve retrieve = writer.getMetadataRetrieve()
                    writer.close()
                    int writerSeries = series == -1 ? reader.getSeries() : 0
                    if (retrieve instanceof MetadataStore) {
                        ((MetadataStore) retrieve).setPixelsSizeX(
                                new PositiveInteger(sizeX), writerSeries)
                        ((MetadataStore) retrieve).setPixelsSizeY(
                                new PositiveInteger(sizeY), writerSeries)
                        setupResolutions((IMetadata) retrieve)
                    }

                    writer.setMetadataRetrieve(retrieve)
                    writer.setId(tileName)
                    if (compression != null) writer.setCompression(compression)

                    outputIndex = 0
                    if (nextOutputIndex.containsKey(tileName)) {
                        outputIndex = nextOutputIndex.get(tileName)
                    }
                    nextOutputIndex.put(tileName, outputIndex + 1)

                    if (nTileRows > 1) {
                        ifd.put(IFD.TILE_LENGTH, tileHeight)
                    }
                    if (nTileCols > 1) {
                        ifd.put(IFD.TILE_WIDTH, tileWidth)
                    }
                }

                applyLUT(writer)
                if (m == null) {
                    m = System.currentTimeMillis()
                }

                // calculate the XY coordinate in the output image
                // don't use tileX and tileY, as they will be too large
                // if any cropping was performed
                int outputX = x * w
                int outputY = y * h

                if (currentFile.indexOf(FormatTools.TILE_NUM) >= 0 ||
                        currentFile.indexOf(FormatTools.TILE_X) >= 0 ||
                        currentFile.indexOf(FormatTools.TILE_Y) >= 0) {
                    outputX = 0;
                    outputY = 0;
                }

                if (writer instanceof TiffWriter) {
                    ((TiffWriter) writer).saveBytes(outputIndex, buf,
                            ifd, outputX, outputY, tileWidth, tileHeight)
                } else if (writer instanceof ImageWriter) {
                    IFormatWriter baseWriter = ((ImageWriter) writer).getWriter(outputPattern)
                    if (baseWriter instanceof TiffWriter) {
                        ((TiffWriter) baseWriter).saveBytes(outputIndex, buf, ifd,
                                outputX, outputY, tileWidth, tileHeight)
                    }
                }
            }
        }
        return m
    }

    /**
     * Calculate the number of vertical tiles represented by the given file name pattern.
     * @param outputName the output file name pattern
     * @return the number of vertical tiles (rows)
     */
    private int getTileRows(String outputName) {
        if (outputName.indexOf(FormatTools.TILE_Y) >= 0 ||
                outputName.indexOf(FormatTools.TILE_NUM) >= 0) {
            int h = reader.getOptimalTileHeight()
            if (tileHeight > 0 && tileHeight <= height) {
                h = tileHeight
            }
            int nYTiles = height / h
            if (nYTiles * h != height) {
                nYTiles++
            }
            return nYTiles
        }
        return 1
    }

    /**
     * Calculate the number of horizontal tiles represented by the given file name pattern.
     * @param outputName the output file name pattern
     * @return the number of horizontal tiles (columns)
     */
    int getTileColumns(String outputName) {
        if (outputName.indexOf(FormatTools.TILE_X) >= 0 ||
                outputName.indexOf(FormatTools.TILE_NUM) >= 0) {
            int w = reader.getOptimalTileWidth()
            if (tileWidth > 0 && tileWidth <= width) {
                w = tileWidth
            }

            int nXTiles = width / w
            if (nXTiles * w != width) {
                nXTiles++
            }
            return nXTiles
        }
        return 1
    }


    /**
     * Use the lookup table from the reader (if present) to set
     * the color model in the given writer
     * @param writer the {@link loci.formats.IFormatWriter} on which to set a color model
     * @throws loci.formats.FormatException* @throws IOException
     */
    private void applyLUT(IFormatWriter writer)
            throws loci.formats.FormatException, IOException {
        byte[][] lut = reader.get8BitLookupTable()
        if (lut != null) {
            IndexColorModel model = new IndexColorModel(8, lut[0].length,
                    lut[0], lut[1], lut[2])
            writer.setColorModel(model)
        } else {
            short[][] lut16 = reader.get16BitLookupTable()
            if (lut16 != null) {
                Index16ColorModel model = new Index16ColorModel(16, lut16[0].length,
                        lut16, reader.isLittleEndian())
                writer.setColorModel(model)
            }
        }
    }

    private void setupResolutions(IMetadata meta) {
        if (!(meta instanceof OMEPyramidStore)) {
            return
        }
        for (int series = 0; series < meta.getImageCount(); series++) {
            int width = meta.getPixelsSizeX(series).getValue()
            int height = meta.getPixelsSizeY(series).getValue()
            for (int i = 1; i < pyramidResolutions; i++) {
                int scale = (int) Math.pow(pyramidScale, i)
                ((OMEPyramidStore) meta).setResolutionSizeX(
                        new PositiveInteger((int) width / scale), series, i)
                ((OMEPyramidStore) meta).setResolutionSizeY(
                        new PositiveInteger((int) height / scale), series, i)
            }
        }
    }

    private byte[] getTile(IFormatReader reader, int resolution,
                           int no, int x, int y, int w, int h)
            throws loci.formats.FormatException, IOException {
        if (resolution < reader.getResolutionCount()) {
            reader.setResolution(resolution)
            return reader.openBytes(no, x, y, w, h)
        }
        reader.setResolution(0)
        IImageScaler scaler = new SimpleImageScaler()
        int scale = (int) Math.pow(pyramidScale, resolution)
        byte[] tile =
                reader.openBytes(no, x * scale, y * scale, w * scale, h * scale)
        int type = reader.getPixelType()
        return scaler.downsample(tile, w * scale, h * scale, scale,
                FormatTools.getBytesPerPixel(type), reader.isLittleEndian(),
                FormatTools.isFloatingPoint(type), reader.getRGBChannelCount(),
                reader.isInterleaved())
    }

}
