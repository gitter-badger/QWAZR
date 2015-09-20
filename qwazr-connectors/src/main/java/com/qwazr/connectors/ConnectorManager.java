/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.connectors;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.qwazr.utils.TrackedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.json.JsonMapper;

public class ConnectorManager implements TrackedFile.FileEventReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorManager.class);

    public static volatile ConnectorManager INSTANCE = null;

    public static void load(File directory) throws IOException {
	if (INSTANCE != null)
	    throw new IOException("Already loaded");
	INSTANCE = new ConnectorManager(directory);
    }

    private final File rootDirectory;
    private final File connectorsFile;
    private final TrackedFile trackedFile;

    private final Map<String, AbstractConnector> connectors;
    private volatile Map<String, AbstractConnector> cachedConnectors;

    private ConnectorManager(File rootDirectory) throws IOException {
	cachedConnectors = null;
	connectors = new HashMap<String, AbstractConnector>();
	this.rootDirectory = rootDirectory;
	connectorsFile = new File(rootDirectory, "connectors.json");
	trackedFile = new TrackedFile(this, connectorsFile);
	trackedFile.check();
    }

    public void load() throws IOException {
	connectors.clear();
	logger.info("Loading connectors configuration file: " + connectorsFile.getAbsolutePath());
	ConnectorsConfiguration configuration = JsonMapper.MAPPER
			.readValue(connectorsFile, ConnectorsConfiguration.class);
	if (configuration.connectors != null) {
	    for (AbstractConnector connector : configuration.connectors) {
		logger.info("Loading connector: " + connector.name);
		connector.load(rootDirectory);
		connectors.put(connector.name, connector);
	    }
	}
	cachedConnectors = new HashMap<String, AbstractConnector>(connectors);
    }

    public void unload() {
	for (AbstractConnector connector : connectors.values()) {
	    try {
		connector.unload();
	    } catch (Exception e) {
		// This should never happen
		logger.warn(e.getMessage(), e);
	    }
	}
	connectors.clear();
	cachedConnectors = Collections.EMPTY_MAP;
    }

    public AbstractConnector get(String name) throws IOException {
	trackedFile.check();
	return cachedConnectors.get(name);
    }
}
