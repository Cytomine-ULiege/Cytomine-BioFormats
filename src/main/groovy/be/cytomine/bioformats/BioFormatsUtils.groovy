package be.cytomine.bioformats

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

import loci.formats.ImageReader

class BioFormatsUtils {

    static int getBiggestSeries(ImageReader reader) {
        long biggestArea = 0
        long biggestSeries = 0

        int seriesCount = reader.getSeriesCount()
        for (series in (0..seriesCount - 1)) {
            reader.setSeries(series)

            long area = (long) reader.getSizeX() * (long) reader.getSizeY()
            if (area > biggestArea) {
                biggestArea = area
                biggestSeries = series
            }
        }

        return biggestSeries
    }

    static String removeExtension(String file) {
        if (file.endsWith(".ome.tif"))
            return file[0..-9]

        if (file.endsWith(".ome.tiff"))
            return file[0..-10]

        return file.substring(0, file.lastIndexOf("."))
    }
}
