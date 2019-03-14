import loci.formats.FormatException
import loci.formats.IFormatReader
import loci.formats.ImageReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ImageInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageInfo.class)

    int getLargestSerie(String filePath) {
        String id = filePath

        IFormatReader reader = new ImageReader()

        try {
            reader.setId(id)
            return readCoreMetadata(reader)
        } catch (FormatException | IOException exc) {
            reader.close()
            LOGGER.error("Failure during the reader initialization")
            LOGGER.error(exc)
            throw new FormatException(exc)
        }
    }

    int readCoreMetadata(IFormatReader reader) throws FormatException, IOException {
        int seriesCount = reader.getSeriesCount()
        LOGGER.info("Series count = " + seriesCount)

        int maximum = -1
        long maximumSize = -1

        for (int j = 0; j < seriesCount; j++) {
            reader.setSeries(j)

            int sizeX = reader.getSizeX()
            int sizeY = reader.getSizeY()

            // output basic metadata for series #i
            //println("Series #$j ");

            long area = (long) sizeX * (long) sizeY

            /*println("\tWidth = "+sizeX);
            println("\tHeight = "+sizeY);
            println("\tArea = "+area);*/

            if (area > maximumSize) {
                maximumSize = area
                maximum = j
            }
        }
        LOGGER.info("maximum is $maximum")

        return maximum
    }
}
