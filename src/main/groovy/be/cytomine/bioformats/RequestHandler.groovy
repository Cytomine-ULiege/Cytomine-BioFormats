package be.cytomine.bioformats

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

import be.cytomine.bioformats.worker.Worker

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RequestHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class)

    private Socket client

    RequestHandler(Socket client) {
        this.client = client
    }

    void run() {
        log.info("Run request handler")
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()))
            PrintWriter output = new PrintWriter(client.getOutputStream(), true)
            String inputLine, outputLine

            while ((inputLine = input.readLine()) != null) {
                try {
                    log.info("Received input $inputLine")
                    Worker w = CommunicationProtocol.getWorkerFromInput(inputLine)

                    w.process()

                    outputLine = CommunicationProtocol.getOutput(w)
                    log.info("Return: " + outputLine)
                }
                catch(Exception e) {
                    outputLine = CommunicationProtocol.getOutput(e)
                    log.error("Error: " + outputLine)
                }
                output.println(outputLine)
            }
        } catch (IOException e) {
            log.error("IO Exception in request handler")
        }
    }
}