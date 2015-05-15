/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwarz.graph.model.GraphDefinition;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwarz.graph.model.GraphResult;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.ServerException;

public class GraphServiceImpl implements GraphServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(GraphServiceImpl.class);

	/**
	 * Return the right client.
	 * 
	 * @param msTimeOut
	 *            a time out used for remote connection
	 * @param local
	 *            pass true to force a local connection
	 * @return a remote client or null for local
	 * @throws URISyntaxException
	 *             thrown if the URI is wrong
	 */
	private GraphMultiClient getMultiClient(Integer msTimeOut, Boolean local)
			throws URISyntaxException {
		if (local != null && local)
			return null;
		if (msTimeOut == null)
			msTimeOut = 60000;
		return GraphManager.INSTANCE.getMultiClient(msTimeOut);
	}

	@Override
	public Set<String> list(Integer msTimeOut, Boolean local) {

		// Read the bases present in the remote nodes
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null)
				return GraphManager.INSTANCE.nameSet();
			else
				return client.list(msTimeOut, local);
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphDefinition createUpdateGraph(String graphName,
			GraphDefinition graphDef, Integer msTimeOut, Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null) {
				GraphManager.INSTANCE.createUpdateGraph(graphName, graphDef);
			} else
				client.createUpdateGraph(graphName, graphDef, msTimeOut, false);
			return graphDef;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	private GraphDefinition getGraphOrNotFound(String graphName)
			throws ServerException {
		GraphDefinition graphDef = GraphManager.INSTANCE.get(graphName);
		if (graphDef == null)
			throw new ServerException(Status.NOT_FOUND, "Graph not found: "
					+ graphName);
		return graphDef;
	}

	@Override
	public GraphResult getGraph(String graphName, Integer msTimeOut,
			Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null) {
				GraphDefinition graphDef = getGraphOrNotFound(graphName);
				GraphInstance graphInstance = GraphManager.INSTANCE
						.getGraphInstance(graphName);
				return new GraphResult(graphDef, graphInstance.getSize());
			} else
				return client.getGraph(graphName, msTimeOut, false);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	private GraphDefinition deleteGraphLocal(String graphName)
			throws IOException, URISyntaxException, ServerException {
		GraphDefinition base = getGraphOrNotFound(graphName);
		GraphManager.INSTANCE.delete(graphName);
		return base;
	}

	@Override
	public GraphDefinition deleteGraph(String graphName, Integer msTimeOut,
			Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null)
				return deleteGraphLocal(graphName);
			else
				return client.deleteGraph(graphName, msTimeOut, false);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode createUpdateNode(String graphName, String node_id,
			GraphNode node, Boolean upsert) {
		try {
			GraphManager.INSTANCE.getGraphInstance(graphName).createUpdateNode(
					node_id, node, upsert);
			return node;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> createUpdateNodes(String graphName,
			LinkedHashMap<String, GraphNode> nodes, Boolean upsert) {
		try {
			GraphManager.INSTANCE.getGraphInstance(graphName)
					.createUpdateNodes(nodes, upsert);
			return nodes.keySet();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	public final static TypeReference<Map<String, GraphNode>> MapStringGraphNodeTypeRef = new TypeReference<Map<String, GraphNode>>() {
	};

	@Override
	public Long createUpdateNodes(String graphName, Boolean upsert,
			InputStream inputStream) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			InputStreamReader irs = null;
			BufferedReader br = null;
			try {
				irs = new InputStreamReader(inputStream, "UTF-8");
				br = new BufferedReader(irs);
				long count = 0;
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty())
						continue;
					Map<String, GraphNode> nodeMap = JsonMapper.MAPPER
							.readValue(line, MapStringGraphNodeTypeRef);
					graphInstance.createUpdateNodes(nodeMap, upsert);
					count += nodeMap.size();
				}
				return count;
			} finally {
				if (br != null)
					IOUtils.closeQuietly(br);
				if (irs != null)
					IOUtils.closeQuietly(irs);
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	private GraphNode getNodeOrNotFound(GraphInstance graphInstance,
			String node_id) throws ServerException, IOException,
			URISyntaxException {
		GraphNode node = graphInstance.getNode(node_id);
		if (node != null)
			return node;
		throw new ServerException(Status.NOT_FOUND, "Graph node not found: "
				+ node_id);
	}

	@Override
	public GraphNode getNode(String graphName, String node_id) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			return getNodeOrNotFound(graphInstance, node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode deleteNode(String graphName, String node_id) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			GraphNode node = getNodeOrNotFound(graphInstance, node_id);
			graphInstance.deleteNode(node_id);
			return node;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode createEdge(String graphName, String node_id,
			String edge_type, String to_node_id) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			return graphInstance.createEdge(node_id, edge_type, to_node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode deleteEdge(String graphName, String node_id,
			String edge_type, String to_node_id) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			return graphInstance.deleteEdge(node_id, edge_type, to_node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public List<GraphNodeResult> requestNodes(String graphName,
			GraphRequest request) {
		try {
			GraphInstance graphInstance = GraphManager.INSTANCE
					.getGraphInstance(graphName);
			return graphInstance.request(request);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
