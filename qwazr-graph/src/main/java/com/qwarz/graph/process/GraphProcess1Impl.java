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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.ws.rs.core.Response.Status;

import com.opensearchserver.client.JsonClient1;
import com.opensearchserver.client.ServerResource;
import com.opensearchserver.client.common.index.TemplateEnum;
import com.opensearchserver.client.common.search.query.DocumentsQuery;
import com.opensearchserver.client.common.search.query.FacetField;
import com.opensearchserver.client.common.search.query.SearchField;
import com.opensearchserver.client.common.search.query.SearchField.SearchFieldMode;
import com.opensearchserver.client.common.search.query.SearchFieldQuery;
import com.opensearchserver.client.common.search.query.SearchQueryAbstract.OperatorEnum;
import com.opensearchserver.client.common.search.query.filter.TermFilter;
import com.opensearchserver.client.common.update.DocumentUpdate;
import com.opensearchserver.client.v1.DocumentApi1;
import com.opensearchserver.client.v1.FieldApi1;
import com.opensearchserver.client.v1.IndexApi1;
import com.opensearchserver.client.v1.SearchApi1;
import com.opensearchserver.client.v1.UpdateApi1;
import com.opensearchserver.client.v1.field.FieldUpdate;
import com.opensearchserver.client.v1.field.ResultFieldList;
import com.opensearchserver.client.v1.field.SchemaField;
import com.opensearchserver.client.v1.field.SchemaField.Indexed;
import com.opensearchserver.client.v1.field.SchemaField.Stored;
import com.opensearchserver.client.v1.field.SchemaField.TermVector;
import com.opensearchserver.client.v1.search.DocumentResult1;
import com.opensearchserver.client.v1.search.DocumentsResult1;
import com.opensearchserver.client.v1.search.FacetFieldItem;
import com.opensearchserver.client.v1.search.FacetResult1;
import com.opensearchserver.client.v1.search.FieldValueList1;
import com.opensearchserver.client.v1.search.SearchResult1;
import com.qwarz.graph.GraphManager;
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphBase.PropertyTypeEnum;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwarz.graph.process.GraphProcess.NodeScore;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class GraphProcess1Impl implements GraphProcessInterface {

	private final JsonClient1 client;

	GraphProcess1Impl(ServerResource server) throws URISyntaxException {
		client = new JsonClient1(server);
	}

	@Override
	public void createDataIndex(GraphBase base) throws URISyntaxException,
			IOException {
		IndexApi1 indexApi = new IndexApi1(client);
		if (!indexApi.indexExists(base.data.name))
			indexApi.createIndex(base.data.name, TemplateEnum.EMPTY_INDEX);
		FieldApi1 fieldApi = new FieldApi1(client);

		// Retrieve the current field list
		ResultFieldList fieldList = fieldApi.getFields(base.data.name);
		TreeSet<String> fieldSet = new TreeSet<String>();
		if (fieldList.fields != null)
			for (SchemaField field : fieldList.fields)
				fieldSet.add(field.name);

		// Build the node field
		List<SchemaField> fields = new ArrayList<SchemaField>();
		SchemaField schemaField = new SchemaField().setIndexed(Indexed.YES)
				.setName(GraphProcess.FIELD_NODE_ID);
		fields.add(schemaField);
		fieldSet.remove(schemaField.name);

		// Build the property fields
		if (base.node_properties != null) {
			for (Map.Entry<String, PropertyTypeEnum> entry : base.node_properties
					.entrySet()) {
				schemaField = new SchemaField().setName(GraphProcess
						.getPropertyField(entry.getKey()));
				switch (entry.getValue()) {
				case indexed:
					schemaField.setIndexed(Indexed.YES).setStored(Stored.NO);
					break;
				case stored:
					schemaField.setIndexed(Indexed.NO).setStored(Stored.YES);
					break;
				}
				fields.add(schemaField);
				fieldSet.remove(schemaField.name);
			}
		}

		// Create the edge fields
		if (base.edge_types != null) {
			for (String type : base.edge_types) {
				schemaField = new SchemaField().setName(GraphProcess
						.getEdgeField(type));
				schemaField.setIndexed(Indexed.YES).setStored(Stored.NO)
						.setTermVector(TermVector.YES);
				fields.add(schemaField);
				fieldSet.remove(schemaField.name);
			}
		}

		// Set the fields and remove the unwanted fields
		fieldApi.setFields(base.data.name, fields);
		for (String field : fieldSet)
			fieldApi.deleteField(base.data.name, field);
		fieldApi.setDefaultUniqueField(base.data.name,
				GraphProcess.FIELD_NODE_ID, GraphProcess.FIELD_NODE_ID);
	}

	@Override
	public void deleteDataIndex(GraphBase base) throws URISyntaxException,
			IOException, ServerException {
		IndexApi1 indexApi = new IndexApi1(client);
		if (!indexApi.indexExists(base.data.name))
			throw new ServerException(Status.NOT_FOUND, "Index not found: "
					+ base.data.name);
		indexApi.deleteIndex(base.data.name);
	}

	private DocumentUpdate getDocumentUpdate(GraphBase base, String node_id,
			GraphNode node) throws ServerException {
		DocumentUpdate document = new DocumentUpdate();

		// Set the node id
		FieldUpdate fieldUpdate = new FieldUpdate().setName(
				GraphProcess.FIELD_NODE_ID).setValue(node_id);
		document.addField(fieldUpdate);

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
				fieldUpdate = new FieldUpdate().setName(
						GraphProcess.getPropertyField(field)).setValue(
						entry.getValue().toString());
				document.addField(fieldUpdate);
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
					for (String value : entry.getValue()) {
						fieldUpdate = new FieldUpdate().setName(
								GraphProcess.getEdgeField(type))
								.setValue(value);
						document.addField(fieldUpdate);
					}
			}
		}
		return document;
	}

	@Override
	public void createUpdateNode(GraphBase base, String node_id, GraphNode node)
			throws IOException, URISyntaxException, ServerException {
		UpdateApi1 updateApi = new UpdateApi1(client);
		List<DocumentUpdate> documents = new ArrayList<DocumentUpdate>(1);
		documents.add(getDocumentUpdate(base, node_id, node));
		updateApi.updateDocuments(base.data.name, documents);
	}

	@Override
	public void createUpdateNodes(GraphBase base, Map<String, GraphNode> nodes)
			throws IOException, URISyntaxException, ServerException {
		UpdateApi1 updateApi = new UpdateApi1(client);
		List<DocumentUpdate> documents = new ArrayList<DocumentUpdate>(
				nodes.size());
		for (Map.Entry<String, GraphNode> entry : nodes.entrySet())
			documents.add(getDocumentUpdate(base, entry.getKey(),
					entry.getValue()));
		updateApi.updateDocuments(base.data.name, documents);
	}

	private Collection<String> populateReturnedFields(GraphBase base,
			Collection<String> returnedFields) {
		returnedFields.add(GraphProcess.FIELD_NODE_ID);
		if (base.node_properties != null)
			for (String name : base.node_properties.keySet())
				returnedFields.add(GraphProcess.getPropertyField(name));
		if (base.edge_types != null)
			for (String type : base.edge_types)
				returnedFields.add(GraphProcess.getEdgeField(type));
		return returnedFields;
	}

	public String populateGraphNode(DocumentResult1 document, GraphNode node) {
		if (document.fields == null)
			return null;
		String node_id = null;
		for (FieldValueList1 fieldValue : document.fields) {
			if (fieldValue.values == null || fieldValue.values.isEmpty())
				continue;
			if (fieldValue.fieldName.equals(GraphProcess.FIELD_NODE_ID))
				node_id = fieldValue.values.get(0);
			else if (fieldValue.fieldName
					.startsWith(GraphProcess.FIELD_PREFIX_PROPERTY)) {
				node.addProperty(
						fieldValue.fieldName
								.substring(GraphProcess.FIELD_PREFIX_PROPERTY
										.length()), fieldValue.values.get(0));
			} else if (fieldValue.fieldName
					.startsWith(GraphProcess.FIELD_PREFIX_EDGE)) {
				for (String value : fieldValue.values)
					node.addEdge(
							fieldValue.fieldName
									.substring(GraphProcess.FIELD_PREFIX_EDGE
											.length()), value);
			}
		}
		return node_id;
	}

	@Override
	public GraphNode getNode(GraphBase base, String node_id)
			throws IOException, URISyntaxException, ServerException {
		SearchApi1 searchApi = new SearchApi1(client);
		SearchField searchField = new SearchField().setField(
				GraphProcess.FIELD_NODE_ID).setMode(SearchFieldMode.TERM);
		SearchFieldQuery searchFieldQuery = (SearchFieldQuery) new SearchFieldQuery()
				.addSearchField(searchField).setQuery(node_id);

		searchFieldQuery.returnedFields = (List<String>) populateReturnedFields(
				base, new ArrayList<String>());

		SearchResult1 searchResult = searchApi.executeSearchField(
				base.data.name, searchFieldQuery);
		if (searchResult == null || searchResult.numFound == null
				|| searchResult.numFound == 0 || searchResult.documents == null)
			throw new ServerException(Status.NOT_FOUND, "Node not found: "
					+ node_id);
		DocumentResult1 document = searchResult.documents.get(0);
		GraphNode node = new GraphNode();
		populateGraphNode(document, node);
		return node;
	}

	@Override
	public void deleteNode(GraphBase base, String node_id) throws IOException,
			URISyntaxException {
		UpdateApi1 updateApi = new UpdateApi1(client);
		updateApi.deleteDocumentsByFieldValue(base.data.name,
				GraphProcess.FIELD_NODE_ID, Arrays.asList(node_id));
	}

	@Override
	public Map<String, GraphNode> getNodes(GraphBase base,
			Collection<String> node_ids) throws IOException, URISyntaxException {
		DocumentApi1 documentApi = new DocumentApi1(client);
		DocumentsQuery documentsQuery = new DocumentsQuery();
		documentsQuery.setField(GraphProcess.FIELD_NODE_ID);
		documentsQuery.returnedFields = (List<String>) populateReturnedFields(
				base, new ArrayList<String>());
		for (String node_id : node_ids)
			documentsQuery.addValue(node_id);
		DocumentsResult1 documentsResult = documentApi.documentsSearch(
				base.data.name, documentsQuery);
		if (documentsResult == null || documentsResult.documents == null)
			return null;
		Map<String, GraphNode> graphNodes = new LinkedHashMap<String, GraphNode>();
		for (DocumentResult1 document : documentsResult.documents) {
			GraphNode node = new GraphNode();
			String node_id = populateGraphNode(document, node);
			if (node_id == null)
				continue;
			graphNodes.put(node_id, node);
		}
		return graphNodes;
	}

	@Override
	public void request(GraphBase base, GraphRequest request,
			List<GraphNodeResult> results) throws ServerException, IOException,
			URISyntaxException {

		// Prepare the query
		SearchApi1 searchApi = new SearchApi1(client);
		SearchFieldQuery searchFieldQuery = (SearchFieldQuery) new SearchFieldQuery()
				.setEmptyReturnsAll(true).setFilterOperator(OperatorEnum.OR);

		// Build the edge filter
		if (request.edges != null && !request.edges.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry : request.edges
					.entrySet()) {
				String edge_type = entry.getKey();
				String field = GraphProcess.getEdgeField(edge_type);
				searchFieldQuery.addFacet(new FacetField().setField(field)
						.setMinCount(1).setMultivalued(true));
				Set<String> edge_set = entry.getValue();
				if (edge_set == null || edge_set.isEmpty())
					continue;
				if (!base.isEdgeType(edge_type))
					throw new ServerException(Status.BAD_REQUEST,
							"Unknown edge type: " + edge_type);
				for (String value : edge_set)
					searchFieldQuery.addFilter(new TermFilter(field, value));
			}
		}

		searchFieldQuery.returnedFields = (List<String>) populateReturnedFields(
				base, new ArrayList<String>());
		searchFieldQuery.setStart(0);
		searchFieldQuery.setRows(0);

		// Execute the search request
		SearchResult1 searchResult = searchApi.executeSearchField(
				base.data.name, searchFieldQuery);
		if (searchResult == null)
			return;
		if (searchResult.numFound == null || searchResult.numFound == 0
				|| searchResult.facets == null)
			return;

		// Compute the score using facets (multithreaded)
		Map<String, NodeScore> nodeScoreMap = new TreeMap<String, NodeScore>();
		List<ScoreThread> scoreThreads = new ArrayList<ScoreThread>(
				searchResult.facets.size());
		for (FacetResult1 facetResult : searchResult.facets) {
			Double weight = request.getEdgeWeight(facetResult.fieldName
					.substring(GraphProcess.FIELD_PREFIX_EDGE.length()));
			ScoreThread scoreThread = new ScoreThread(facetResult,
					nodeScoreMap, weight);
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

		private final FacetResult1 facetResult;
		private final Map<String, NodeScore> nodeScoreMap;
		private final Double weight;

		public ScoreThread(FacetResult1 facetResult,
				Map<String, NodeScore> nodeScoreMap, Double weight) {
			this.facetResult = facetResult;
			this.nodeScoreMap = nodeScoreMap;
			this.weight = weight;
		}

		@Override
		public void execute() {
			for (FacetFieldItem facetItem : facetResult.terms) {
				NodeScore nodeScore;
				synchronized (nodeScoreMap) {
					nodeScore = nodeScoreMap.get(facetItem.term);
					if (nodeScore == null) {
						nodeScore = new NodeScore(facetItem.term);
						nodeScoreMap.put(facetItem.term, nodeScore);
					}
				}
				synchronized (nodeScore) {
					nodeScore.score += facetItem.count * weight;
				}
			}
		}
	}

	@Override
	public void load(GraphBase base) {
		// Nothing to do with this backend
	}

}
