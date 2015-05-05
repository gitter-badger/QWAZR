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
package com.qwarz.graph.process;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensearchserver.client.ServerResource;
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerException;

public class GraphProcess {

	private static final Logger logger = LoggerFactory
			.getLogger(GraphProcess.class);

	static final String FIELD_NODE_ID = "node_id";
	static final String FIELD_PREFIX_PROPERTY = "prop.";
	static final String FIELD_PREFIX_EDGE = "edge.";

	/**
	 * @param server
	 * @return the right implementation
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ServerException
	 */
	private static GraphProcessInterface getImplementation(String graphName,
			ServerResource server) throws IOException, URISyntaxException,
			ServerException {
		if (server == null)
			return new GraphProcess3Impl(graphName);
		if (server.url != null)
			return new GraphProcess1Impl(server);
		else
			return new GraphProcess2Impl(server.time_out);
	}

	static String getPropertyField(String name) {
		return StringUtils.fastConcat(GraphProcess.FIELD_PREFIX_PROPERTY, name);
	}

	static String getEdgeField(String name) {
		return StringUtils.fastConcat(GraphProcess.FIELD_PREFIX_EDGE, name);
	}

	public static void createDataIndex(String graphName, GraphBase base)
			throws IOException, URISyntaxException, ServerException {
		getImplementation(graphName, base.data).createDataIndex(base);
	}

	public static void load(String graphName, GraphBase base)
			throws IOException, URISyntaxException, ServerException {
		getImplementation(graphName, base.data).load(base);
	}

	public static void deleteDataIndex(String graphName, GraphBase base)
			throws IOException, URISyntaxException, ServerException {
		getImplementation(graphName, base.data).deleteDataIndex(base);
	}

	public static void createUpdateNode(String graphName, GraphBase base,
			String node_id, GraphNode node, Boolean upsert) throws IOException,
			URISyntaxException, ServerException {
		if (node == null)
			return;
		GraphProcessInterface graphImpl = getImplementation(graphName,
				base.data);
		if (upsert != null && upsert) {
			// If the node already exists, we merge it
			try {
				node.add(graphImpl.getNode(base, node_id));
			} catch (ServerException e) {
				if (e.getStatusCode() != Status.NOT_FOUND.getStatusCode())
					throw e;
			}
		}
		graphImpl.createUpdateNode(base, node_id, node);
	}

	public static void createUpdateNodes(String graphName, GraphBase base,
			Map<String, GraphNode> nodes, Boolean upsert) throws IOException,
			URISyntaxException, ServerException {
		if (nodes == null || nodes.isEmpty())
			return;
		if (logger.isInfoEnabled())
			logger.info("Update " + nodes.size() + " node(s)");
		GraphProcessInterface graphImpl = getImplementation(graphName,
				base.data);
		if (upsert != null && upsert) {
			// If the nodes already exists, we merge them
			Map<String, GraphNode> dbNodes = graphImpl.getNodes(base,
					nodes.keySet());
			if (dbNodes != null) {
				for (Map.Entry<String, GraphNode> entry : nodes.entrySet()) {
					GraphNode dbNode = dbNodes.get(entry.getKey());
					if (dbNode != null)
						entry.getValue().add(dbNode);
				}
			}
		}
		graphImpl.createUpdateNodes(base, nodes);
	}

	public static GraphNode getNode(String graphName, GraphBase base,
			String node_id) throws IOException, URISyntaxException,
			ServerException {
		return getImplementation(graphName, base.data).getNode(base, node_id);
	}

	public static void deleteNode(String graphName, GraphBase base,
			String node_id) throws IOException, URISyntaxException,
			ServerException {
		getImplementation(graphName, base.data).deleteNode(base, node_id);
	}

	public static GraphNode createEdge(String graphName, GraphBase base,
			String node_id, String type, String to_node_id) throws IOException,
			URISyntaxException, ServerException {

		// Check if the type exists
		if (base.edge_types == null || base.edge_types.isEmpty())
			throw new ServerException(Status.BAD_REQUEST,
					"This base did not define any edge type");
		if (!base.isEdgeType(type))
			throw new ServerException(Status.BAD_REQUEST, "Unknown edge type: "
					+ type);

		GraphProcessInterface graphProcess = getImplementation(graphName,
				base.data);

		// Retrieve the node from the index
		GraphNode node = graphProcess.getNode(base, node_id);

		// Check if it does not already exists;
		if (!node.addEdge(type, to_node_id))
			return node;

		// We do the update
		graphProcess.createUpdateNode(base, node_id, node);
		return node;
	}

	public static GraphNode deleteEdge(String graphName, GraphBase base,
			String node_id, String type, String to_node_id) throws IOException,
			URISyntaxException, ServerException {
		GraphProcessInterface graphProcess = getImplementation(graphName,
				base.data);

		// Retrieve the node from the index
		GraphNode node = graphProcess.getNode(base, node_id);

		// Check if the edge exists;
		if (!node.removeEdge(type, to_node_id))
			return node;

		// We do the update
		graphProcess.createUpdateNode(base, node_id, node);
		return node;
	}

	public static List<GraphNodeResult> request(String graphName,
			GraphBase base, GraphRequest request) throws Exception {

		GraphProcessInterface graphProcess = getImplementation(graphName,
				base.data);
		List<GraphNodeResult> resultList = new ArrayList<GraphNodeResult>(
				request.getRowsOrDefault());

		// Execute the request
		graphProcess.request(base, request, resultList);
		Map<String, GraphNodeResult> nodeResultMap = new TreeMap<String, GraphNodeResult>();
		for (GraphNodeResult nodeResult : resultList)
			nodeResultMap.put(nodeResult.node_id, nodeResult);

		// Retrieve the nodes from the database
		Map<String, GraphNode> graphNodeMap = graphProcess.getNodes(base,
				nodeResultMap.keySet());
		if (graphNodeMap != null) {
			for (GraphNodeResult graphNodeResult : nodeResultMap.values()) {
				GraphNode graphNode = graphNodeMap.get(graphNodeResult.node_id);
				if (graphNode != null) {
					graphNodeResult.edges = graphNode.edges;
					graphNodeResult.properties = graphNode.properties;
				}
			}
		}
		return resultList;
	}

	public static class NodeScore implements Comparable<NodeScore> {

		final public String node_id;
		public double score;

		NodeScore(String node_id) {
			this.node_id = node_id;
			this.score = 0;
		}

		@Override
		/**
		 * Descending order by default
		 */
		public int compareTo(final NodeScore o) {
			return Double.compare(o.score, score);
		}

	}

}
