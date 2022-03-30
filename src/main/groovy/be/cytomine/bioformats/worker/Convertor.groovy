/*
 * Cytomine-Bioformats, a wrapper to link Bio-formats with Cytomine.
 * Copyright (C) 2015-2021 cytomine.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 *  your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package be.cytomine.bioformats.worker

import be.cytomine.bioformats.BioFormatsUtils
import be.cytomine.bioformats.FormatException
import be.cytomine.bioformats.ImageConverter
import loci.formats.ImageReader
import loci.formats.Memoizer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Convertor extends Worker {
    private static final Logger log = LogManager.getLogger(Convertor.class)

    private def convertedFiles
    private File output = null

    Boolean legacyMode

    String compression
    Boolean group
    Boolean onlyBiggestSeries
    Boolean keepOriginalMetadata
    Boolean flatten
    Boolean applyLUTs
    Integer nPyramidResolutions = 1
    Integer pyramidScaleFactor = 1
    Integer tileSize

    Convertor(File input, String output, Boolean group, Boolean onlyBiggestSeries,
              Boolean keepOriginalMetadata, String compression, Boolean flatten,
              Integer nPyramidResolutions, Integer pyramidScaleFactor, Integer tileSize,
              Boolean applyLUTs, Boolean legacyMode) {
        this.file = input
        if (output != null) {
            this.output = new File(output)
        }
        this.group = group
        this.onlyBiggestSeries = onlyBiggestSeries
        this.keepOriginalMetadata = keepOriginalMetadata
        this.compression = compression
        this.flatten = flatten
        if (!this.flatten) {
            this.nPyramidResolutions = nPyramidResolutions
            this.pyramidScaleFactor = pyramidScaleFactor ?: 2
        }
        this.tileSize = tileSize
        this.applyLUTs = applyLUTs
        this.legacyMode = legacyMode
    }

    def process() throws Exception {
        def reader = new Memoizer(new ImageReader(), 0, new File(BioFormatsUtils.CACHE_DIRECTORY))
        reader.setId(file.absolutePath)

        def serieNumber = (onlyBiggestSeries) ? BioFormatsUtils.getBiggestSeries(reader) : -1

        if (this.legacyMode || this.output == null) {
            File parentDirectory = file.parentFile
            output = new File(parentDirectory, "conversion")
            output.mkdirs()
            output.setReadable(true, false)
            output.setWritable(true, false)
        } else {
            if (output.path.endsWith(".EPTIFF")) {
                // If we want an exploded pyramidal tiff (equivalent to legacy mode)
                output.mkdirs()
                output.setReadable(true, false)
                output.setWritable(true, false)
            }
        }


        ImageConverter ic = new ImageConverter(file, output, serieNumber, compression,
                keepOriginalMetadata, flatten, nPyramidResolutions, pyramidScaleFactor,
                tileSize, applyLUTs)
        List<File> results
        try {
            results = ic.convert()
        }
        catch (Exception e) {
            e.printStackTrace()
            throw new FormatException("Error during conversion by BioFormats")
        }

        if (this.legacyMode) {
            log.info("conversion result: " + results.toString())
            this.convertedFiles = results
        } else {
            this.convertedFiles = [output]
        }
    }


    @Override
    def getOutput() {
        if (this.legacyMode) {
            return [files: convertedFiles*.toMap()]
        }
        return [file: output.absolutePath]
    }
}
