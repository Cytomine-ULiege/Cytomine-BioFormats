/*
 * Application based on the OME-BIOFORMATS C++ library for image IO.
 * Copyright © 2006 - 2014 Open Microscopy Environment:
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


import loci.common.DebugTools
import loci.common.services.DependencyException
import loci.common.services.ServiceException
import loci.common.services.ServiceFactory
import loci.formats.IFormatReader
import loci.formats.ImageReader
import loci.formats.ImageWriter
import loci.formats.meta.IMetadata
import loci.formats.out.OMETiffWriter
import loci.formats.services.OMEXMLService
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * main class: the idea behind this tool:
 - first: convert the multidimensional images to ome.tiff format ===> one file will be created (multiplanes)
 - split the file created above in such away we obtain one image plane by file (mutlifiles ome.tiff)
 */

class Convertor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Convertor.class)
    private int serieNumber = -1


    def conversion(String input, boolean group, boolean onlyBiggestSerie) throws Exception {

        if (onlyBiggestSerie) {
            ImageInfo info = new ImageInfo()
            serieNumber = info.getLargestSerie(input)
        }


        def results = []
        if (group) {

            DebugTools.enableLogging("INFO");
            String out

            String parentFolder = input.substring(0, input.lastIndexOf("/"))

            //first, we create the convertor target directory
            File target_directory = createTargetDirectory(parentFolder)

            LOGGER.info(input)

            out = input.substring(input.lastIndexOf("/") + 1)
            /*out = out.substring(0, out.lastIndexOf("."))+"_converted";

            LOGGER.info(out);
            out = convert(input, out);
            LOGGER.info(out);

            LOGGER.info("[DONE]: First pass completed successfully");

            LOGGER.info("Loading file for the second pass in order to split it");
            Thread.sleep(4000);



            String output_directory = target_directory.getAbsolutePath()+"/";
            String basePath = output_directory+out.substring(0, out.lastIndexOf("."));

            split(out, basePath, spliting_mode);

            LOGGER.info("\t\t\t\t\n[DONE!]: The results of all the conversion process can be found in "+output_directory+" directory.");
            LOGGER.info("\n");
            int nb_files = target_directory.listFiles().length;
            LOGGER.info("\t\t\t\tNumber of files to be upload on server is: "+nb_files);

            if(nb_files < z*t*c)
                throw new Exception("Error during the conversion")

            LOGGER.info("[DONE!]");

            String[] names = target_directory.listFiles().collect {it.getAbsolutePath()};
            names.each { name ->
                def result = new JSONObject();
                result.put("path", name);
                name = name.substring(basePath.length())
                Pattern p = Pattern.compile("\\d+");
                Matcher m = p.matcher(name);
                if (m.find()) {
                    result.put("z", m.group());
                }
                if (m.find()) {
                    result.put("t", m.group());
                }
                if (m.find()) {
                    result.put("c", m.group());
                }
                results.add(result);
            }*/

            String output_directory = target_directory.getAbsolutePath() + "/"
            String basePath = output_directory + out.substring(0, out.lastIndexOf("."))

            ArrayList<String> args = []
            args << input
            args << "-series"
            args << "" + serieNumber + ""
            args << "-compression"
            args << "LZW"
            args << "-bigtiff"
            args << "-tilex"
            args << "256"
            args << "-tiley"
            args << "256"
            args << basePath + "_Z%z_C%c_T%t.tiff"

            LOGGER.info("args")
            LOGGER.info(args.join(" "))
            //System.sleep(10000)

            new loci.formats.tools.ImageConverter().testConvert(new ImageWriter(), (String[]) args.toArray())


            String[] names = target_directory.listFiles().collect { it.getAbsolutePath() }

            /*println "names size"
            println names.size()

            println names[0]*/
            if (names.size() == 1) {

                //File file =new File(basePath+"_Z%z_C%c_T%t.tiff");
                def command = "mv " + names[0] + " " + basePath + ".tiff"
                LOGGER.info(command)
                command.execute().waitFor()
                //file.renameTo(new File(basePath+".tiff"))

                names[0] = basePath + ".tiff"
            }
            //println names[0]

            names.each { name ->
                def result = [:]
                result.put("path", name)
                name = name.substring(basePath.length())
                Pattern p = Pattern.compile("\\d+")
                Matcher m = p.matcher(name)
                if (m.find()) {
                    result.put("z", m.group())
                }
                if (m.find()) {
                    result.put("c", m.group())
                }
                if (m.find()) {
                    result.put("t", m.group())
                }
                results.add(result)
            }
        } else {
            String output
            output = input.substring(input.lastIndexOf("/") + 1)
            if (output.lastIndexOf(".") > -1)
                output = output.substring(0, output.lastIndexOf("."))
            output += ".tiff"
            output = input.substring(0, input.lastIndexOf("/") + 1) + output

            loci.formats.tools.ImageConverter converter = new loci.formats.tools.ImageConverter()
            ArrayList<String> args = []
            args << "-bigtiff"
            args << "-compression"
            args << "LZW"
            args << "-tilex"
            args << "256"
            args << "-tiley"
            args << "256"

            if (onlyBiggestSerie) {
                args << "-series"
                args << "$serieNumber"
            }

            args << input
            args << output

            IFormatReader reader = new ImageReader()
            reader.setGroupFiles(true)
            reader.setMetadataFiltered(true)
            reader.setOriginalMetadataPopulated(true)
            reader.setId(input)


            LOGGER.info("args")
            LOGGER.info(args.join(" "))
            converter.testConvert(new ImageWriter(), (String[]) args.toArray())


            def result = [:]
            result.put("path", output)
            results.add(result)
        }


        LOGGER.info("conversion result")
        LOGGER.info(results.toString())

        return results
    }

    private static File createTargetDirectory(String parent) throws IOException, SecurityException {
        File target_directory = new File(parent + "/conversion")
        boolean created = false
        if (!target_directory.exists()) {
            LOGGER.info("creating directory: conversion")
            target_directory.mkdir()
            created = true
            if (created) {
                LOGGER.info("conversion was created ")
            }
        } else {
            FileUtils.cleanDirectory(target_directory)
            if (created) {
                LOGGER.error("conversion was cleaned ")
            }
        }
        """chmod -R 777 $target_directory""".execute().waitFor()
        return target_directory
    }

