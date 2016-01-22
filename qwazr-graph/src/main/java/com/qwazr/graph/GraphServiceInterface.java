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

import com.qwazr.graph.model.*;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@RolesAllowed(GraphManager.SERVICE_NAME_GRAPH)
@Path("/graph")
@ServiceName(GraphManager.SERVICE_NAME_GRAPH)
public interface GraphServiceInterface extends ServiceInterface {

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> list();

	@POST
	@Path("/{graph_name}")
	@Consumes(ServiceInterface.APPLICATION_JSON_UTF8)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphDefinition createUpdateGraph(@PathParam("graph_name") String graph_name, GraphDefinition graph_def);

	@GET
	@Path("/{graph_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphResult getGraph(@PathParam("graph_name") String graph_name);

	@DELETE
	@Path("/{graph_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphDefinition deleteGraph(@PathParam("graph_name") String graph_name);

	@POST
	@Path("/{graph_name}/node")
	@Consumes(ServiceInterface.APPLICATION_JSON_UTF8)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> createUpdateNodes(@PathParam("graph_name") String graph_name, LinkedHashMap<String, GraphNode> nodes,
					@QueryParam("upsert") Boolean upsert);

	@POST
	@Path("/{graph_name}/node")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Long createUpdateNodes(@PathParam("graph_name") String graph_name, @QueryParam("upsert") Boolean upsert,
					InputStream inpustStream);

	@POST
	@Path("/{graph_name}/node/{node_id}")
	@Consumes(ServiceInterface.APPLICATION_JSON_UTF8)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphNode createUpdateNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					GraphNode node, @QueryParam("upsert") Boolean upsert);

	@GET
	@Path("/{graph_name}/node/{node_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphNode getNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id);

	@DELETE
	@Path("/{graph_name}/node/{node_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphNode deleteNode(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id);

	@POST
	@Path("/{graph_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphNode createEdge(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					@PathParam("edge_type") String edge_type, @PathParam("to_node_id") String to_node_id);

	@DELETE
	@Path("/{graph_name}/node/{node_id}/edge/{edge_type}/{to_node_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	GraphNode deleteEdge(@PathParam("graph_name") String graph_name, @PathParam("node_id") String node_id,
					@PathParam("edge_type") String edge_type, @PathParam("to_node_id") String to_node_id);

	@POST
	@Path("/{graph_name}/request")
	@Consumes(ServiceInterface.APPLICATION_JSON_UTF8)
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	List<GraphNodeResult> requestNodes(@PathParam("graph_name") String graph_name, GraphRequest request);

}
