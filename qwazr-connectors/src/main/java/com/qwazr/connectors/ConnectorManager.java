/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.connectors;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.AbstractServer;

public class ConnectorManager {

	private final Map<String, AbstractConnector> connectors;

	private static final Logger logger = LoggerFactory
			.getLogger(ConnectorManager.class);

	public static volatile ConnectorManager INSTANCE = null;

	public static void load(AbstractServer server, File directory,
			String contextId) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ConnectorManager(directory, contextId);
	}

	private final String contextId;

	private ConnectorManager(File rootDirectory, String contextId)
			throws IOException {
		this.contextId = contextId;
		connectors = new ConcurrentHashMap<String, AbstractConnector>();
		File connectorFile = new File(rootDirectory, "connectors.json");
		if (!connectorFile.exists())
			return;
		if (!connectorFile.isFile())
			return;
		logger.info("Loading connector configuration file: "
				+ rootDirectory.getPath());
		ConnectorsConfiguration configuration = JsonMapper.MAPPER.readValue(
				connectorFile, ConnectorsConfiguration.class);
		if (configuration.connectors == null)
			return;
		for (AbstractConnector connector : configuration.connectors) {
			logger.info("Loading connector: " + connector.name);
			connector.load(contextId);
			add(connector);
		}
	}

	void add(AbstractConnector connector) {
		connectors.put(connector.name, connector);
	}

	protected void unload() {
		if (connectors == null)
			return;
		for (AbstractConnector connector : connectors.values()) {
			try {
				connector.unload(contextId);
			} catch (Exception e) {
				// This should never happen
				System.err.println(e);
			}
		}
		// Paranoid free
		connectors.clear();
	}

	public ConnectorMap getReadOnlyMap() {
		return new ConnectorMap();
	}

	public class ConnectorMap {

		public AbstractConnector get(String name) {
			return connectors.get(name);
		}
	}
}
