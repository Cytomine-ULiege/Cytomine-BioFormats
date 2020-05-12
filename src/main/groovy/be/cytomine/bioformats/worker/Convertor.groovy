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
import be.cytomine.bioformats.CytomineFile
import be.cytomine.bioformats.FormatException
import be.cytomine.bioformats.ImageConverter
import loci.common.DebugTools
import loci.formats.ImageReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Convertor extends Worker {
    private static final Logger log = LoggerFactory.getLogger(Convertor.class)

    private def convertedFiles

    boolean group
    boolean onlyBiggestSerie

    public Convertor(def file, def group, def onlyBiggestSerie) {
        this.file = file
        this.group = group
        this.onlyBiggestSerie = onlyBiggestSerie
    }

    def process() throws Exception {
        DebugTools.enableLogging("INFO")

        def reader = new ImageReader()
        reader.setId(file.absolutePath)

        def serieNumber = (onlyBiggestSerie) ? BioFormatsUtils.getBiggestSeries(reader) : -1

        File parentDirectory = file.parentFile
        File targetDirectory = new File(parentDirectory, "conversion")
        targetDirectory.mkdirs()
        targetDirectory.setReadable(true, false)
        targetDirectory.setWritable(true, false)

        ImageConverter ic = new ImageConverter(file, targetDirectory, serieNumber)
        List<CytomineFile> results = []
        try {
            results = ic.convert()
        }
        catch (Exception e) {
            e.printStackTrace()
            throw new FormatException("Error during conversion by BioFormats")
        }

        log.info("conversion result")
        log.info(results.toString())
        this.convertedFiles = results
    }



    @Override
    def getOutput() {
        return [files: convertedFiles*.toMap()]
    }
}
