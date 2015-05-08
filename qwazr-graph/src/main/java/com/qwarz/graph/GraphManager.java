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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.process.GraphProcess;
import com.qwazr.cluster.client.ClusterMultiClient;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.ServerException;

public class GraphManager extends DirectoryJsonManager<GraphBase> {

	public static volatile GraphManager INSTANCE = null;

	public File directory;

	public final ExecutorService executor;

	public static void load(File directory) throws IOException,
			URISyntaxException, ServerException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new GraphManager(directory);
		for (String name : INSTANCE.nameSet())
			GraphProcess.load(name, INSTANCE.get(name));
	}

	private GraphManager(File directory) throws ServerException, IOException {
		super(directory, GraphBase.class);
		this.directory = directory;
		executor = Executors.newFixedThreadPool(8);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				executor.shutdown();
			}
		});
	}

	@Override
	public Set<String> nameSet() {
		return super.nameSet();
	}

	@Override
	public GraphBase get(String name) {
		return super.get(name);
	}

	@Override
	public void set(String name, GraphBase affinity) throws IOException,
			ServerException {
		super.set(name, affinity);
	}

	@Override
	public GraphBase delete(String name) throws ServerException {
		return super.delete(name);
	}

	GraphMultiClient getMultiClient(int msTimeOut) throws URISyntaxException {
		ClusterMultiClient clusterClient = ClusterManager.INSTANCE
				.getClusterClient();
		if (clusterClient == null)
			return null;
		return new GraphMultiClient(executor,
				clusterClient.getActiveNodes(GraphServer.SERVICE_NAME_GRAPH),
				msTimeOut);
	}
}
