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
 **/
package com.qwarz.graph;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwarz.database.CollectorInterface.LongCounter;
import com.qwarz.database.FieldInterface.FieldDefinition;
import com.qwarz.database.Query;
import com.qwarz.database.Query.OrGroup;
import com.qwarz.database.Query.QueryHook;
import com.qwarz.database.Query.TermQuery;
import com.qwarz.database.Table;
import com.qwarz.database.UniqueKey;
import com.qwarz.graph.model.GraphDefinition;
import com.qwarz.graph.model.GraphDefinition.PropertyTypeEnum;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class GraphInstance {

	private static final Logger logger = LoggerFactory
			.getLogger(GraphInstance.class);

	static final String FIELD_NODE_ID = "node_id";
	static final String FIELD_PREFIX_PROPERTY = "prop.";
	static final String FIELD_PREFIX_EDGE = "edge.";

	static String getPropertyField(String name) {
		return StringUtils.fastConcat(FIELD_PREFIX_PROPERTY, name);
	}

	static String getEdgeField(String name) {
		return StringUtils.fastConcat(FIELD_PREFIX_EDGE, name);
	}

	private final Table table;

	private final GraphDefinition graphDef;

	GraphInstance(String name, Table table, GraphDefinition graphDef) {
		this.table = table;
		this.graphDef = graphDef;
	}

	/**
	 * The required fields are create or deleted.
	 * 
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void checkFields() throws ServerException, IOException {

		Set<String> fieldLeft = new HashSet<String>();
		table.collectExistingFields(fieldLeft);
		AtomicBoolean needCommit = new AtomicBoolean(false);

		List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();

		fieldDefinitions.add(new FieldDefinition(FIELD_NODE_ID,
				FieldDefinition.Type.STORED));

		// Build the property fields
		if (graphDef.node_properties != null) {
			for (Map.Entry<String, PropertyTypeEnum> entry : graphDef.node_properties
					.entrySet()) {
				String fieldName = getPropertyField(entry.getKey());
				switch (entry.getValue()) {
				case indexed:
					fieldDefinitions.add(new FieldDefinition(fieldName,
							FieldDefinition.Type.INDEXED));
					break;
				case stored:
					fieldDefinitions.add(new FieldDefinition(fieldName,
							FieldDefinition.Type.STORED));
					break;
				}
			}
		}

		// Create the edge fields
		if (graphDef.edge_types != null) {
			for (String type : graphDef.edge_types) {
				String fieldName = getEdgeField(type);
				fieldDefinitions.add(new FieldDefinition(fieldName,
						FieldDefinition.Type.INDEXED));
			}
		}

		try {
			table.setFields(fieldDefinitions, fieldLeft, needCommit);
		} catch (Exception e) {
			throw ServerException.getServerException(e);
		}

		if (fieldLeft.size() > 0) {
			needCommit.set(true);
			for (String fieldName : fieldLeft)
				table.removeField(fieldName);
		}

		if (needCommit.get())
			table.commit();

	}

	private static void createUpdateNoCommit(Table table,
			GraphDefinition graphDef, String node_id, GraphNode node)
			throws ServerException, IOException {

		Integer id = table.getPrimaryKeyIndex().getIdOrNew(node_id, null);
		table.setValue(id, FIELD_NODE_ID, node_id);

		// Populate the property fields
		if (node.properties != null && !node.properties.isEmpty()) {
			if (graphDef.node_properties == null)
				throw new ServerException(Status.BAD_REQUEST,
						"This graph database does not define any property.");
			for (Map.Entry<String, String> entry : node.properties.entrySet()) {
				String field = entry.getKey();
				if (!graphDef.node_properties.containsKey(field))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown property name: " + field);
				table.setValue(id, getPropertyField(field), entry.getValue()
						.toString());
			}
		}

		// Populate the edge fields
		if (node.edges != null && !node.edges.isEmpty()) {
			if (graphDef.edge_types == null)
				throw new ServerException(Status.BAD_REQUEST,
						"This graph database does not define any edge.");
			for (Map.Entry<String, Set<String>> entry : node.edges.entrySet()) {
				String type = entry.getKey();
				if (!graphDef.edge_types.contains(type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + type);
				if (entry.getValue() == null)
					System.out.println("M'enfin !");
				else
					table.setValues(id, getEdgeField(type), entry.getValue());
			}
		}
	}

	/**
	 * Create a new node. If the node does not exists, it is created. If the
	 * node exist, it is updated.
	 * 
	 * @param node_id
	 *            the ID of the node
	 * @param node
	 *            the content of the node
	 * @param upsert
	 *            set to true to add the values, false (or null) to replace the
	 *            node
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void createUpdateNode(String node_id, GraphNode node, Boolean upsert)
			throws ServerException, IOException {

		if (node == null)
			return;

		if (upsert != null && upsert) {
			// If the node already exists, we merge it
			try {
				node.add(getNode(node_id));
			} catch (ServerException e) {
				if (e.getStatusCode() != Status.NOT_FOUND.getStatusCode())
					throw e;
			}
		}

		createUpdateNoCommit(table, graphDef, node_id, node);
		table.commit();
	}

	/**
	 * Create a list of node. If a node does not exists, it is created.
	 * Otherwise it is updated.
	 * 
	 * @param nodes
	 *            a map of nodes
	 * @param upsert
	 *            set to true to add the values, false (or null) to replace the
	 *            node
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void createUpdateNodes(Map<String, GraphNode> nodes, Boolean upsert)
			throws IOException, URISyntaxException, ServerException {

		if (nodes == null || nodes.isEmpty())
			return;
		if (logger.isInfoEnabled())
			logger.info("Update " + nodes.size() + " node(s)");

		if (upsert != null && upsert) {
			// If the nodes already exists, we merge them
			Map<String, GraphNode> dbNodes = getNodes(nodes.keySet());
			if (dbNodes != null) {
				for (Map.Entry<String, GraphNode> entry : nodes.entrySet()) {
					GraphNode dbNode = dbNodes.get(entry.getKey());
					if (dbNode != null)
						entry.getValue().add(dbNode);
				}
			}
		}

		for (Map.Entry<String, GraphNode> entry : nodes.entrySet())
			createUpdateNoCommit(table, graphDef, entry.getKey(),
					entry.getValue());
		table.commit();

	}

	private void populateReturnedFields(Collection<String> returnedFields) {
		if (graphDef.node_properties != null)
			for (String name : graphDef.node_properties.keySet())
				returnedFields.add(getPropertyField(name));
		if (graphDef.edge_types != null)
			for (String type : graphDef.edge_types)
				returnedFields.add(getEdgeField(type));
	}

	private void populateGraphNode(Map<String, List<String>> document,
			GraphNode node) {
		if (document == null)
			return;
		for (Map.Entry<String, List<String>> entry : document.entrySet()) {
			String field = entry.getKey();
			List<String> values = entry.getValue();
			if (values == null || values.isEmpty())
				continue;
			else if (field.startsWith(FIELD_PREFIX_PROPERTY)) {
				node.addProperty(
						field.substring(FIELD_PREFIX_PROPERTY.length()),
						values.get(0));
			} else if (field.startsWith(FIELD_PREFIX_EDGE)) {
				for (String value : values)
					node.addEdge(field.substring(FIELD_PREFIX_EDGE.length()),
							value);
			}
		}
	}

	/**
	 * Retrieve a node. If the node does not exists, an IOException is thrown
	 * thrown.
	 * 
	 * @param node_id
	 *            the id of the node
	 * @return a node instance
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	GraphNode getNode(String node_id) throws ServerException, IOException {

		Collection<String> returnedFields = new ArrayList<String>();
		populateReturnedFields(returnedFields);

		Map<String, List<String>> document = table.getDocument(node_id,
				returnedFields);
		if (document == null)
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
		GraphNode node = new GraphNode();
		populateGraphNode(document, node);
		return node;
	}

	/**
	 * Retrieve a list of nodes.
	 * 
	 * @param node_ids
	 *            a collection of node id
	 * @return a map with the nodes
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	Map<String, GraphNode> getNodes(Collection<String> node_ids)
			throws IOException, URISyntaxException, ServerException {

		Collection<String> returnedFields = new ArrayList<String>();
		populateReturnedFields(returnedFields);

		List<Map<String, List<String>>> documents = table.getDocuments(
				node_ids, returnedFields);
		if (documents == null || documents.isEmpty())
			return null;
		Iterator<String> iteratorId = node_ids.iterator();
		Map<String, GraphNode> graphNodes = new LinkedHashMap<String, GraphNode>();
		for (Map<String, List<String>> document : documents) {
			GraphNode node = new GraphNode();
			populateGraphNode(document, node);
			graphNodes.put(iteratorId.next(), node);
		}
		return graphNodes;
	}

	/**
	 * Add a new edge if it does not already exist
	 * 
	 * @param node_id
	 *            the id of initial node
	 * @param type
	 *            the type of the edge
	 * @param to_node_id
	 *            the id of the targeted node
	 * @return the updated node
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	GraphNode createEdge(String node_id, String type, String to_node_id)
			throws IOException, ServerException {

		// Check if the type exists
		if (graphDef.edge_types == null || graphDef.edge_types.isEmpty())
			throw new ServerException(Status.BAD_REQUEST,
					"This base did not define any edge type");
		if (!graphDef.isEdgeType(type))
			throw new ServerException(Status.BAD_REQUEST, "Unknown edge type: "
					+ type);

		// Retrieve the node from the index
		GraphNode node = getNode(node_id);

		// Check if it does not already exists;
		if (!node.addEdge(type, to_node_id))
			return node;

		// We do the update
		createUpdateNode(node_id, node, false);
		return node;
	}

	public GraphNode deleteEdge(String node_id, String type, String to_node_id)
			throws IOException, URISyntaxException, ServerException {

		// Retrieve the node from the index
		GraphNode node = getNode(node_id);

		// Check if the edge exists;
		if (!node.removeEdge(type, to_node_id))
			return node;

		// We do the update
		createUpdateNode(node_id, node, false);
		return node;
	}

	/**
	 * Delete a node.
	 * 
	 * @param node_id
	 *            the ID of the node to delete
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void deleteNode(String node_id) throws ServerException, IOException {
		if (!table.deleteDocument(node_id))
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
	}

	/**
	 * Execute a Graph request
	 * 
	 * @param request
	 *            the Graph request definition
	 * @return a collection with the results
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	public List<GraphNodeResult> request(GraphRequest request)
			throws IOException, URISyntaxException, ServerException {

		List<GraphNodeResult> resultList = new ArrayList<GraphNodeResult>(
				request.getRowsOrDefault());

		Map<String, Map<String, LongCounter>> facetFields = new HashMap<String, Map<String, LongCounter>>();

		OrGroup orGroup = new OrGroup();

		// Prepare the Graph query
		if (request.edges != null && !request.edges.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry : request.edges
					.entrySet()) {
				String edge_type = entry.getKey();
				String field = getEdgeField(edge_type);
				Map<String, LongCounter> termCount = new HashMap<String, LongCounter>();
				facetFields.put(field, termCount);
				Set<String> edge_set = entry.getValue();
				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (!graphDef.isEdgeType(edge_type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + edge_type);

				if (edge_set == null || edge_set.isEmpty())
					continue;
				for (String term : edge_set)
					orGroup.add(new TermQuery(field, term));
			}
		}

		final RoaringBitmap filterBitset;
		if (request.filters != null) {
			// Add user filters if any
			Query query = Query.prepare(request.filters, new QueryHook() {

				@Override
				public Query query(Query query) {
					if (query instanceof TermQuery) {
						TermQuery tq = (TermQuery) query;
						return new TermQuery(getPropertyField(tq.getField()),
								tq.getValue());
					}
					return query;
				}
			});
			filterBitset = table.query(query, null);
		} else
			filterBitset = null;

		// Do the query
		RoaringBitmap docBitset = table.query(orGroup, facetFields);
		if (docBitset == null || docBitset.isEmpty())
			return resultList;

		// Compute the score using facets (multithreaded)
		Map<String, NodeScore> nodeScoreMap = new PatriciaTrie<NodeScore>();
		List<ScoreThread> scoreThreads = new ArrayList<ScoreThread>(
				facetFields.size());
		for (Map.Entry<String, Map<String, LongCounter>> entry : facetFields
				.entrySet()) {
			String field = entry.getKey();
			Map<String, LongCounter> facets = entry.getValue();
			Double weight = request.getEdgeWeight(field
					.substring(FIELD_PREFIX_EDGE.length()));
			ScoreThread scoreThread = new ScoreThread(facets, nodeScoreMap,
					weight, filterBitset);
			scoreThreads.add(scoreThread);
		}
		try {
			ThreadUtils.invokeAndJoin(GraphManager.INSTANCE.executor,
					scoreThreads);
		} catch (Exception e) {
			throw ServerException.getServerException(e);
		}

		// Exclude the unwanted nodes
		if (request.exclude_nodes != null)
			for (String id : request.exclude_nodes)
				nodeScoreMap.remove(id);

		// Sort the result in descending order
		NodeScore[] nodeScoreArray = (NodeScore[]) nodeScoreMap.values()
				.toArray(new NodeScore[nodeScoreMap.size()]);
		Arrays.sort(nodeScoreArray);
		for (int i = request.getStartOrDefault(); i < request
				.getRowsOrDefault() && i < nodeScoreArray.length; i++)
			resultList.add(new GraphNodeResult().set(nodeScoreArray[i]));

		Map<String, GraphNodeResult> nodeResultMap = new PatriciaTrie<GraphNodeResult>();
		for (GraphNodeResult nodeResult : resultList)
			nodeResultMap.put(nodeResult.node_id, nodeResult);

		// Retrieve the nodes from the database
		Map<String, GraphNode> graphNodeMap = getNodes(nodeResultMap.keySet());
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

	public class ScoreThread extends ProcedureExceptionCatcher {

		private final Map<String, LongCounter> facets;
		private final Map<String, NodeScore> nodeScoreMap;
		private final Double weight;
		private final RoaringBitmap filterBitset;
		private final UniqueKey primaryKeys;

		public ScoreThread(Map<String, LongCounter> facets,
				Map<String, NodeScore> nodeScoreMap, Double weight,
				RoaringBitmap filterBitset) {
			this.facets = facets;
			this.nodeScoreMap = nodeScoreMap;
			this.weight = weight;
			this.filterBitset = filterBitset;
			this.primaryKeys = table.getPrimaryKeyIndex();
		}

		@Override
		public void execute() {
			for (Map.Entry<String, LongCounter> facet : facets.entrySet()) {
				NodeScore nodeScore;
				String term = facet.getKey();
				if (filterBitset != null) {
					Integer docId = primaryKeys.getExistingId(term);
					if (docId == null)
						continue;
					synchronized (filterBitset) {
						if (!filterBitset.contains(docId))
							continue;
					}
				}
				long count = facet.getValue().count;
				synchronized (nodeScoreMap) {
					nodeScore = nodeScoreMap.get(term);
					if (nodeScore == null) {
						nodeScore = new NodeScore(term);
						nodeScoreMap.put(term, nodeScore);
					}
				}
				synchronized (nodeScore) {
					nodeScore.score += count * weight;
				}
			}
		}
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
