/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.analyze.postagger;

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
