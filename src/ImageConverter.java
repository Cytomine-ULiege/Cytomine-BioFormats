/**
 * This class provided by ome team has been modified in order to be used in this project
 * <p>
 * <p>
 * Application based on the OME-BIOFORMATS C++ library for image IO.
 * Copyright © 2006 - 2014 Open Microscopy Environment:
 * - Massachusetts Institute of Technology
 * - National Institutes of Health
 * - University of Dundee
 * - Board of Regents of the University of Wisconsin-Madison
 * - Glencoe Software, Inc.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 */

import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.*;
import loci.formats.gui.Index16ColorModel;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.services.OMEXMLServiceImpl;
import loci.formats.tiff.IFD;
import ome.xml.model.primitives.PositiveInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.IndexColorModel;
import java.io.IOException;

/**
 * ImageConverter is a utility class for converting a file between formats.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/tools/ImageConverter.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/tools/ImageConverter.java;hb=HEAD">Gitweb</a></dd></dl>
 */
public final class ImageConverter {

    // -- Constants --

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageConverter.class);

    // -- Fields --

    private String in = null, out = null;
    private boolean group = true;
    private int xCoordinate = 0, yCoordinate = 0, width = 0, height = 0;
    private int z = 0, t = 0, c = 0;

    private IFormatReader reader;
    private MinMaxCalculator minMax;

    // -- Constructor --

    public ImageConverter(String in, String out) {
        this.in = in;
        this.out = out;
    }

    public int getZ() {
        return z;
    }

    public int getT() {
        return t;
    }

    public int getC() {
        return c;
    }
    // -- Utility methods --

    /** A utility method for converting a file from the command line. */
    public boolean testConvert(IFormatWriter writer, boolean bigtiff) throws FormatException, IOException {

        long start = System.currentTimeMillis(); //to mesure the runtime

        //set up the reader
        reader = new ImageReader();

        reader.setGroupFiles(group); //group files
        reader.setMetadataFiltered(true);
        reader.setOriginalMetadataPopulated(true);
        OMEXMLService service = null;
        try {
            ServiceFactory factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata());
        } catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de);
        } catch (ServiceException se) {
            throw new FormatException(se);
        }
        //reader is an object instance of an Interface "IFormatReader" for all biological file format readers.
        //bind the reader to OMEXMLMetadata object
        reader.setId(in);

        //MetadataStore is used for store the Metadadata of the image retrieved by the reader
        //this MetadataStore object is used  also by the reader to populate information in a linear order
        MetadataStore store = reader.getMetadataStore();

        MetadataTools.populatePixels(store, reader, false, false);
        /**
         * Populates the 'pixels' element of the given metadata store, using core
         * metadata from the given reader.  If the 'doPlane' flag is set,
         * then the 'plane' elements will be populated as well.
         * If the 'doImageName' flag is set, then the image name will be populated
         * as well.  By default, 'doImageName' is true.
         */


        boolean dimensionsSet = true;
        if (width == 0 || height == 0) { //if it is not a crop
            // only switch series if the '-series' flag was used;
            // otherwise default to series 0
            width = reader.getSizeX(); /** Gets the size of the X dimension. */
            height = reader.getSizeY(); /** Gets the size of the Y dimension. */
            dimensionsSet = false;
        }

        /**
         * MetadataStore goes hand in hand with MetadatRetrieve interface, Generally, this last provides getter methods
         * While the first provides the setter methods
         */

        if (store instanceof MetadataRetrieve) {
            //here, we don't need to specify the series image
            for (int i = 0; i < reader.getSeriesCount(); i++) {
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!  ici le if est peeut etre de l'étudiant !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                if (width != reader.getSizeX() || height != reader.getSizeY()) { //if, it does not a crop
                    // Ignoring Plane element, complex property
                    store.setPixelsSizeX(new PositiveInteger(width), 0);  //void setPixelsSizeX(PositiveInteger sizeX, int imageIndex);
                    store.setPixelsSizeY(new PositiveInteger(height), 0); //void setPixelsSizeY(PositiveInteger sizeX, int imageIndex);
                }
            }
            writer.setMetadataRetrieve((MetadataRetrieve) store);
        } //end of if (store instanceof MetadataRetrieve) {


        writer.setWriteSequentially(true); //we write the image planes sequentially

        if (writer instanceof TiffWriter) { //the case of bigTiff type, force BIGTIFF file to be written
            ((TiffWriter) writer).setBigTiff(bigtiff);
        } else if (writer instanceof ImageWriter) { //yet, the bigTiff case
            IFormatWriter w = ((ImageWriter) writer).getWriter(out);
            if (w instanceof TiffWriter) {
                ((TiffWriter) w).setBigTiff(bigtiff);
            }
        }

        String format = writer.getFormat(); //retrieve the output format
        LOGGER.info("[{}] -> {} [{}]", reader.getFormat(), out, format);

        long mid = System.currentTimeMillis(); //for writting runtime

        int total = 0;
        int num = writer.canDoStacks() ? reader.getSeriesCount() : 1; //for all the series
        long read = 0, write = 0;
        int first = 0; //Either, series from 0 ---> num for all series, or from series --->series+1 for
        int last = num; //specified series numerous
        long timeLastLogged = System.currentTimeMillis();

        // !!!!!!!!!!!!!!!!!!!!Il a aussi ajouté ce println !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        System.out.println("Test convert last = " + last);
        for (int q = first; q < last; q++) { //here, we read and write the image series, from the first to the last
            reader.setSeries(q);

            if (!dimensionsSet) {
                width = reader.getSizeX();
                height = reader.getSizeY();
            }

            int writerSeries = q;
            writer.setSeries(writerSeries);
            writer.setInterleaved(reader.isInterleaved());
            writer.setValidBitsPerPixel(reader.getBitsPerPixel());
            int numImages = writer.canDoStacks() ? reader.getImageCount() : 1;

            int startPlane = 0;
            int endPlane = numImages;
            numImages = endPlane - startPlane;

            total += numImages;

            int count = 0;
            // !!!!!!!!!!!!!!!!!Les 3 println et les 3 var!!!!!!!!!!!!!!!!!!!!!!!!!!!
            System.out.println("Test convert c = " + reader.getSizeC());
            z = reader.getSizeZ();
            //c = reader.getSizeC();
            c = reader.getEffectiveSizeC();
            t = reader.getSizeT();

            System.out.println("Test convert startPlane = " + startPlane);
            System.out.println("Test convert endPlane = " + endPlane);
            for (int i = startPlane; i < endPlane; i++) { //retrieve planes and write its

                //Note that the extension of the file name passed to ‘writer.setId(…)’ determines the file format of the exported file.
                writer.setId(FormatTools.getFilename(q, i, reader, out));

                long s = System.currentTimeMillis();
                long m = convertPlane(writer, i, startPlane);
                long e = System.currentTimeMillis();
                read += m - s;
                write += e - m;

                // log number of planes processed every second or so
                if (count == numImages - 1 || (e - timeLastLogged) / 1000 > 0) {
                    int current = (count - startPlane) + 1;
                    int percent = 100 * current / numImages;
                    StringBuilder sb = new StringBuilder();
                    sb.append("\t");
                    int numSeries = last - first;
                    if (numSeries > 1) {
                        sb.append("Series ");
                        sb.append(q);
                        sb.append(": converted ");
                    } else sb.append("Converted ");
                    LOGGER.info(sb.toString() + "{}/{} planes ({}%)",
                            current, numImages, percent);
                    timeLastLogged = e;
                }
                count++;
            } //end of inner for loop
        }//end of enclosure for loop

        writer.close();
        long end = System.currentTimeMillis();
        LOGGER.info("[done]");

        // output timing results
        float sec = (end - start) / 1000f;
        long initial = mid - start;
        float readAvg = (float) read / total;
        float writeAvg = (float) write / total;
        LOGGER.info("{}s elapsed ({}+{}ms per plane, {}ms overhead)",
                sec, readAvg, writeAvg, initial);

        return true;
    } //end  of the testConvert method


    //========================================================= -- Helper methods -- =================================================================

    private long convertPlane(IFormatWriter writer, int index, int startPlane) throws FormatException, IOException {
        if (DataTools.safeMultiply64(width, height) >=
                DataTools.safeMultiply64(4096, 4096)) {
            // this is a "big image", so we will attempt to convert it one tile
            // at a time

            if ((writer instanceof TiffWriter) || ((writer instanceof ImageWriter) &&
                    (((ImageWriter) writer).getWriter(out) instanceof TiffWriter))) {
                return convertTilePlane(writer, index, startPlane);
            }
        }

        byte[] buf =
                reader.openBytes(index, xCoordinate, yCoordinate, width, height);


        applyLUT(writer);
        long m = System.currentTimeMillis();
        writer.saveBytes(index - startPlane, buf);
        return m;
    }


    private long convertTilePlane(IFormatWriter writer, int index, int startPlane) throws FormatException, IOException {
        int w = reader.getOptimalTileWidth();
        int h = reader.getOptimalTileHeight();
        int nXTiles = width / w;
        int nYTiles = height / h;

        if (nXTiles * w != width) {
            nXTiles++;
        }
        if (nYTiles * h != height) {
            nYTiles++;
        }

        IFD ifd = new IFD();
        ifd.put(IFD.TILE_WIDTH, w);
        ifd.put(IFD.TILE_LENGTH, h);

        Long m = null;
        for (int y = 0; y < nYTiles; y++) {
            for (int x = 0; x < nXTiles; x++) {
                int tileX = xCoordinate + x * w;
                int tileY = yCoordinate + y * h;
                int tileWidth = x < nXTiles - 1 ? w : width - (w * x);
                int tileHeight = y < nYTiles - 1 ? h : height - (h * y);
                byte[] buf =
                        reader.openBytes(index, tileX, tileY, tileWidth, tileHeight);


                applyLUT(writer);
                if (m == null) {
                    m = System.currentTimeMillis();
                }

                if (writer instanceof TiffWriter) {
                    ((TiffWriter) writer).saveBytes(index - startPlane, buf,
                            ifd, tileX, tileY, tileWidth, tileHeight);
                } else if (writer instanceof ImageWriter) {
                    IFormatWriter baseWriter = ((ImageWriter) writer).getWriter(out);
                    if (baseWriter instanceof TiffWriter) {
                        ((TiffWriter) baseWriter).saveBytes(index - startPlane, buf, ifd,
                                tileX, tileY, tileWidth, tileHeight);
                    }
                }
            }
        }
        return m;
    }


    private void applyLUT(IFormatWriter writer) throws FormatException, IOException {
        byte[][] lut = reader.get8BitLookupTable();
        if (lut != null) {
            IndexColorModel model = new IndexColorModel(8, lut[0].length,
                    lut[0], lut[1], lut[2]);
            writer.setColorModel(model);
        } else {
            short[][] lut16 = reader.get16BitLookupTable();
            if (lut16 != null) {
                Index16ColorModel model = new Index16ColorModel(16, lut16[0].length,
                        lut16, reader.isLittleEndian());
                writer.setColorModel(model);
            }
        }
    }


}
