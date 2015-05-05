/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.analyze.markdown;

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
