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
package com.qwazr.analyzer.markdown;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.pegdown.PegDownProcessor;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServerException;

public class MarkdownProcessManager extends
		DirectoryJsonManager<MarkdownProcessorDefinition> {

	public static volatile MarkdownProcessManager INSTANCE = null;

	public static void load(AbstractServer server, File rootDirectory)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new MarkdownProcessManager(server, rootDirectory);

	}

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	private final Map<String, PegDownProcessor> processorMap;

	private MarkdownProcessManager(AbstractServer server, File rootDirectory)
			throws IOException {
		super(new File(rootDirectory, "markdown"),
				MarkdownProcessorDefinition.class);
		processorMap = new HashMap<String, PegDownProcessor>();
	}

	private PegDownProcessor getProcessor(String processorName)
			throws ServerException {
		rwl.r.lock();
		try {
			PegDownProcessor processor = processorMap.get(processorName);
			if (processor != null)
				return processor;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			PegDownProcessor processor = processorMap.get(processorName);
			if (processor != null)
				return processor;
			MarkdownProcessorDefinition processorDefinition = get(processorName);
			if (processorDefinition == null)
				throw new ServerException(Status.NOT_FOUND,
						"Process not found: " + processorName);
			processor = processorDefinition.getNewPegdownProcessor();
			processorMap.put(processorName, processor);
			return processor;
		} finally {
			rwl.w.unlock();
		}
	}

	public String toHtml(String processorName, String input)
			throws ServerException {
		return getProcessor(processorName).markdownToHtml(input);
	}

}
