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
import loci.common.DebugTools
import loci.formats.ImageReader
import loci.formats.Memoizer


class Identifier extends Worker {

    private String format = null

    Identifier(def file) {
        this.file = file
    }

    @Override
    def process() {
        DebugTools.enableLogging("INFO")

        def reader = new Memoizer(new ImageReader(), 0, new File(BioFormatsUtils.CACHE_DIRECTORY))
        reader.setId(this.file.absolutePath)
        this.format = reader.getFormat()
    }

    @Override
    def getOutput() {
        return [format: this.format]
    }
}
