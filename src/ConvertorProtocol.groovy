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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConvertorProtocol {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertorProtocol.class)

    String processInput(String theInput) {
        def response = [:]

        try {
            LOGGER.info("input: " + theInput)

            if (theInput == null || theInput == "")
                throw new FormatException("Error : Input is null or empty")

            def json = new JsonSlurper().parseText(theInput)
            if (json.size() == 0) throw new FormatException("Formatting error")

            if (json.keySet().sort() != ["group", "onlyBiggestSerie", "path"]) {
                response.put("error", "Some parameters of the JSON are invalid :" + json.keySet())
                return response.toString()
            }

            if (json.path == null || json.message == "")
                throw new FormatException("path is null or empty")

            File file = new File(json.path)
            if (!file.exists())
                throw new Exception("The file of path " + json.path + " doesn't exist")
            if (!file.canRead())
                throw new Exception("The file of path " + json.path + " is not readable")

            LOGGER.info("json: " + json.path + " " + json.group + " " + json.onlyBiggestSerie)

            def files = new Convertor().conversion(json.path, Boolean.parseBoolean(json.group), Boolean.parseBoolean(json.onlyBiggestSerie))
            response.put("files", files)
        } catch (Exception e) {
            response.put("error", e.toString())
        }

        return JsonOutput.toJson(response)
    }
}
