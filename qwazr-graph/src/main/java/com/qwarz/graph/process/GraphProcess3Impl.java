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

import java.io.File;
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
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.Response.Status;

import com.qwarz.graph.GraphManager;
import com.qwarz.graph.database.CollectorInterface.LongCounter;
import com.qwarz.graph.database.Database;
import com.qwarz.graph.database.FieldInterface.FieldDefinition;
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphBase.PropertyTypeEnum;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwarz.graph.process.GraphProcess.NodeScore;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class GraphProcess3Impl implements GraphProcessInterface {

	private final String graphName;

	GraphProcess3Impl(String graphName) {
		this.graphName = graphName;
	}

	private Database getGraphDatabase() throws IOException {
		File dbDirectory = new File(GraphManager.INSTANCE.directory, graphName);
		if (!dbDirectory.exists())
			dbDirectory.mkdir();
		return Database.getInstance(graphName, dbDirectory);
	}

	private void checkFields(Database graphDb, GraphBase base,
			Set<String> existingFields, AtomicBoolean needCommit)
			throws ServerException {

		List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();

		// Build the property fields
		if (base.node_properties != null) {
			for (Map.Entry<String, PropertyTypeEnum> entry : base.node_properties
					.entrySet()) {
				String fieldName = GraphProcess
						.getPropertyField(entry.getKey());
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
		if (base.edge_types != null) {
			for (String type : base.edge_types) {
				String fieldName = GraphProcess.getEdgeField(type);
				fieldDefinitions.add(new FieldDefinition(fieldName,
						FieldDefinition.Type.INDEXED));
			}
		}

		try {
			graphDb.setFields(fieldDefinitions, existingFields, needCommit);
		} catch (Exception e) {
			throw ServerException.getServerException(e);
		}
	}

	@Override
	public void load(GraphBase base) throws IOException, ServerException {
		Database graphDb = getGraphDatabase();
		AtomicBoolean needCommit = new AtomicBoolean(false);
		checkFields(graphDb, base, null, needCommit);
		if (needCommit.get())
			graphDb.commit();
	}

	@Override
	public void createDataIndex(GraphBase base) throws IOException,
			ServerException {

		Database graphDb = getGraphDatabase();

		Set<String> fieldLeft = new HashSet<String>();
		AtomicBoolean needCommit = new AtomicBoolean(false);
		checkFields(getGraphDatabase(), base, fieldLeft, needCommit);

		if (fieldLeft.size() > 0) {
			needCommit.set(true);
			for (String fieldName : fieldLeft)
				graphDb.removeField(fieldName);
		}
		if (needCommit.get())
			graphDb.commit();
	}

	@Override
	public void deleteDataIndex(GraphBase base) throws ServerException,
			IOException {
		Database.deleteBase(graphName);
	}

	private void createUpdateNoCommit(Database graphDb, GraphBase base,
			String node_id, GraphNode node) throws ServerException, IOException {

		Integer id = graphDb.getNewPrimaryId(node_id);

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
				graphDb.setValue(id, GraphProcess.getPropertyField(field),
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
					graphDb.setValues(id, GraphProcess.getEdgeField(type),
							entry.getValue());
			}
		}
	}

	@Override
	public void createUpdateNode(GraphBase base, String node_id, GraphNode node)
			throws ServerException, IOException {

		Database graphDb = getGraphDatabase();
		createUpdateNoCommit(graphDb, base, node_id, node);
		graphDb.commit();

	}

	@Override
	public void createUpdateNodes(GraphBase base, Map<String, GraphNode> nodes)
			throws IOException, URISyntaxException, ServerException {

		Database graphDb = getGraphDatabase();
		for (Map.Entry<String, GraphNode> entry : nodes.entrySet())
			createUpdateNoCommit(graphDb, base, entry.getKey(),
					entry.getValue());
		graphDb.commit();

	}

	private void populateReturnedFields(GraphBase base,
			Collection<String> returnedFields) {
		if (base.node_properties != null)
			for (String name : base.node_properties.keySet())
				returnedFields.add(GraphProcess.getPropertyField(name));
		if (base.edge_types != null)
			for (String type : base.edge_types)
				returnedFields.add(GraphProcess.getEdgeField(type));
	}

	public void populateGraphNode(Map<String, List<String>> document,
			GraphNode node) {
		if (document == null)
			return;
		for (Map.Entry<String, List<String>> entry : document.entrySet()) {
			String field = entry.getKey();
			List<String> values = entry.getValue();
			if (values == null || values.isEmpty())
				continue;
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
	}

	@Override
	public GraphNode getNode(GraphBase base, String node_id)
			throws ServerException, IOException {

		Database graphDb = getGraphDatabase();

		Collection<String> returnedFields = new ArrayList<String>();
		populateReturnedFields(base, returnedFields);

		Map<String, List<String>> document = graphDb.getDocument(node_id,
				returnedFields);
		if (document == null)
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
		GraphNode node = new GraphNode();
		populateGraphNode(document, node);
		return node;
	}

	@Override
	public Map<String, GraphNode> getNodes(GraphBase base,
			Collection<String> node_ids) throws IOException,
			URISyntaxException, ServerException {
		Database graphDb = getGraphDatabase();

		Collection<String> returnedFields = new ArrayList<String>();
		populateReturnedFields(base, returnedFields);

		List<Map<String, List<String>>> documents = graphDb.getDocuments(
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

	@Override
	public void deleteNode(GraphBase base, String node_id)
			throws ServerException, IOException {
		Database graphDb = getGraphDatabase();
		if (!graphDb.deleteDocument(node_id))
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
	}

	@Override
	public void request(GraphBase base, GraphRequest request,
			List<GraphNodeResult> results) throws IOException,
			URISyntaxException, ServerException {

		Map<String, Set<String>> orTermQuery = new HashMap<String, Set<String>>();
		Map<String, Map<String, LongCounter>> facetFields = new HashMap<String, Map<String, LongCounter>>();

		// Prepare the query
		if (request.edges != null && !request.edges.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry : request.edges
					.entrySet()) {
				String edge_type = entry.getKey();
				String field = GraphProcess.getEdgeField(edge_type);
				Map<String, LongCounter> termCount = new HashMap<String, LongCounter>();
				facetFields.put(field, termCount);
				Set<String> edge_set = entry.getValue();
				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (!base.isEdgeType(edge_type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + edge_type);

				if (edge_set == null || edge_set.isEmpty())
					continue;
				Set<String> termSet = orTermQuery.get(field);
				if (termSet == null) {
					termSet = new HashSet<String>();
					orTermQuery.put(field, termSet);
				}
				termSet.addAll(edge_set);
			}
		}

		Database graphDb = getGraphDatabase();
		int found = graphDb.findDocumentsOr(orTermQuery, null, facetFields);
		if (found == 0)
			return;

		// Compute the score using facets (multithreaded)
		Map<String, NodeScore> nodeScoreMap = new TreeMap<String, NodeScore>();
		List<ScoreThread> scoreThreads = new ArrayList<ScoreThread>(
				facetFields.size());
		for (Map.Entry<String, Map<String, LongCounter>> entry : facetFields
				.entrySet()) {
			String field = entry.getKey();
			Map<String, LongCounter> facets = entry.getValue();
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

		private final Map<String, LongCounter> facets;
		private final Map<String, NodeScore> nodeScoreMap;
		private final Double weight;

		public ScoreThread(Map<String, LongCounter> facets,
				Map<String, NodeScore> nodeScoreMap, Double weight) {
			this.facets = facets;
			this.nodeScoreMap = nodeScoreMap;
			this.weight = weight;
		}

		@Override
		public void execute() {
			for (Map.Entry<String, LongCounter> facet : facets.entrySet()) {
				NodeScore nodeScore;
				String term = facet.getKey();
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

}
