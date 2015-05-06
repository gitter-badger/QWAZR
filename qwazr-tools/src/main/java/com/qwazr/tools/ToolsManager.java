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
package com.qwazr.tools;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.AbstractServer;

public class ToolsManager {

	private static final Logger logger = LoggerFactory
			.getLogger(ToolsManager.class);

	public static volatile ToolsManager INSTANCE = null;

	public static void load(AbstractServer server, File directory,
			String contextId) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ToolsManager(directory, contextId);
	}

	private final String contextId;

	private final Map<String, AbstractTool> tools;

	private ToolsManager(File rootDirectory, String contextId)
			throws IOException {
		this.contextId = contextId;
		tools = new ConcurrentHashMap<String, AbstractTool>();
		File connectorFile = new File(rootDirectory, "connectors.json");
		if (!connectorFile.exists())
			return;
		if (!connectorFile.isFile())
			return;
		logger.info("Loading tools configuration file: "
				+ rootDirectory.getPath());
		ToolsConfiguration configuration = JsonMapper.MAPPER.readValue(
				connectorFile, ToolsConfiguration.class);
		if (configuration.tools == null)
			return;
		for (AbstractTool tool : configuration.tools) {
			logger.info("Loading tool: " + tool.name);
			tool.load(contextId);
			add(tool);
		}
	}

	void add(AbstractTool tool) {
		tools.put(tool.name, tool);
	}

	protected void unload() {
		if (tools == null)
			return;
		for (AbstractTool tool : tools.values()) {
			try {
				tool.unload(contextId);
			} catch (Exception e) {
				// This should never happen
				System.err.println(e);
			}
		}
		// Paranoid free
		tools.clear();
	}

	public ToolMap getReadOnlyMap() {
		return new ToolMap();
	}

	public class ToolMap {

		public AbstractTool get(String name) {
			return tools.get(name);
		}
	}
}
