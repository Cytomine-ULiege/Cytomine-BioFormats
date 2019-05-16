package be.cytomine.bioformats.worker

/*
 * Application based on the OME-BIOFORMATS C++ library for image IO.
 * Copyright © 2006 - 2019 Open Microscopy Environment:
 *   - Massachusetts Institute of Technology
 *   - National Institutes of Health
 *   - University of Dundee
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
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
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 */

import be.cytomine.bioformats.BioFormatsUtils
import loci.common.DebugTools
import loci.common.services.ServiceFactory
import loci.formats.ImageReader
import loci.formats.meta.IMetadata
import loci.formats.services.OMEXMLService
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
        ServiceFactory factory = new ServiceFactory()
        OMEXMLService service = factory.getInstance(OMEXMLService.class)
        IMetadata meta = service.createOMEXMLMetadata()
        reader.setMetadataStore(meta)

        reader.setId(file.absolutePath)
        def biggestSeries = BioFormatsUtils.getBiggestSeries(reader)
        reader.setSeries(biggestSeries)

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

        def bps = null
        try {
            bps = meta.getPixelsSignificantBits(biggestSeries).value
        }
        catch (Exception ignored) {}

        def properties = [
                'Bioformats.Pixels.SizeX': reader.getSizeX(),
                'Bioformats.Pixels.SizeY': reader.getSizeY(),
                'Bioformats.Pixels.SizeZ': reader.getSizeZ(),
                'Bioformats.Pixels.SizeC': reader.getSizeC(),
                'Bioformats.Pixels.SizeT': reader.getSizeT(),
                'Bioformats.Pixels.BitsPerPixel': reader.getBitsPerPixel(),
                'Bioformats.Pixels.SignificantBits': bps,
                'Bioformats.Pixels.PhysicalSizeX': physicalSizeX?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeXUnit': (physicalSizeX) ? "µm" : null,
                'Bioformats.Pixels.PhysicalSizeY': physicalSizeY?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeYUnit': (physicalSizeY) ? "µm" : null,
                'Bioformats.Pixels.PhysicalSizeZ': physicalSizeZ?.value(UNITS.MICROMETER),
                'Bioformats.Pixels.PhysicalSizeZUnit': (physicalSizeZ) ? "µm" : null,
                'Bioformats.Pixels.TimeIncrement': timeIncrement?.value(UNITS.SECOND),
                'Bioformats.Pixels.TimeIncrementUnit': (timeIncrement) ? 's' : null,
                'Bioformats.Objective.NominalMagnification': magnification,
                'Bioformats.Image.AcquisitionDate': meta.getImageAcquisitionDate(biggestSeries).value
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
