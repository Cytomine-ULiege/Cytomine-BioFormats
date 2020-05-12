package be.cytomine.bioformats

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
