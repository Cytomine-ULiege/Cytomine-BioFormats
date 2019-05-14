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

import be.cytomine.bioformats.worker.Convertor
import be.cytomine.bioformats.worker.Identifier
import be.cytomine.bioformats.worker.PropertyExtractor
import be.cytomine.bioformats.worker.Worker

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommunicationProtocol {

    private static final Logger log = LoggerFactory.getLogger(CommunicationProtocol.class)

    public static Worker getWorkerFromInput(String input) {
        log.info("Identify worker for input ${input}")

        if (input == null || input == "")
            throw new FormatException("Request is null or empty")

        def json = new JsonSlurper().parseText(input)
        if (json.size() == 0)
            throw new FormatException("Request is not json")

        def keys = json.keySet()
        if (!keys.contains("path"))
            throw new FormatException("Request is missing 'path' key")

        if (!keys.contains("action"))
            throw new FormatException("Request is missing 'action' key")

        File file = new File(json.path)
        if (!file.exists())
            throw new FileNotFoundException("The file of path ${json.path} doesn't exist")
        if (!file.canRead())
            throw new FileNotFoundException("The file of path ${json.path} is not readable")

        switch (json.action) {
            case "identify":
                return new Identifier(file)
            case "convert":
                if (!keys.containsAll(["group", "onlyBiggestSerie", "path"]))
                    throw new FormatException("Missing JSON parameters required for this action.")
                return new Convertor(file, json.group, json.onlyBiggestSerie)
            case "properties":
                return new PropertyExtractor(file, json.includeRawProperties ?: false)
            default:
                throw new FormatException("Unknown action")
        }
    }

    public static String getOutput(Worker w) {
        return JsonOutput.toJson(w.getOutput())
    }

    public static String getOutput(Exception e) {
        return JsonOutput.toJson([error: e.getMessage()])
    }
}