//    private String convert(String input, String out) throws DependencyException, ServiceException, loci.formats.FormatException, IOException {
//        //create the reader which parse the original file and store the data to the omexml metadata objet, and the writer to retrieve the stored data and
//        //write its to the target ome.tiff file
//        ImageReader reader = new ImageReader()
//        OMETiffWriter writer = new OMETiffWriter()
//
//        out += ".tiff"
//        LOGGER.info("Converting " + input + " to " + out + " ")
//
//        // record metadata to OME-XML format
//        //Creation of OMEXMLMetadata object
//        ServiceFactory factory = new ServiceFactory()
//        OMEXMLService service = factory.getInstance(OMEXMLService.class)
//        IMetadata omexmlMeta = service.createOMEXMLMetadata()
//
//        //attaching the OMEXMLMetadata object to the reader
//        reader.setMetadataStore(omexmlMeta)
//        reader.setId(input)
//        reader.setSeries(serieNumber)
//
//        //delete previous files
//        try {
//            File out_file = new File(out)
//            if (out_file.exists()) {
//                out_file.delete()
//                LOGGER.info("previous temporary file " + out_file.getName() + " is deleted!")
//            }
//        } catch (Exception e) {
//            e.printStackTrace()
//            LOGGER.error("delete of previous file is failed!")
//            System.exit(0)
//        }
//
//        // configure OME-TIFF writer
//        // The OMEXMLMetadata object is then fed to the OMETiffWriter, which extracts the appropriate OME-XML string
//        // and embeds it into the OME-TIFF file properly
//        writer.setMetadataRetrieve(omexmlMeta)
//        writer.setId(out)
//        writer.setCompression("LZW")
//        //if(serieNumber > -1) writer.setSeries(serieNumber);
//
//        // write out image planes.
//        int seriesCount = reader.getSeriesCount()
//        LOGGER.info("Series count value: " + seriesCount)
//
//        if (serieNumber == -1) {
//            for (int s = 0; s < seriesCount; s++) {
//                reader.setSeries(s)
//                writer.setSeries(s)
//                int planeCount = reader.getImageCount()
//                LOGGER.info("\t\t\t\tNumber of image planes : " + planeCount)
//                for (int p = 0; p < planeCount; p++) {
//                    byte[] plane = reader.openBytes(p)
//                    // write planes to separate files and
//                    writer.saveBytes(p, plane)
//                    System.out.print(".")
//                }
//            }
//        } else {
//            int s = serieNumber
//            reader.setSeries(s)
//            writer.setSeries(s)
//            int planeCount = reader.getImageCount()
//            LOGGER.info("\t\t\t\tNumber of image planes : " + planeCount)
//            for (int p = 0; p < planeCount; p++) {
//                byte[] plane = reader.openBytes(p)
//                // write planes to separate files and
//                writer.saveBytes(p, plane)
//                System.out.print(".")
//            }
//        }
//
//        writer.close()
//        reader.close()
//        return out
//    }
//
//    private void split(String input, String basePath, String spliting_mode) throws loci.formats.FormatException, IOException {
//        //still use the old class because we have a bug in je jar 5.0.1 http://trac.openmicroscopy.org/ome/ticket/12268
//        ImageConverter converter = new ImageConverter(input, basePath + spliting_mode)
//
//        //ImageConverter converter = new ImageConverter();
//        String[] args = new String[2]
//        args[0] = input
//        args[1] = basePath + spliting_mode
//
//        IFormatReader reader = new ImageReader()
//        reader.setGroupFiles(true)
//        reader.setMetadataFiltered(true)
//        reader.setOriginalMetadataPopulated(true)
//        reader.setId(input)
//
//
//        if (!converter.testConvert(new ImageWriter(), false))
//        //if (!converter.testConvert(new ImageWriter(), args))
//            System.exit(1)
//        //get image proprieties
//
//        z = reader.getSizeZ()
//        t = reader.getSizeT()
//        c = reader.getEffectiveSizeC()
//
//        LOGGER.info("\n\t\t\t\t========================Image proprieties=======================\n")
//        LOGGER.info("\t\t\t\tZ-section : " + z)
//        LOGGER.info("\t\t\t\tT-Timepoints : " + t)
//        LOGGER.info("\t\t\t\tC-Channel : " + c)
//        LOGGER.info("\n\t\t\t\t================================================================\n")
//    }

}
