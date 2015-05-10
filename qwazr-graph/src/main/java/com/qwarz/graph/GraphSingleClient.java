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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.json.client.JsonClientAbstract;

public class GraphSingleClient extends JsonClientAbstract implements
		GraphServiceInterface {

	GraphSingleClient(String url, int msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
	}

	private URIBuilder getGraphBaseUrl(String db_name, Boolean local,
			Integer msTimeout) throws URISyntaxException {
		URIBuilder uriBuilder = getBaseUrl("/graph/", db_name);
		if (local != null)
			uriBuilder.setParameter("local", local.toString());
		if (msTimeout != null)
			uriBuilder.setParameter("timeout", msTimeout.toString());
		return uriBuilder;
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	public Set<String> list(Integer msTimeOut, Boolean local) {
		try {
			URIBuilder uriBuilder = getGraphBaseUrl(null, local, null);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, SetStringTypeRef, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GraphBase createUpdateBase(String db_name, GraphBase base,
			Integer msTimeOut, Boolean local) {
		try {
			URIBuilder uriBuilder = getGraphBaseUrl(db_name, local, msTimeOut);
			Request request = Request.Post(uriBuilder.build());
			return execute(request, base, msTimeOut, GraphBase.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GraphBase getBase(String db_name, Integer msTimeOut, Boolean local) {
		try {
			URIBuilder uriBuilder = getGraphBaseUrl(db_name, local, msTimeOut);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, GraphBase.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public GraphBase deleteBase(String db_name, Integer msTimeOut, Boolean local) {
		try {
			URIBuilder uriBuilder = getGraphBaseUrl(db_name, local, msTimeOut);
			Request request = Request.Delete(uriBuilder.build());
			return execute(request, null, msTimeOut, GraphBase.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
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

}
