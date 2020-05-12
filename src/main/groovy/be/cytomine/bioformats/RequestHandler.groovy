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