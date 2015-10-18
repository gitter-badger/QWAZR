/**
 * Copyright 2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.graph;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
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

import com.qwazr.graph.model.GraphDefinition;
import com.qwazr.graph.model.GraphNode;
import com.qwazr.graph.model.GraphNodeResult;
import com.qwazr.graph.model.GraphRequest;
import com.qwazr.graph.model.GraphResult;
import com.qwazr.utils.server.RestApplication;

@RolesAllowed(GraphServer.SERVICE_NAME_GRAPH)
@Path("/graph")
public interface GraphServiceInterface {

	@GET
	@Path("/")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Set<String> list(@QueryParam("timeout") Integer msTimeOut, @QueryParam("local") Boolean local);

	@PUT
	@POST
	@Path("/{graph_name}")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphDefinition createUpdateGraph(@PathParam("graph_name") String graph_name, GraphDefinition graph_def,
					@QueryParam("timeout") Integer msTimeOut, @QueryParam("local") Boolean local);

	@GET
	@Path("/{graph_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphResult getGraph(@PathParam("graph_name") String graph_name, @QueryParam("timeout") Integer msTimeOut,
					@QueryParam("local") Boolean local);

	@DELETE
	@Path("/{graph_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphDefinition deleteGraph(@PathParam("graph_name") String graph_name,
					@QueryParam("timeout") Integer msTimeOut, @QueryParam("local") Boolean local);

	@PUT
	@POST
	@Path("/{graph_name}/node")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Set<String> createUpdateNodes(@PathParam("graph_name") String graph_name,
					LinkedHashMap<String, GraphNode> nodes, @QueryParam("upsert") Boolean upsert);

	@PUT
	@POST
	@Path("/{graph_name}/node")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Long createUpdateNodes(@PathParam("graph_name") String graph_name, @QueryParam("upsert") Boolean upsert,
					InputStream inpustStream);

	@PUT
	@POST
	@Path("/{graph_name}/node/{node_id}")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode createUpdateNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					GraphNode node, @QueryParam("upsert") Boolean upsert);

	@GET
	@Path("/{graph_name}/node/{node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode getNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id);

	@DELETE
	@Path("/{graph_name}/node/{node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode deleteNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id);

	@PUT
	@POST
	@Path("/{graph_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode createEdge(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					@PathParam("edge_type") String edge_type, @PathParam("to_node_id") String to_node_id);

	@DELETE
	@Path("/{graph_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public GraphNode deleteEdge(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					@PathParam("edge_type") String edge_type, @PathParam("to_node_id") String to_node_id);

	@POST
	@Path("/{graph_name}/request")
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public List<GraphNodeResult> requestNodes(@PathParam("graph_name") String graph_name, GraphRequest request);

}
