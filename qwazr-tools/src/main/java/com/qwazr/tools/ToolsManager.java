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
package com.qwazr.tools;

import com.fasterxml.jackson.core.JsonParseException;
import com.qwazr.utils.TrackedFile;
import com.qwazr.utils.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class ToolsManager implements TrackedFile.FileEventReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ToolsManager.class);

    public static volatile ToolsManager INSTANCE = null;

    public static void load(File directory) throws IOException {
	if (INSTANCE != null)
	    throw new IOException("Already loaded");
	INSTANCE = new ToolsManager(directory);
    }

    private final File rootDirectory;
    private final File toolsFile;
    private final TrackedFile trackedFile;

    private final Map<String, AbstractTool> tools;
    private volatile Map<String, AbstractTool> cachedTools;

    private ToolsManager(File rootDirectory) throws IOException {
	cachedTools = null;
	tools = new HashMap<String, AbstractTool>();
	this.rootDirectory = rootDirectory;
	toolsFile = new File(rootDirectory, "tools.json");
	trackedFile = new TrackedFile(this, toolsFile);
	trackedFile.check();
    }

    public void load() throws IOException {
	tools.clear();
	logger.info("Loading tools configuration file: " + toolsFile.getAbsolutePath());
	ToolsConfiguration configuration = JsonMapper.MAPPER.readValue(toolsFile, ToolsConfiguration.class);
	if (configuration.tools != null) {
	    for (AbstractTool tool : configuration.tools) {
		logger.info("Loading tool: " + tool.name);
		tool.load(rootDirectory);
		tools.put(tool.name, tool);
	    }
	}
	cachedTools = new HashMap<String, AbstractTool>(tools);
    }

    public void unload() {
	for (AbstractTool tool : tools.values()) {
	    try {
		tool.unload();
	    } catch (Exception e) {
		// This should never happen
		logger.warn(e.getMessage(), e);
	    }
	}
	tools.clear();
	cachedTools = Collections.EMPTY_MAP;
    }

    public AbstractTool get(String name) throws IOException {
	trackedFile.check();
	return cachedTools.get(name);
    }

}
