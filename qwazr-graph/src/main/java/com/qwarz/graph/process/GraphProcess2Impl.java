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
package com.qwarz.graph.process;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.qwarz.graph.GraphManager;
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphBase.PropertyTypeEnum;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwarz.graph.process.GraphProcess.NodeScore;
import com.qwazr.search.index.DocumentBuilder;
import com.qwazr.search.index.FieldContent;
import com.qwazr.search.index.FieldDefinition;
import com.qwazr.search.index.IndexManager;
import com.qwazr.search.index.IndexServiceInterface;
import com.qwazr.search.index.IndexStatus;
import com.qwazr.search.index.QueryBuilder;
import com.qwazr.search.index.QueryDefinition.GroupQuery;
import com.qwazr.search.index.QueryDefinition.GroupQuery.OperatorEnum;
import com.qwazr.search.index.ResultDefinition;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class GraphProcess2Impl implements GraphProcessInterface {

	private final IndexServiceInterface indexClient;

	GraphProcess2Impl(int timeOut) throws URISyntaxException {
		indexClient = IndexManager.getClient(timeOut);
	}

	@Override
	public void createDataIndex(GraphBase base) {

		// Retrieve the status of the existing index
		IndexStatus indexStatus = null;
		try {
			indexStatus = indexClient.getIndex(base.data.name);
		} catch (WebApplicationException e) {
			// If it is not a 404, it is a real error
			if (e.getResponse().getStatus() != 404)
				throw e;
		}
		Set<String> existingFields = new HashSet<String>();
		if (indexStatus != null && indexStatus.fields != null)
			existingFields.addAll(indexStatus.fields.keySet());

		// Create the index if it does not exist
		if (indexStatus == null)
			indexStatus = indexClient.createIndex(base.data.name, false, null);

		// The field map to build
		Map<String, FieldDefinition> fieldsToAdd = new HashMap<String, FieldDefinition>();

		// Build the node field
		fieldsToAdd.put(GraphProcess.FIELD_NODE_ID, new FieldDefinition());

		// Build the property fields
		if (base.node_properties != null) {
			for (Map.Entry<String, PropertyTypeEnum> entry : base.node_properties
					.entrySet()) {
				String fieldName = GraphProcess
						.getPropertyField(entry.getKey());
				FieldDefinition fieldDef = null;
				switch (entry.getValue()) {
				case indexed:
					fieldDef = new FieldDefinition();
					break;
				case stored:
					fieldDef = new FieldDefinition();
					break;
				}

				if (fieldDef != null)
					fieldsToAdd.put(fieldName, fieldDef);
			}
		}

		// Create the edge fields
		if (base.edge_types != null) {
			for (String type : base.edge_types) {
				String fieldName = GraphProcess.getEdgeField(type);
				fieldsToAdd.put(fieldName, new FieldDefinition());
			}
		}

		// Set the fields minus the already existing fields
		for (String existingField : existingFields)
			fieldsToAdd.remove(existingField);
		if (fieldsToAdd.size() > 0)
			indexClient.createFields(base.data.name, fieldsToAdd);
	}

	@Override
	public void deleteDataIndex(GraphBase base) throws ServerException {
		try {
			indexClient.deleteIndex(base.data.name, false);
		} catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == 404)
				throw new ServerException(Status.NOT_FOUND, "Index not found: "
						+ base.data.name);
			throw e;
		}
	}

	private Map<String, FieldContent> getDocumentUpdate(GraphBase base,
			String node_id, GraphNode node) throws ServerException {

		DocumentBuilder documentBuilder = new DocumentBuilder();

		// Set the node id
		documentBuilder.addTerm(GraphProcess.FIELD_NODE_ID, node_id);

		// Populate the property fields
		if (node.properties != null && !node.properties.isEmpty()) {
			if (base.node_properties == null)
				throw new ServerException(Status.BAD_REQUEST,
						"This graph database does not define any property.");
			for (Map.Entry<String, String> entry : node.properties.entrySet()) {
				String field = entry.getKey();
				if (!base.node_properties.containsKey(field))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown property name: " + field);
				documentBuilder.addTerm(GraphProcess.getPropertyField(field),
						entry.getValue().toString());
			}
		}

		// Populate the edge fields
		if (node.edges != null && !node.edges.isEmpty()) {
			if (base.edge_types == null)
				throw new ServerException(Status.BAD_REQUEST,
						"This graph database does not define any edge.");
			for (Map.Entry<String, Set<String>> entry : node.edges.entrySet()) {
				String type = entry.getKey();
				if (!base.edge_types.contains(type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + type);
				if (entry.getValue() == null)
					System.out.println("M'enfin !");
				else
					for (String value : entry.getValue())
						documentBuilder.addTerm(
								GraphProcess.getEdgeField(type), value);
			}
		}
		return documentBuilder.build();
	}

	@Override
	public void createUpdateNode(GraphBase base, String node_id, GraphNode node)
			throws ServerException {
		// First we delete the previous version
		QueryBuilder queryBuilder = new QueryBuilder();
		queryBuilder.setQuery(queryBuilder.createTermQuery(
				GraphProcess.FIELD_NODE_ID, node_id));
		indexClient.findDocuments(base.data.name, queryBuilder.build(), true);

		// Then we insert the new version
		indexClient.postDocuments(base.data.name,
				Arrays.asList(getDocumentUpdate(base, node_id, node)));
	}

	@Override
	public void createUpdateNodes(GraphBase base, Map<String, GraphNode> nodes)
			throws IOException, URISyntaxException, ServerException {

		// The query to delete previous version
		QueryBuilder queryBuilder = new QueryBuilder();
		GroupQuery groupQuery = queryBuilder.createGroupQuery(OperatorEnum.or);
		queryBuilder.setQuery(groupQuery);

		// The array for the new version of the documents
		List<Map<String, FieldContent>> documents = new ArrayList<Map<String, FieldContent>>(
				nodes.size());
		for (Map.Entry<String, GraphNode> entry : nodes.entrySet()) {
			String node_id = entry.getKey();
			documents.add(getDocumentUpdate(base, node_id, entry.getValue()));
			groupQuery.addQuery(queryBuilder.createTermQuery(
					GraphProcess.FIELD_NODE_ID, node_id));
		}

		// Let do the transaction (delete and create = update)
		// indexClient.findDocuments(base.data.name, queryBuilder.build(),
		// true);
		indexClient.postDocuments(base.data.name, documents);
	}

	private void populateReturnedFields(GraphBase base,
			QueryBuilder queryBuilder) {
		queryBuilder.addReturnedField(GraphProcess.FIELD_NODE_ID);
		if (base.node_properties != null)
			for (String name : base.node_properties.keySet())
				queryBuilder.addReturnedField(GraphProcess
						.getPropertyField(name));
		if (base.edge_types != null)
			for (String type : base.edge_types)
				queryBuilder.addReturnedField(GraphProcess.getEdgeField(type));
	}

	public String populateGraphNode(Map<String, List<String>> document,
			GraphNode node) {
		if (document == null)
			return null;
		String node_id = null;
		for (Map.Entry<String, List<String>> entry : document.entrySet()) {
			String field = entry.getKey();
			List<String> values = entry.getValue();
			if (values == null || values.isEmpty())
				continue;
			if (field.equals(GraphProcess.FIELD_NODE_ID))
				node_id = values.get(0);
			else if (field.startsWith(GraphProcess.FIELD_PREFIX_PROPERTY)) {
				node.addProperty(
						field.substring(GraphProcess.FIELD_PREFIX_PROPERTY
								.length()), values.get(0));
			} else if (field.startsWith(GraphProcess.FIELD_PREFIX_EDGE)) {
				for (String value : values)
					node.addEdge(field.substring(GraphProcess.FIELD_PREFIX_EDGE
							.length()), value);
			}
		}
		return node_id;
	}

	@Override
	public GraphNode getNode(GraphBase base, String node_id)
			throws ServerException {
		QueryBuilder queryBuilder = new QueryBuilder();
		queryBuilder.setQuery(queryBuilder.createTermQuery(
				GraphProcess.FIELD_NODE_ID, node_id));
		populateReturnedFields(base, queryBuilder);

		ResultDefinition searchResult = indexClient.findDocuments(
				base.data.name, queryBuilder.build(), false);
		if (searchResult == null || searchResult.number_of_documents == 0
				|| searchResult.documents == null)
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
		Map<String, List<String>> document = searchResult.documents.get(0);
		GraphNode node = new GraphNode();
		populateGraphNode(document, node);
		return node;
	}

	@Override
	public Map<String, GraphNode> getNodes(GraphBase base,
			Collection<String> node_ids) throws IOException,
			URISyntaxException, ServerException {
		QueryBuilder queryBuilder = new QueryBuilder();
		GroupQuery groupQuery = queryBuilder.createGroupQuery(OperatorEnum.or);
		queryBuilder.setQuery(groupQuery);
		populateReturnedFields(base, queryBuilder);
		for (String node_id : node_ids)
			groupQuery.addQuery(queryBuilder.createTermQuery(
					GraphProcess.FIELD_NODE_ID, node_id));
		ResultDefinition searchResult = indexClient.findDocuments(
				base.data.name, queryBuilder.build(), false);
		if (searchResult == null || searchResult.documents == null)
			return null;
		Map<String, GraphNode> graphNodes = new LinkedHashMap<String, GraphNode>();
		for (Map<String, List<String>> document : searchResult.documents) {
			GraphNode node = new GraphNode();
			String node_id = populateGraphNode(document, node);
			if (node_id == null)
				continue;
			graphNodes.put(node_id, node);
		}
		return graphNodes;
	}

	@Override
	public void deleteNode(GraphBase base, String node_id)
			throws ServerException {
		QueryBuilder queryBuilder = new QueryBuilder();
		queryBuilder.setQuery(queryBuilder.createTermQuery(
				GraphProcess.FIELD_NODE_ID, node_id));
		indexClient.findDocuments(base.data.name, queryBuilder.build(), true);
	}

	@Override
	public void request(GraphBase base, GraphRequest request,
			List<GraphNodeResult> results) throws IOException,
			URISyntaxException, ServerException {

		// Prepare the query
		QueryBuilder queryBuilder = new QueryBuilder();
		GroupQuery groupQuery = queryBuilder.createGroupQuery(OperatorEnum.or);
		queryBuilder.setQuery(groupQuery);

		// Build the edge filter
		if (request.edges != null && !request.edges.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry : request.edges
					.entrySet()) {
				String edge_type = entry.getKey();
				String field = GraphProcess.getEdgeField(edge_type);
				queryBuilder.addFacetField(field);
				Set<String> edge_set = entry.getValue();
				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (!base.isEdgeType(edge_type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + edge_type);
				for (String value : edge_set)
					groupQuery.addQuery(queryBuilder.createTermQuery(field,
							value));
			}
		}

		// Execute the search request
		ResultDefinition searchResult = indexClient.findDocuments(
				base.data.name, queryBuilder.build(), false);
		if (searchResult == null)
			return;
		if (searchResult.number_of_documents == 0
				|| searchResult.facets == null)
			return;

		// Compute the score using facets (multithreaded)
		Map<String, NodeScore> nodeScoreMap = new TreeMap<String, NodeScore>();
		List<ScoreThread> scoreThreads = new ArrayList<ScoreThread>(
				searchResult.facets.size());
		for (Map.Entry<String, Map<String, Long>> entry : searchResult.facets
				.entrySet()) {
			String field = entry.getKey();
			Map<String, Long> facets = entry.getValue();
			Double weight = request.getEdgeWeight(field
					.substring(GraphProcess.FIELD_PREFIX_EDGE.length()));
			ScoreThread scoreThread = new ScoreThread(facets, nodeScoreMap,
					weight);
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
			results.add(new GraphNodeResult().set(nodeScoreArray[i]));
	}

	public class ScoreThread extends ProcedureExceptionCatcher {

		private final Map<String, Long> facets;
		private final Map<String, NodeScore> nodeScoreMap;
		private final Double weight;

		public ScoreThread(Map<String, Long> facets,
				Map<String, NodeScore> nodeScoreMap, Double weight) {
			this.facets = facets;
			this.nodeScoreMap = nodeScoreMap;
			this.weight = weight;
		}

		@Override
		public void execute() {
			for (Map.Entry<String, Long> facet : facets.entrySet()) {
				NodeScore nodeScore;
				String term = facet.getKey();
				long count = facet.getValue();
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

	@Override
	public void load(GraphBase base) {
		// Nothing to do with this backend
	}

}
