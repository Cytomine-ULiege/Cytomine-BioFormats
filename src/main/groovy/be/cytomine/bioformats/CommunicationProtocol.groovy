package be.cytomine.bioformats

import be.cytomine.bioformats.worker.Convertor

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

import be.cytomine.bioformats.worker.Identifier
import be.cytomine.bioformats.worker.PropertyExtractor
import be.cytomine.bioformats.worker.Worker
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommunicationProtocol {

    private static final Logger log = LoggerFactory.getLogger(CommunicationProtocol.class)

    static Worker getWorkerFromInput(String input) {
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
                def output = getParamValue(json, 'output', null)
                def keepOriginalMetadata = getParamValue(json, 'keepOriginalMetadata', true)
                def compression = getParamValue(json, 'compression', "LZW")
                def flatten = getParamValue(json, 'flatten', true)
                def nPyramidResolutions = getParamValue(json, 'nPyramidResolutions', null)
                def pyramidScaleFactor = getParamValue(json, 'pyramidScaleFactor', 2)
                def legacyMode = getParamValue(json, 'legacyMode', true)
                return new Convertor(file, output, json.group, json.onlyBiggestSerie, keepOriginalMetadata,
                        compression, flatten, nPyramidResolutions, pyramidScaleFactor, legacyMode)
            case "properties":
                def includeRawProperties = getParamValue(json, 'includeRawProperties', false)
                def legacyMode = getParamValue(json, 'legacyMode', true)
                return new PropertyExtractor(file, includeRawProperties, legacyMode)
            default:
                throw new FormatException("Unknown action")
        }
    }

    static String getOutput(Worker w) {
        return JsonOutput.toJson(w.getOutput())
    }

    static String getOutput(Exception e) {
        return JsonOutput.toJson([error: e.getMessage()])
    }

    private static def getParamValue(def json, String key, def defaultValue) {
        if (key in json.keySet()) {
            return json[key]
        }
        return defaultValue
    }
}
