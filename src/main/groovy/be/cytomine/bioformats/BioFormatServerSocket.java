package be.cytomine.bioformats;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

public class BioFormatServerSocket {
    private static final Logger log = LoggerFactory.getLogger(BioFormatServerSocket.class);

    private void printBioFormatsInfo() {
        InputStream is;
        try {
            is = getClass().getClassLoader().getResourceAsStream("bioformats.properties");
            Properties props = new Properties();
            props.load(is);
            log.info("Use BioFormats " + props.getProperty("Bioformats-Version"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        int portNumber = 4321;
        if (args.length >= 1) {
            portNumber = Integer.parseInt(args[0]);
        }

        BioFormatServerSocket main = new BioFormatServerSocket();
        main.printBioFormatsInfo();

        log.info("Listen on port " + portNumber);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                RequestHandler w = new RequestHandler(serverSocket.accept());
                Thread t = new Thread(w);
                t.start();
            }
        } catch (IOException e) {
            log.error("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            log.error(e.getMessage());
        }
    }
}