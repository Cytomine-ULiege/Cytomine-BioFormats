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

import loci.formats.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.io.*;

public class BioFormatServerSocket {

    //Use BioFormat v5.3.3

    private static final Logger LOGGER = LoggerFactory.getLogger(BioFormatServerSocket.class);

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            LOGGER.error("Usage: java BioFormat <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);

        LOGGER.info("initialization : done");
        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
        ) {

            while(true) {
                ClientWorker w;
                w = new ClientWorker(serverSocket.accept());
                Thread t = new Thread(w);
                t.start();
            }

        } catch (IOException e) {
            LOGGER.error("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            LOGGER.error(e.getMessage());
        }

        /*ConvertorProtocol protocol = new ConvertorProtocol();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        protocol.processInput("{path:\"/tmp/slice-2_DAPI-NeuN-AT8.czi\",group:true,onlyBiggestSerie:true}");
        //protocol.processInput("{path:\"/tmp/vsi/Image_02_controle_posFOXP3.vsi\",group:false,onlyBiggestSerie:true}");

        //protocol.processInput("{path:\"/home/hoyoux/Desktop/images_test_iip/VSI/Movie_763.vsi\"}");
        //protocol.processInput("{path:\"/home/hoyoux/Desktop/images_test_iip/DICOM/CHEVALSHETL.CHEYENNE-FERA_/IMAGES/IM2\"}");

        /*try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        protocol.processInput("{path:\"/home/hoyoux/Desktop/test/multi-channel.ome_converted.tiff\"}");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        protocol.processInput("{path:\"/home/hoyoux/Desktop/test/multi-channel-4D-series.ome_converted.tiff\"}");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        protocol.processInput("{path:\"/home/hoyoux/Desktop/test/Zeiss-1-Merged_converted.tiff\"}");*/
    }
}