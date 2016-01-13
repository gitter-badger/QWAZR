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

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.graph.model.*;
import com.qwazr.utils.json.client.JsonClientAbstract;
import org.apache.http.client.fluent.Request;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GraphSingleClient extends JsonClientAbstract implements GraphServiceInterface {

	private final static String GRAPH_PREFIX = "/graph/";

	GraphSingleClient(String url, int msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	public Set<String> list() {
		UBuilder uBuilder = new UBuilder(GRAPH_PREFIX);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, null, SetStringTypeRef, 200);
	}

	@Override
	public GraphResult createUpdateGraph(String graphName, GraphDefinition graphDef) {
		UBuilder uBuilder = new UBuilder(GRAPH_PREFIX, graphName);
		Request request = Request.Post(uBuilder.build());
		return commonServiceRequest(request, graphDef, null, GraphResult.class, 200);
	}

	@Override
	public GraphResult getGraph(String graphName) {
		UBuilder uBuilder = new UBuilder(GRAPH_PREFIX, graphName);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, null, GraphResult.class, 200);
	}

	@Override
	public GraphResult deleteGraph(String graphName) {
		UBuilder uBuilder = new UBuilder(GRAPH_PREFIX, graphName);
		Request request = Request.Delete(uBuilder.build());
		return commonServiceRequest(request, null, null, GraphResult.class, 200);
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

}
