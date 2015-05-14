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
package com.qwazr.affinities;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResults;
import com.qwazr.utils.server.ServerException;

@Path("/")
public interface AffinitiesServiceInterface {

	public final static String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";

	@GET
	@Path("/")
	@Produces(APPLICATION_JSON_UTF8)
	public Set<String> list(@Context UriInfo uriInfo);

	@GET
	@Path("/{name}")
	@Produces(APPLICATION_JSON_UTF8)
	public Affinity get(@Context UriInfo uriInfo, @PathParam("name") String name)
			throws ServerException;

	@DELETE
	@Path("/{name}")
	@Produces(APPLICATION_JSON_UTF8)
	public Affinity delete(@Context UriInfo uriInfo,
			@PathParam("name") String name) throws ServerException, IOException;

	@POST
	@PUT
	@Path("/{name}")
	@Consumes(APPLICATION_JSON_UTF8)
	@Produces(APPLICATION_JSON_UTF8)
	public Affinity create(@Context UriInfo uriInfo,
			@PathParam("name") String name, Affinity affinity)
			throws ServerException;

	@POST
	@Path("/{name}/request")
	@Consumes(APPLICATION_JSON_UTF8)
	@Produces(APPLICATION_JSON_UTF8)
	public AffinityResults recommendRequest(@Context UriInfo uriInfo,
			@PathParam("name") String name, AffinityRequest request)
			throws ServerException;

	@POST
	@Path("/requests")
	@Consumes(APPLICATION_JSON_UTF8)
	@Produces(APPLICATION_JSON_UTF8)
	public List<AffinityResults> recommendRequests(@Context UriInfo uriInfo,
			List<AffinityBatchRequest> requests) throws ServerException;

	@POST
	@Path("/batch/requests")
	@Consumes(APPLICATION_JSON_UTF8)
	@Produces(APPLICATION_JSON_UTF8)
	public List<List<AffinityResults>> recommendBatchRequests(
			@Context UriInfo uriInfo, List<List<AffinityBatchRequest>> requests)
			throws ServerException;

	@GET
	@Path("/{name}/crawl-request")
	@Produces(MediaType.TEXT_HTML)
	public String refererTemplateRecommend(@Context UriInfo uriInfo,
			@PathParam("name") String name,
			@HeaderParam("referer") String headerReferer,
			@QueryParam("referer") String queryReferer) throws ServerException;
}
