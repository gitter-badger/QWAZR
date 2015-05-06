/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.analyzer.postagger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.qwazr.utils.server.RestApplication;

@Path("/analyze/postagger")
public interface POSTaggerServiceInterface {

	@Path("/languages")
	@GET
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	Map<String, String> getAvailableLanguages();

	@Path("/languages/{lang_name}")
	@POST
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	List<List<POSToken>> analyze(@PathParam("lang_name") String languageName,
			InputStream content);

	@Path("/filters")
	@GET
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	Set<String> getFilters();

	@Path("/filters/{filter_name}")
	@GET
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	POSFilter getFilter(@PathParam("filter_name") String filterName);

	@Path("/filters/{filter_name}")
	@PUT
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	POSFilter createUpdateFilter(@PathParam("filter_name") String filterName,
			POSFilter filter);

	@Path("/filters/{filter_name}")
	@DELETE
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	POSFilter deleteFilter(@PathParam("filter_name") String filterName);

	@Path("/filters/{filter_name}")
	@POST
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	@Consumes(RestApplication.APPLICATION_JSON_UTF8)
	List<List<POSToken>> applyFilter(
			@PathParam("filter_name") String filterName,
			List<List<POSToken>> sentences);
}
