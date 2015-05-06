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

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class GraphMultiClient extends
		JsonMultiClientAbstract<GraphSingleClient> implements
		GraphServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(GraphMultiClient.class);

	GraphMultiClient(ExecutorService executor, Collection<String> urls,
			int msTimeOut) throws URISyntaxException {
		super(executor, new GraphSingleClient[urls.size()], urls, msTimeOut);
	}

	@Override
	public Set<String> list(Boolean local) {

		try {

			// If not global, just request the local node
			if (local != null && local) {
				GraphSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
				if (client == null)
					throw new ServerException(Status.NOT_ACCEPTABLE,
							"Node not valid: "
									+ ClusterManager.INSTANCE.myAddress);
				return client.list(true);
			}

			// We merge the result of all the nodes
			TreeSet<String> globalSet = new TreeSet<String>();

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (GraphSingleClient client : this) {
				threads.add(new ProcedureExceptionCatcher() {

					@Override
					public void execute() throws Exception {
						Set<String> set = client.list(true);
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
	public GraphBase createUpdateBase(String db_name, GraphBase base,
			Boolean local) {

		try {

			if (local != null && local) {
				GraphSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
				if (client == null)
					throw new ServerException(Status.NOT_ACCEPTABLE,
							"Node not valid: "
									+ ClusterManager.INSTANCE.myAddress);
				return client.createUpdateBase(db_name, base, true);
			}

			List<FunctionExceptionCatcher<GraphBase>> threads = new ArrayList<>(
					size());
			for (GraphSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<GraphBase>() {
					@Override
					public GraphBase execute() throws Exception {
						return client.createUpdateBase(db_name, base, true);
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
	public GraphBase getBase(String db_name, Boolean local) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (GraphSingleClient client : this) {
			try {
				return client.getBase(db_name, true);
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
	public GraphBase deleteBase(String db_name, Boolean local) {

		try {

			// Is it local ?
			if (local != null && local) {
				GraphSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
				if (client == null)
					throw new ServerException(Status.NOT_ACCEPTABLE,
							"Node not valid: "
									+ ClusterManager.INSTANCE.myAddress);
				return client.deleteBase(db_name, true);
			}

			List<FunctionExceptionCatcher<GraphBase>> threads = new ArrayList<>(
					size());
			for (GraphSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<GraphBase>() {
					@Override
					public GraphBase execute() throws Exception {
						try {
							return client.deleteBase(db_name, true);
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
			GraphBase base = ThreadUtils.getFirstResult(threads);
			if (base == null)
				throw new ServerException(Status.NOT_FOUND, "Base not found: "
						+ db_name);
			return base;

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> createUpdateNodes(String db_name,
			LinkedHashMap<String, GraphNode> nodes, Boolean upsert) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long createUpdateNodes(String db_name, Boolean upsert,
			InputStream inpustStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode createUpdateNode(String db_name, String node_id,
			GraphNode node, Boolean upsert) {
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
	public GraphNode createEdge(String db_name, String node_id,
			String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphNode deleteEdge(String db_name, String node_id,
			String edge_type, String to_node_id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<GraphNodeResult> requestNodes(String db_name,
			GraphRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GraphSingleClient newClient(String url, int msTimeOut)
			throws URISyntaxException {
		return new GraphSingleClient(url, msTimeOut);
	}

}