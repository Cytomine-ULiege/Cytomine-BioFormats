package be.cytomine.bioformats.worker

/*
 * Cytomine-Bioformats, a wrapper to link Bio-formats with Cytomine.
 * Copyright (C) 2015-2020 cytomine.org
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

import be.cytomine.bioformats.BioFormatsUtils
import loci.common.DebugTools
import loci.common.services.DependencyException
import loci.common.services.ServiceException
import loci.common.services.ServiceFactory
import loci.formats.FormatTools
import loci.formats.ImageReader
import loci.formats.MissingLibraryException
import loci.formats.meta.DummyMetadata
import loci.formats.meta.MetadataRetrieve
import loci.formats.meta.MetadataStore
import loci.formats.services.OMEXMLService
import loci.formats.services.OMEXMLServiceImpl
import ome.units.UNITS
import ome.units.quantity.Length

class PropertyExtractor extends Worker {

    /*
     * Keywords extracted from, e.g.:
     * https://github.com/ome/bioformats/blob/develop/components/formats-gpl/src/loci/formats/in/ZeissCZIReader.java#L1344
     * https://github.com/ome/bioformats/blob/25645389e076a7bd0011e04c4dd8982c0f0614ed/components/formats-gpl/src/loci/formats/in/SVSReader.java#L486
     * https://github.com/ome/bioformats/blob/25645389e076a7bd0011e04c4dd8982c0f0614ed/components/formats-gpl/src/loci/formats/in/HamamatsuVMSReader.java#L360
     * https://github.com/ome/bioformats/blob/25645389e076a7bd0011e04c4dd8982c0f0614ed/components/formats-gpl/src/loci/formats/in/LeicaSCNReader.java#L452
     * https://github.com/ome/bioformats/blob/25645389e076a7bd0011e04c4dd8982c0f0614ed/components/formats-gpl/src/loci/formats/in/NDPIReader.java#L616
     * https://github.com/ome/bioformats/blob/b68a64959b9f17ceb6b9cb57c15d7b46b56f23ed/components/formats-gpl/src/loci/formats/in/VentanaReader.java#L799
     *
     */
    private static final List<String> THUMB_KEYWORDS = ["thumb", "thumb image", "thumbnail", "thumbnail image"]
    private static final List<String> LABEL_KEYWORDS = ["label", "label image", "overview", "overview image"]
    private static final List<String> MACRO_KEYWORDS = ["macro", "macro image"]

    private def computedProperties

    def includeRaw = false

    // Compatibility with IMS
    def legacyMode = true

    public PropertyExtractor(def file, def includeRaw, def legacyMode) {
        this.file = file
        this.includeRaw = includeRaw
        this.legacyMode = legacyMode
    }

    private Integer getAssociatedSeries(MetadataRetrieve meta, List<String> keywords, int biggestSeries, int seriesCount) {
        for (int i = 0; i < seriesCount; i++) {
            if (i != biggestSeries && keywords.contains(meta.getImageName(i))) {
                return i
            }
        }
        return null
    }

    @Override
    def process() {
        DebugTools.enableLogging("INFO")

        def reader = new ImageReader()
        reader.setMetadataFiltered(true)
        reader.setOriginalMetadataPopulated(true)

        // create OME-XML metadata store
        try {
            ServiceFactory factory = new ServiceFactory()
            OMEXMLService service = factory.getInstance(OMEXMLService.class)
            reader.setMetadataStore(service.createOMEXMLMetadata())
        }
        catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de)
        }
        catch (ServiceException se) {
            throw new loci.formats.FormatException(se)
        }

        reader.setId(file.absolutePath)
        def biggestSeries = BioFormatsUtils.getBiggestSeries(reader)
        reader.setSeries(biggestSeries)

        MetadataStore store = reader.getMetadataStore()
        MetadataRetrieve meta = store instanceof MetadataRetrieve ?
                (MetadataRetrieve) store : new DummyMetadata()

        String pixelType = null
        try {
            // Should return one of: int8, uint8, int16, uint16, int32, uint32, float, double, bit
            pixelType = FormatTools.getPixelTypeString(reader.getPixelType())
        }
        catch (Exception ignored) {}

        Number physicalSizeX = null
        String physicalSizeXUnit = null
        try {
            Length length = meta.getPixelsPhysicalSizeX(biggestSeries)
            if (length.unit().isConvertible(UNITS.METER)) {
                physicalSizeX = (legacyMode) ? length.value(UNITS.MICROMETER) : length.value()
                physicalSizeXUnit = length.unit().getSymbol()
            }
        }
        catch (Exception ignored) {}

        Number physicalSizeY = null
        String physicalSizeYUnit = null
        try {
            Length length = meta.getPixelsPhysicalSizeY(biggestSeries)
            if (length.unit().isConvertible(UNITS.METER)) {
                physicalSizeY = (legacyMode) ? length.value(UNITS.MICROMETER) : length.value()
                physicalSizeYUnit = length.unit().getSymbol()
            }
        }
        catch (Exception ignored) {}

        Number physicalSizeZ = null
        String physicalSizeZUnit = null
        try {
            Length length = meta.getPixelsPhysicalSizeZ(biggestSeries)
            if (length.unit().isConvertible(UNITS.METER)) {
                physicalSizeZ = (legacyMode) ? length.value(UNITS.MICROMETER) : length.value()
                physicalSizeZUnit = length.unit().getSymbol()
            }
        }
        catch (Exception ignored) {}

        Number timeIncrement = null
        String timeIncrementUnit = null
        try {
            Length length = meta.getPixelsTimeIncrement(biggestSeries)
            if (length.unit().isConvertible(UNITS.SECOND)) {
                timeIncrement = (legacyMode) ? length.value(UNITS.SECOND) : length.value()
                timeIncrementUnit = length.unit().getSymbol()
            }
        }
        catch (Exception ignored) {}

        Double magnification = null
        try {
            magnification = meta.getObjectiveNominalMagnification(biggestSeries, 0)
        }
        catch(Exception ignored) {}

        Double calibratedMagnification = null
        try {
            calibratedMagnification = meta.getObjectiveCalibratedMagnification(biggestSeries, 0)
        }
        catch(Exception ignored) {}

        String microscope = null
        try {
            String instrumentIndex = meta.getImageInstrumentRef(biggestSeries)
            microscope = meta.getMicroscopeModel(instrumentIndex)
        }
        catch(Exception ignored) {}

        def spp = null
        try {
            spp = meta.getChannelSamplesPerPixel(biggestSeries, 0).value
        }
        catch (Exception ignored) {}

        def date = null
        try {
            date = meta.getImageAcquisitionDate(biggestSeries).value
        }
        catch (Exception ignored) {}

        String description = null
        try {
            description = meta.getImageDescription(biggestSeries)
        }
        catch (Exception ignored) {}

        def channelNames = [:]
        def channels = []
        (0..<reader.getSizeC()).each {c ->
            String channelName = null
            try {
                channelName = meta.getChannelName(biggestSeries, c)
            }
            catch (Exception ignored) {}

            Number emissionWavelength = null
            String emissionWavelengthUnit = null
            try {
                Length length = meta.getChannelEmissionWavelength(biggestSeries, c)
                if (length.unit().isConvertible(UNITS.METER)) {
                    emissionWavelength = (legacyMode) ? length.value(UNITS.NANOMETER) : length.value()
                    emissionWavelengthUnit = length.unit().getSymbol()
                }
            }
            catch (Exception ignored) {}

            Number excitationWavelength = null
            String excitationWavelengthUnit = null
            try {
                Length length = meta.getChannelExcitationWavelength(biggestSeries, c)
                if (length.unit().isConvertible(UNITS.METER)) {
                    excitationWavelength = (legacyMode) ? length.value(UNITS.NANOMETER) : length.value()
                    excitationWavelengthUnit = length.unit().getSymbol()
                }
            }
            catch (Exception ignored) {}

            Integer color = null
            try {
                color = meta.getChannelColor(biggestSeries, c).value
            }
            catch (Exception ignored) {}

            String suggestedName = (String)(channelName ?: emissionWavelength ?: excitationWavelength)
            if (suggestedName != null)
                channelNames << [(c): suggestedName]

            channels << [
                    Name: channelName,
                    EmissionWavelength: emissionWavelength,
                    EmissionWavelengthUnit: emissionWavelengthUnit,
                    ExcitationWavelength: excitationWavelength,
                    ExcitationWavelengthUnit: excitationWavelengthUnit,
                    Color: color,
                    SuggestedName: suggestedName
            ]
        }

        if (channelNames.isEmpty()) {
            channelNames = null
        }
        if (channels.isEmpty()) {
            channels = null
        }

        def properties = [
                'Bioformats.Pixels.SizeX': reader.getSizeX(),
                'Bioformats.Pixels.SizeY': reader.getSizeY(),
                'Bioformats.Pixels.SizeZ': reader.getSizeZ(),
                'Bioformats.Pixels.SizeC': reader.getSizeC(),
                'Bioformats.Pixels.SizeT': reader.getSizeT(),
                'Bioformats.Pixels.BitsPerPixel': reader.getBitsPerPixel(),
                'Bioformats.Pixels.PixelType': pixelType,
                'Bioformats.Pixels.EffectiveSizeC': reader.getEffectiveSizeC(),
                'Bioformats.Pixels.IsRGB': reader.isRGB(),
                'Bioformats.Pixels.RGBChannelCount': reader.getRGBChannelCount(),
                'Bioformats.Pixels.SamplesPerPixel': spp,
                'Bioformats.Pixels.PhysicalSizeX': physicalSizeX,
                'Bioformats.Pixels.PhysicalSizeXUnit': physicalSizeXUnit,
                'Bioformats.Pixels.PhysicalSizeY': physicalSizeY,
                'Bioformats.Pixels.PhysicalSizeYUnit': physicalSizeYUnit,
                'Bioformats.Pixels.PhysicalSizeZ': physicalSizeZ,
                'Bioformats.Pixels.PhysicalSizeZUnit': physicalSizeZUnit,
                'Bioformats.Pixels.TimeIncrement': timeIncrement,
                'Bioformats.Pixels.TimeIncrementUnit': timeIncrementUnit,
                'Bioformats.Objective.NominalMagnification': magnification,
                'Bioformats.Objective.CalibratedMagnification': calibratedMagnification,
                'Bioformats.Microscope.Model': microscope,
                'Bioformats.Image.Description': description,
                'Bioformats.Image.AcquisitionDate': date,
                'Bioformats.Channels.Name': channelNames,
                'Bioformats.Channels': channels,
        ]

        if (includeRaw) {
            reader.getGlobalMetadata().each {
                if (legacyMode) {
                    properties << [(it.key.replaceAll("\\|", ".").replaceAll(" #", "[") + "]") : it.value]
                }
                else {
                    List tokens = it.key.replaceAll("\\|", ".").split(" #")
                    String key = tokens[0]
                    if (key in properties.keySet()) {
                        def existing = properties[key]
                        if (existing instanceof List) {
                            properties[key] = existing + [it.value]
                        }
                        else {
                            properties[key] = [existing, it.value]
                        }
                    }
                    else {
                        properties << [(key): it.value]
                    }
                }
            }
        }

        def planes = []
        (0..<reader.getImageCount()).each {p ->
            try {
                Integer c = meta.getPlaneTheC(biggestSeries, p).value
                Integer z = meta.getPlaneTheZ(biggestSeries, p).value
                Integer t = meta.getPlaneTheT(biggestSeries, p).value
                planes << [
                        'TheC': c,
                        'TheZ': z,
                        'TheT': t,
                        '_Index': FormatTools.getIndex(reader, z, c, t),
                        '_Series': biggestSeries
                ]
            }
            catch (Exception ignored) {}
        }
        if (!planes.isEmpty()) {
            properties['Bioformats.Planes'] = planes
        }
        
        int seriesCount = reader.getSeriesCount()
        Integer thumbSeries = this.getAssociatedSeries(meta, THUMB_KEYWORDS, biggestSeries, seriesCount)
        if (thumbSeries != null) {
            reader.setSeries(thumbSeries)
            properties['Bioformats.Series.Thumb'] = [
                    'Width': reader.getSizeX(),
                    'Height': reader.getSizeY(),
                    'Channels': reader.getSizeC(),
                    '_Series': thumbSeries
            ]
        }

        Integer labelSeries = this.getAssociatedSeries(meta, LABEL_KEYWORDS, biggestSeries, seriesCount)
        if (labelSeries != null) {
            reader.setSeries(labelSeries)
            properties['Bioformats.Series.Label'] = [
                    'Width': reader.getSizeX(),
                    'Height': reader.getSizeY(),
                    'Channels': reader.getSizeC(),
                    '_Series': labelSeries
            ]
        }

        Integer macroSeries = this.getAssociatedSeries(meta, MACRO_KEYWORDS, biggestSeries, seriesCount)
        if (macroSeries != null) {
            reader.setSeries(macroSeries)
            properties['Bioformats.Series.Macro'] = [
                    'Width': reader.getSizeX(),
                    'Height': reader.getSizeY(),
                    'Channels': reader.getSizeC(),
                    '_Series': macroSeries
            ]
        }

        this.computedProperties = properties.findAll {
            it.value != null && !(it as String).isEmpty()
        }
    }

    @Override
    def getOutput() {
        return this.computedProperties
    }
}
