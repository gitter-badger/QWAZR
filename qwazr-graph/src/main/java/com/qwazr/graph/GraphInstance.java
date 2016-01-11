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
 **/
package com.qwazr.graph;

import com.qwazr.database.model.ColumnDefinition;
import com.qwazr.database.store.CollectorInterface.LongCounter;
import com.qwazr.database.store.*;
import com.qwazr.database.store.Query.OrGroup;
import com.qwazr.database.store.Query.QueryHook;
import com.qwazr.database.store.Query.TermQuery;
import com.qwazr.graph.model.GraphDefinition;
import com.qwazr.graph.model.GraphDefinition.PropertyTypeEnum;
import com.qwazr.graph.model.GraphNode;
import com.qwazr.graph.model.GraphNodeResult;
import com.qwazr.graph.model.GraphRequest;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class GraphInstance {

	private static final Logger logger = LoggerFactory.getLogger(GraphInstance.class);

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
	 * @throws IOException     if any I/O error occurs
	 * @throws ServerException if any server exception occurs
	 */
	void checkFields() throws ServerException, IOException {

		Map<String, ColumnDefinition> existingColumns = table.getColumns();
		Map<String, ColumnDefinition> columnDefinitions = new LinkedHashMap<String, ColumnDefinition>();

		// Build the property fields
		if (graphDef.node_properties != null) {
			for (Map.Entry<String, PropertyTypeEnum> entry : graphDef.node_properties.entrySet()) {
				String fieldName = getPropertyField(entry.getKey());
				switch (entry.getValue()) {
				case indexed:
					columnDefinitions.put(fieldName,
							new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.INDEXED));
					break;
				case stored:
					columnDefinitions.put(fieldName,
							new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.STORED));
					break;
				case boost:
					columnDefinitions.put(fieldName,
							new ColumnDefinition(ColumnDefinition.Type.DOUBLE, ColumnDefinition.Mode.INDEXED));
					break;
				}
			}
		}

		// Create the edge fields
		if (graphDef.edge_types != null) {
			for (String type : graphDef.edge_types) {
				String fieldName = getEdgeField(type);
				columnDefinitions.put(fieldName,
						new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.INDEXED));
			}
		}

		try {
			for (Map.Entry<String, ColumnDefinition> entry : columnDefinitions.entrySet())
				if (!existingColumns.containsKey(entry.getKey()))
					table.addColumn(entry.getKey(), entry.getValue());
			table.commit();
		} catch (Exception e) {
			throw ServerException.getServerException(e);
		}

	}

	private static void createUpdate(Table table, GraphDefinition graphDef, String node_id, GraphNode node)
			throws ServerException, IOException, DatabaseException {

		Map<String, Object> row = new HashMap<String, Object>();

		// Populate the property fields
		if (node.properties != null && !node.properties.isEmpty()) {
			if (graphDef.node_properties == null)
				throw new ServerException(Status.BAD_REQUEST, "This graph database does not define any property.");
			for (Map.Entry<String, Object> entry : node.properties.entrySet()) {
				String field = entry.getKey();
				if (!graphDef.node_properties.containsKey(field))
					throw new ServerException(Status.BAD_REQUEST, "Unknown property name: " + field);
				row.put(getPropertyField(field), entry.getValue());
			}
		}

		// Populate the edge fields
		if (node.edges != null && !node.edges.isEmpty()) {
			if (graphDef.edge_types == null)
				throw new ServerException(Status.BAD_REQUEST, "This graph database does not define any edge.");
			for (Map.Entry<String, Set<Object>> entry : node.edges.entrySet()) {
				String type = entry.getKey();
				if (!graphDef.edge_types.contains(type))
					throw new ServerException(Status.BAD_REQUEST, "Unknown edge type: " + type);
				if (entry.getValue() == null)
					continue;
				row.put(getEdgeField(type), entry.getValue());
			}
		}

		table.upsertRow(node_id, row);
	}

	/**
	 * Create a new node. If the node does not exists, it is created. If the
	 * node exist, it is updated.
	 *
	 * @param node_id the ID of the node
	 * @param node    the content of the node
	 * @param upsert  set to true to add the values, false (or null) to replace the
	 *                node
	 * @throws URISyntaxException if the server parameters are wrong
	 * @throws IOException        if any I/O error occurs
	 * @throws ServerException    if any server exception occurs
	 */
	void createUpdateNode(String node_id, GraphNode node, Boolean upsert)
			throws ServerException, IOException, DatabaseException {

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

		createUpdate(table, graphDef, node_id, node);
		table.commit();
	}

	/**
	 * Create a list of node. If a node does not exists, it is created.
	 * Otherwise it is updated.
	 *
	 * @param nodes  a map of nodes
	 * @param upsert set to true to add the values, false (or null) to replace the
	 *               node
	 * @throws URISyntaxException if the server parameters are wrong
	 * @throws IOException        if any I/O error occurs
	 * @throws ServerException    if any server exception occurs
	 */
	void createUpdateNodes(Map<String, GraphNode> nodes, Boolean upsert)
			throws IOException, URISyntaxException, ServerException, DatabaseException {

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
			createUpdate(table, graphDef, entry.getKey(), entry.getValue());
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

	private void populateGraphNode(Map<String, Object> document, GraphNode node) {
		if (document == null)
			return;
		for (Map.Entry<String, Object> entry : document.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (value == null)
				continue;

			if (value instanceof List<?>) {
				List<?> values = (List<?>) value;
				if (values.isEmpty())
					continue;
				if (field.startsWith(FIELD_PREFIX_PROPERTY)) {
					node.addProperty(field.substring(FIELD_PREFIX_PROPERTY.length()), values.get(0));
				} else if (field.startsWith(FIELD_PREFIX_EDGE)) {
					for (Object val : values)
						node.addEdge(field.substring(FIELD_PREFIX_EDGE.length()), val);
				}
			}

			if (field.startsWith(FIELD_PREFIX_PROPERTY))
				node.addProperty(field.substring(FIELD_PREFIX_PROPERTY.length()), value);
			else if (field.startsWith(FIELD_PREFIX_EDGE)) {
				node.addEdge(field.substring(FIELD_PREFIX_EDGE.length()), value);

			}
		}
	}

	/**
	 * Retrieve a node. If the node does not exists, an IOException is thrown
	 * thrown.
	 *
	 * @param node_id the id of the node
	 * @return a node instance
	 * @throws URISyntaxException if the server parameters are wrong
	 * @throws IOException        if any I/O error occurs
	 * @throws ServerException    if any server exception occurs
	 */
	GraphNode getNode(String node_id) throws ServerException, IOException, DatabaseException {

		Set<String> returnedFields = new LinkedHashSet<String>();
		populateReturnedFields(returnedFields);

		Map<String, Object> document = table.getRow(node_id, returnedFields);
		if (document == null)
			throw new ServerException(Status.NOT_FOUND, "Node not found: " + node_id);
		GraphNode node = new GraphNode();
		populateGraphNode(document, node);
		return node;
	}

	/**
	 * Retrieve a list of nodes.
	 *
	 * @param node_ids a collection of node id
	 * @return a map with the nodes
	 * @throws URISyntaxException if the server parameters are wrong
	 * @throws IOException        if any I/O error occurs
	 * @throws ServerException    if any server exception occurs
	 */
	Map<String, GraphNode> getNodes(Set<String> node_ids)
			throws IOException, URISyntaxException, ServerException, DatabaseException {

		Set<String> returnedFields = new LinkedHashSet<String>();
		populateReturnedFields(returnedFields);

		List<LinkedHashMap<String, Object>> documents = new ArrayList<LinkedHashMap<String, Object>>();
		table.getRows(node_ids, returnedFields, documents);
		if (documents == null || documents.isEmpty())
			return null;
		Iterator<String> iteratorId = node_ids.iterator();
		Map<String, GraphNode> graphNodes = new LinkedHashMap<String, GraphNode>();
		for (Map<String, Object> document : documents) {
			GraphNode node = new GraphNode();
			populateGraphNode(document, node);
			graphNodes.put(iteratorId.next(), node);
		}
		return graphNodes;
	}

	/**
	 * Add a new edge if it does not already exist
	 *
	 * @param node_id    the id of initial node
	 * @param type       the type of the edge
	 * @param to_node_id the id of the targeted node
	 * @return the updated node
	 * @throws IOException     if any I/O error occurs
	 * @throws ServerException if any server exception occurs
	 */
	GraphNode createEdge(String node_id, String type, String to_node_id)
			throws IOException, ServerException, DatabaseException {

		// Check if the type exists
		if (graphDef.edge_types == null || graphDef.edge_types.isEmpty())
			throw new ServerException(Status.BAD_REQUEST, "This base did not define any edge type");
		if (!graphDef.isEdgeType(type))
			throw new ServerException(Status.BAD_REQUEST, "Unknown edge type: " + type);

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
			throws IOException, URISyntaxException, ServerException, DatabaseException {

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
	 * @param node_id the ID of the node to delete
	 * @throws DatabaseException if the server parameters are wrong
	 * @throws IOException       if any I/O error occurs
	 * @throws ServerException   if any server exception occurs
	 */
	void deleteNode(String node_id) throws ServerException, IOException, DatabaseException {
		if (!table.deleteRow(node_id))
			throw new ServerException(Status.NOT_FOUND, "Node not found: " + node_id);
		table.commit();
	}

	/**
	 * Execute a Graph request
	 *
	 * @param request the Graph request definition
	 * @return a collection with the results
	 * @throws URISyntaxException if the server parameters are wrong
	 * @throws IOException        if any I/O error occurs
	 * @throws ServerException    if any server exception occurs
	 */
	public List<GraphNodeResult> request(GraphRequest request)
			throws IOException, URISyntaxException, ServerException, DatabaseException {

		List<GraphNodeResult> resultList = new ArrayList<GraphNodeResult>(request.getRowsOrDefault());

		Map<String, Map<String, LongCounter>> facetFields = new HashMap<String, Map<String, LongCounter>>();

		OrGroup orGroup = null;

		// Prepare the Graph query
		if (request.edges != null && !request.edges.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry : request.edges.entrySet()) {
				String edge_type = entry.getKey();
				String field = getEdgeField(edge_type);
				Map<String, LongCounter> termCount = new HashMap<String, LongCounter>();
				facetFields.put(field, termCount);
				Set<String> edge_set = entry.getValue();
				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (!graphDef.isEdgeType(edge_type))
					throw new ServerException(Status.BAD_REQUEST, "Unknown edge type: " + edge_type);

				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (orGroup == null)
					orGroup = new OrGroup();
				for (String term : edge_set)
					orGroup.add(new TermQuery<String>(field, term));
			}
		}

		// Do the query
		QueryResult result = table.query(orGroup, facetFields);
		if (result == null || result.finalBitmap == null || result.finalBitmap.isEmpty())
			return resultList;

		// Add user filters if any
		final RoaringBitmap filterBitset;
		if (request.filters != null) {
			Query query = Query.prepare(request.filters, new QueryHook() {

				@Override
				public void query(Query query) {
					if (query instanceof TermQuery) {
						TermQuery<?> tq = (TermQuery<?>) query;
						tq.setField(getPropertyField(tq.getField()));
					}
				}
			});
			filterBitset = table.query(query, null).finalBitmap;
		} else
			filterBitset = null;

		// Get the boost fields
		Set<String> boostFields = null;
		if (request.node_property_boost != null && !request.node_property_boost.isEmpty()) {
			boostFields = new HashSet<String>();
			int i = 0;
			for (String boostField : request.node_property_boost)
				boostFields.add(getPropertyField(boostField));
		}

		// Compute the score using facets (multithreaded)
		Map<String, NodeScore> nodeScoreMap = new PatriciaTrie<NodeScore>();
		List<ScoreThread> scoreThreads = new ArrayList<ScoreThread>(facetFields.size());
		for (Map.Entry<String, Map<String, LongCounter>> entry : facetFields.entrySet()) {
			String field = entry.getKey();
			Map<String, LongCounter> facets = entry.getValue();
			Double weight = request.getEdgeWeight(field.substring(FIELD_PREFIX_EDGE.length()));
			ScoreThread scoreThread = new ScoreThread(facets, nodeScoreMap, weight, filterBitset, boostFields,
					result.context);
			scoreThreads.add(scoreThread);
		}
		try {
			ThreadUtils.invokeAndJoin(GraphManager.INSTANCE.executorService, scoreThreads);
		} catch (Exception e) {
			throw ServerException.getServerException(e);
		}

		// Exclude the unwanted nodes
		if (request.exclude_nodes != null)
			for (String id : request.exclude_nodes)
				nodeScoreMap.remove(id);

		// Sort the result in descending order
		NodeScore[] nodeScoreArray = nodeScoreMap.values().toArray(new NodeScore[nodeScoreMap.size()]);
		Arrays.sort(nodeScoreArray);
		for (int i = request.getStartOrDefault(); i < request.getRowsOrDefault() && i < nodeScoreArray.length; i++)
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
		private final Set<String> boostFields;
		private final QueryContext queryContext;

		public ScoreThread(Map<String, LongCounter> facets, Map<String, NodeScore> nodeScoreMap, Double weight,
				RoaringBitmap filterBitset, Set<String> boostFields, QueryContext queryContext) {
			this.facets = facets;
			this.nodeScoreMap = nodeScoreMap;
			this.weight = weight;
			this.filterBitset = filterBitset;
			this.boostFields = boostFields;
			this.queryContext = queryContext;
		}

		private class ScoreBooster implements ValueConsumer {

			private double score;

			private ScoreBooster(double score) {
				this.score = score;
			}

			@Override
			public void consume(double value) {
				score = score * value;
			}

			@Override
			public void consume(long value) {
				score = score * value;

			}

			@Override
			public void consume(float value) {
				score = score * value;

			}

			@Override
			public void consume(String value) {
				throw new RuntimeException("Score cannot be boosted by a string value");
			}
		}

		@Override
		public void execute() throws IOException, DatabaseException {
			for (Map.Entry<String, LongCounter> facet : facets.entrySet()) {
				NodeScore nodeScore;
				Integer docId = null;
				String term = facet.getKey();
				if (filterBitset != null) {
					docId = queryContext.getExistingDocId(term);
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
				ScoreBooster scoreInc = new ScoreBooster(count * weight);
				if (boostFields != null) {
					if (docId == null)
						docId = queryContext.getExistingDocId(term);
					if (docId != null)
						for (String boostField : boostFields)
							queryContext.consumeFirstValue(boostField, docId, scoreInc);
				}
				synchronized (nodeScore) {
					nodeScore.score += scoreInc.score;
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
		 */ public int compareTo(final NodeScore o) {
			return Double.compare(o.score, score);
		}

	}

	public int getSize() throws IOException {
		return table.getSize();
	}
}
