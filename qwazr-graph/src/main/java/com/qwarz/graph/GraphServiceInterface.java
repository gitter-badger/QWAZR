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

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.utils.server.RestApplication;

@Path("/graph")
public interface GraphServiceInterface {

	@GET
	@Path("/")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Set<String> list(@QueryParam("local") Boolean local);

	@PUT
	@POST
	@Path("/{db_name}")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphBase createUpdateBase(@PathParam("db_name") String db_name,
			GraphBase base, @QueryParam("local") Boolean local);

	@GET
	@Path("/{db_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphBase getBase(@PathParam("db_name") String db_name,
			@QueryParam("local") Boolean local);

	@DELETE
	@Path("/{db_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphBase deleteBase(@PathParam("db_name") String db_name,
			@QueryParam("local") Boolean local);

	@PUT
	@POST
	@Path("/{db_name}/node")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Set<String> createUpdateNodes(@PathParam("db_name") String db_name,
			LinkedHashMap<String, GraphNode> nodes,
			@QueryParam("upsert") Boolean upsert);

	@PUT
	@POST
	@Path("/{db_name}/node")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Long createUpdateNodes(@PathParam("db_name") String db_name,
			@QueryParam("upsert") Boolean upsert, InputStream inpustStream);

	@PUT
	@POST
	@Path("/{db_name}/node/{node_id}")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode createUpdateNode(@PathParam("db_name") String db_name,
			@PathParam("node_id") String node_id, GraphNode node,
			@QueryParam("upsert") Boolean upsert);

	@GET
	@Path("/{db_name}/node/{node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode getNode(@PathParam("db_name") String db_name,
			@PathParam("node_id") String node_id);

	@DELETE
	@Path("/{db_name}/node/{node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode deleteNode(@PathParam("db_name") String db_name,
			@PathParam("node_id") String node_id);

	@PUT
	@POST
	@Path("/{db_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode createEdge(@PathParam("db_name") String db_name,
			@PathParam("node_id") String node_id,
			@PathParam("edge_type") String edge_type,
			@PathParam("to_node_id") String to_node_id);

	@DELETE
	@Path("/{db_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode deleteEdge(@PathParam("db_name") String db_name,
			@PathParam("node_id") String node_id,
			@PathParam("edge_type") String edge_type,
			@PathParam("to_node_id") String to_node_id);

	@POST
	@Path("/{db_name}/request")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public List<GraphNodeResult> requestNodes(
			@PathParam("db_name") String db_name, GraphRequest request);

}
