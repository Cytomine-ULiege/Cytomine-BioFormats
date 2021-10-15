package be.cytomine.bioformats;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

        try {
            File cache = new File(BioFormatsUtils.CACHE_DIRECTORY);
            boolean ok = cache.mkdirs();
            if (ok) {
                cache.setReadable(true, false);
                cache.setExecutable(true, false);
            }
        } catch (SecurityException e) {
            log.error("Impossible to create cache directory");
            log.error(e.getMessage());
        }

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