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

class CytomineFile extends File {

    def c
    def z
    def t
    def channelName

    CytomineFile(String pathname) {
        super(pathname)
    }

    CytomineFile(String pathname, def c, def z, def t) {
        super(pathname)
        setDimensions(c, z, t)
    }

    CytomineFile(String pathname, def c, def z, def t, def channelName) {
        super(pathname)
        setDimensions(c, z, t)
        this.channelName = channelName
    }

    def setDimensions(def c, def z, def t) {
        this.c = c
        this.z = z
        this.t = t
    }

    String toString() {
        return "$absolutePath | ($c, $z, $t) | channel name: $channelName"
    }

    Map toMap() {
        return [
                path: absolutePath,
                c: c,
                z: z,
                t: t,
                channelName: channelName
        ]
    }
}
