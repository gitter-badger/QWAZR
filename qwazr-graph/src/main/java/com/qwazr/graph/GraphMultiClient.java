/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.graph;

import com.qwazr.graph.model.*;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.server.WebAppExceptionHolder;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class GraphMultiClient extends JsonMultiClientAbstract<String, GraphSingleClient>
		implements GraphServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(GraphMultiClient.class);

	GraphMultiClient(ExecutorService executor, String[] urls, Integer msTimeOut) throws URISyntaxException {
		super(executor, new GraphSingleClient[urls.length], urls, msTimeOut);
	}

	@Override
	public Set<String> list() {

		try {

			// We merge the result of all the nodes
			TreeSet<String> globalSet = new TreeSet<String>();

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (GraphSingleClient client : this) {
				threads.add(new ProcedureExceptionCatcher() {

					@Override
					public void execute() throws Exception {
						Set<String> set = client.list();
						synchronized (globalSet) {
							if (set != null)
								globalSet.addAll(set);
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return globalSet;

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphDefinition createUpdateGraph(String graphName, GraphDefinition graphDef) {

		try {

			List<FunctionExceptionCatcher<GraphDefinition>> threads = new ArrayList<>(size());
			for (GraphSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<GraphDefinition>() {
					@Override
					public GraphResult execute() throws Exception {
						return client.createUpdateGraph(graphName, graphDef);
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return ThreadUtils.getFirstResult(threads);

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphResult getGraph(String graphName) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(logger);

		for (GraphSingleClient client : this) {
			try {
				return client.getGraph(graphName);
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404)
					logger.warn(e.getMessage(), e);
				else
					exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return null;
	}

	@Override
	public GraphDefinition deleteGraph(String graphName) {

		try {

			List<FunctionExceptionCatcher<GraphDefinition>> threads = new ArrayList<>(size());
			for (GraphSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<GraphDefinition>() {
					@Override
					public GraphDefinition execute() throws Exception {
						try {
							return client.deleteGraph(graphName);
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() == 404)
								return null;
							logger.warn(e.getMessage(), e);
							throw e;
						}
					}
				});
			}
			ThreadUtils.invokeAndJoin(executor, threads);
			GraphDefinition graphDef = ThreadUtils.getFirstResult(threads);
			if (graphDef == null)
				throw new ServerException(Status.NOT_FOUND, "Graph not found: " + graphName);
			return graphDef;

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> createUpdateNodes(String db_name, LinkedHashMap<String, GraphNode> nodes, Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long createUpdateNodes(String db_name, Boolean upsert, InputStream inpustStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createUpdateNode(String db_name, String node_id, GraphNode node, Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode getNode(String db_name, String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteNode(String db_name, String node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createEdge(String db_name, String node_id, String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteEdge(String db_name, String node_id, String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GraphNodeResult> requestNodes(String db_name, GraphRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GraphSingleClient newClient(String url, Integer msTimeOut) throws URISyntaxException {
		return new GraphSingleClient(url, msTimeOut);
	}

}
