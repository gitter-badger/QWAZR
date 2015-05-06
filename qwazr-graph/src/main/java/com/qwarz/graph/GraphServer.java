/**
 * Copyright 2015 OpenSearchServer Inc.
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
 */
package com.qwarz.graph;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.server.ServletApplication;

public class GraphServer extends AbstractServer {

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9093;
		serverDefinition.mainJarPath = "qwazr-graph.jar";
		serverDefinition.defaultDataDirPath = "qwazr/graph";
	}

	public final static String SERVICE_NAME_GRAPH = "graph";

	private GraphServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class GraphApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(ClusterServiceImpl.class);
			classes.add(GraphServiceImpl.class);
			return classes;
		}
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

	@Override
	protected RestApplication getRestApplication() {
		return new GraphApplication();
	}

	@Override
	protected ServletApplication getServletApplication() {
		return null;
	}

	public static void load(File dataDir) throws IOException {
		try {
			GraphManager.load(dataDir);
		} catch (URISyntaxException | ServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void load() throws IOException {
		File currentDataDir = getCurrentDataDir();
		ClusterServer.load(getWebServicePublicAddress(), currentDataDir, null);
		load(currentDataDir);
	}

	public static void main(String[] args) throws IOException, ParseException,
			ServletException {
		new GraphServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME_GRAPH);
	}

}
