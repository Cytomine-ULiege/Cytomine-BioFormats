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
