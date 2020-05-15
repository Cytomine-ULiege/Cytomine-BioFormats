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
import loci.formats.ImageReader
import loci.formats.MissingLibraryException
import loci.formats.meta.DummyMetadata
import loci.formats.meta.MetadataRetrieve
import loci.formats.meta.MetadataStore
import loci.formats.services.OMEXMLService
import loci.formats.services.OMEXMLServiceImpl
import ome.units.UNITS

class PropertyExtractor extends Worker {

    private def computedProperties

    def includeRaw = false

    public PropertyExtractor(def file, def includeRaw) {
        this.file = file
        this.includeRaw = includeRaw
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

        def physicalSizeX = null
        try {
            physicalSizeX = meta.getPixelsPhysicalSizeX(biggestSeries)
        }
        catch (Exception ignored) {}

        def physicalSizeY = null
        try {
            physicalSizeY = meta.getPixelsPhysicalSizeY(biggestSeries)
        }
        catch (Exception ignored) {}

        def physicalSizeZ = null
        try {
            physicalSizeZ = meta.getPixelsPhysicalSizeZ(biggestSeries)
        }
        catch (Exception ignored) {}

        def timeIncrement = null
        try {
            timeIncrement = meta.getPixelsTimeIncrement(biggestSeries)
        }
        catch (Exception ignored) {}

        def magnification = null
        try {
            magnification = meta.getObjectiveNominalMagnification(biggestSeries, 0)
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

        def channelNames = [:]
        try {
            (0..<reader.getSizeC()).each {
                def channelName = meta.getChannelName(biggestSeries, it)
                if (!channelName) channelName = meta.getChannelEmissionWavelength(biggestSeries, it)
                if (channelName != null)
                    channelNames << [(it): channelName]
            }
        }
        catch (Exception ignored) {}
        if (channelNames.isEmpty()) {
            channelNames = null
        }

        def properties = [
                'Bioformats.Pixels.SizeX': reader.getSizeX(),
                'Bioformats.Pixels.SizeY': reader.getSizeY(),
                'Bioformats.Pixels.SizeZ': reader.getSizeZ(),
                'Bioformats.Pixels.SizeC': reader.getSizeC(),
                'Bioformats.Pixels.SizeT': reader.getSizeT(),
                'Bioformats.Pixels.BitsPerPixel': reader.getBitsPerPixel(),
                'Bioformats.Pixels.SamplesPerPixel': spp,
                'Bioformats.Pixels.PhysicalSizeX': physicalSizeX?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeXUnit': (physicalSizeX) ? "µm" : null,
                'Bioformats.Pixels.PhysicalSizeY': physicalSizeY?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeYUnit': (physicalSizeY) ? "µm" : null,
                'Bioformats.Pixels.PhysicalSizeZ': physicalSizeZ?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeZUnit': (physicalSizeZ) ? "µm" : null,
                'Bioformats.Pixels.TimeIncrement': timeIncrement?.value(UNITS.SECOND),
                'Bioformats.Pixels.TimeIncrementUnit': (timeIncrement) ? 's' : null,
                'Bioformats.Objective.NominalMagnification': magnification,
                'Bioformats.Image.AcquisitionDate': date,
                'Bioformats.Channels.Name': channelNames
        ]

        if (includeRaw) {
            reader.getGlobalMetadata().each {
                properties << [(it.key.replaceAll("\\|", ".").replaceAll(" #", "[") + "]") : it.value]
            }
        }

        this.computedProperties = properties.findAll {it.value != null && !(it as String).isEmpty()}
    }

    @Override
    def getOutput() {
        return this.computedProperties
    }
}
