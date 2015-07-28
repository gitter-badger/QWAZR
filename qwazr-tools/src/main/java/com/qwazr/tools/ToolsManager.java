/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.tools;

import com.qwazr.utils.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ToolsManager {

	private static final Logger logger = LoggerFactory
			.getLogger(ToolsManager.class);

	public static volatile ToolsManager INSTANCE = null;

	public static void load(File directory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ToolsManager(directory);
	}

	private final Map<String, AbstractTool> tools;

	private ToolsManager(File rootDirectory) throws IOException {
		tools = new ConcurrentHashMap<String, AbstractTool>();
		File toolsFile = new File(rootDirectory, "tools.json");
		if (!toolsFile.exists())
			return;
		if (!toolsFile.isFile())
			return;
		logger.info("Loading tools configuration file: "
				+ toolsFile.getAbsolutePath());
		ToolsConfiguration configuration = JsonMapper.MAPPER.readValue(
				toolsFile, ToolsConfiguration.class);
		if (configuration.tools == null)
			return;
		for (AbstractTool tool : configuration.tools) {
			logger.info("Loading tool: " + tool.name);
			tool.load(rootDirectory);
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
				tool.unload();
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
