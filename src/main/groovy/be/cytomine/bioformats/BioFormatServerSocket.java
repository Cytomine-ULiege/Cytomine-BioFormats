package be.cytomine.bioformats;

/*
 * Cytomine-Bioformats, a wrapper to link Bio-formats with Cytomine.
 * Copyright (C) 2015-2021 cytomine.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 *  your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class BioFormatServerSocket {
    private static final Logger log = LogManager.getLogger(BioFormatServerSocket.class);

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
        log.info("Starting Cytomine-BioFormats server...");

        int portNumber = 4321;
        if (args.length >= 1) {
            portNumber = Integer.parseInt(args[0]);
        }

        int poolSize = 4;
        if (args.length >= 2) {
            poolSize = Integer.parseInt(args[1]);
        }
        if (poolSize == -1) {
            poolSize = Runtime.getRuntime().availableProcessors();
        }
        log.info("Thread pool size: " + poolSize);
        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);

        BioFormatServerSocket main = new BioFormatServerSocket();
        main.printBioFormatsInfo();

        log.info("Listen on port " + portNumber);

        int requestNumber = 0;
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
                RequestHandler w = new RequestHandler(serverSocket.accept(), ++requestNumber);
                executor.submit(w);
            }
        } catch (IOException e) {
            log.error("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            log.error(e.getMessage());
        }
    }
}