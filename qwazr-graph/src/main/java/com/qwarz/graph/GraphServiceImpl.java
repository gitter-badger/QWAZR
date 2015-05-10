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
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwarz.graph.process.GraphProcess;
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
	public GraphBase createUpdateBase(String db_name, GraphBase base,
			Integer msTimeOut, Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null) {
				GraphManager.INSTANCE.set(db_name, base);
				GraphProcess.createDataIndex(db_name, base);
			} else
				client.createUpdateBase(db_name, base, msTimeOut, false);
			return base;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	private GraphBase getBaseOrNotFound(String db_name) throws ServerException {
		GraphBase base = GraphManager.INSTANCE.get(db_name);
		if (base != null)
			return base;
		throw new ServerException(Status.NOT_FOUND, "Graph base not found: "
				+ db_name);
	}

	@Override
	public GraphBase getBase(String db_name, Integer msTimeOut, Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null)
				return getBaseOrNotFound(db_name);
			else
				return client.getBase(db_name, msTimeOut, false);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	private GraphBase deleteBaseLocal(String db_name) throws IOException,
			URISyntaxException, ServerException {
		GraphBase base = getBaseOrNotFound(db_name);
		GraphProcess.deleteDataIndex(db_name, base);
		GraphManager.INSTANCE.delete(db_name);
		return base;
	}

	@Override
	public GraphBase deleteBase(String db_name, Integer msTimeOut, Boolean local) {
		try {
			GraphMultiClient client = getMultiClient(msTimeOut, local);
			if (client == null)
				return deleteBaseLocal(db_name);
			else
				return client.deleteBase(db_name, msTimeOut, false);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode createUpdateNode(String db_name, String node_id,
			GraphNode node, Boolean upsert) {
		try {
			GraphProcess.createUpdateNode(db_name, getBaseOrNotFound(db_name),
					node_id, node, upsert);
			return node;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> createUpdateNodes(String db_name,
			LinkedHashMap<String, GraphNode> nodes, Boolean upsert) {
		try {
			GraphProcess.createUpdateNodes(db_name, getBaseOrNotFound(db_name),
					nodes, upsert);
			return nodes.keySet();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	public final static TypeReference<Map<String, GraphNode>> MapStringGraphNodeTypeRef = new TypeReference<Map<String, GraphNode>>() {
	};

	@Override
	public Long createUpdateNodes(String db_name, Boolean upsert,
			InputStream inputStream) {
		try {
			GraphBase base = getBaseOrNotFound(db_name);
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
					GraphProcess.createUpdateNodes(db_name, base, nodeMap,
							upsert);
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

	private GraphNode getNodeOrNotFound(String db_name, GraphBase base,
			String node_id) throws ServerException, IOException,
			URISyntaxException {
		GraphNode node = GraphProcess.getNode(db_name, base, node_id);
		if (node != null)
			return node;
		throw new ServerException(Status.NOT_FOUND, "Graph node not found: "
				+ node_id);
	}

	@Override
	public GraphNode getNode(String db_name, String node_id) {
		try {
			GraphBase base = getBaseOrNotFound(db_name);
			return getNodeOrNotFound(db_name, base, node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode deleteNode(String db_name, String node_id) {
		try {
			GraphBase base = getBaseOrNotFound(db_name);
			GraphNode node = getNodeOrNotFound(db_name, base, node_id);
			GraphProcess.deleteNode(db_name, base, node_id);
			return node;
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode createEdge(String db_name, String node_id,
			String edge_type, String to_node_id) {
		try {
			return GraphProcess.createEdge(db_name, getBaseOrNotFound(db_name),
					node_id, edge_type, to_node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public GraphNode deleteEdge(String db_name, String node_id,
			String edge_type, String to_node_id) {
		try {
			return GraphProcess.deleteEdge(db_name, getBaseOrNotFound(db_name),
					node_id, edge_type, to_node_id);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public List<GraphNodeResult> requestNodes(String db_name,
			GraphRequest request) {
		try {
			return GraphProcess.request(db_name, getBaseOrNotFound(db_name),
					request);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
