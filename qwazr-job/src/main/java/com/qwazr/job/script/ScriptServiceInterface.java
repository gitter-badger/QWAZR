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
package com.qwazr.job.script;

import java.util.Map;
import java.util.TreeMap;

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
import javax.ws.rs.core.Response;

@Path("/scripts")
public interface ScriptServiceInterface {

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public TreeMap<String, ScriptFileStatus> getScripts(
			@QueryParam("local") Boolean local);

	@GET
	@Path("/{script_name}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getScript(@PathParam("script_name") String script_name);

	@DELETE
	@Path("/{script_name}")
	public Response deleteScript(@PathParam("script_name") String script_name,
			@QueryParam("local") Boolean local);

	@POST
	@PUT
	@Path("/{script_name}")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response setScript(@PathParam("script_name") String script_name,
			@QueryParam("last_modified") Long last_modified,
			@QueryParam("local") Boolean local, String script);

	@GET
	@Path("/{script_name}/run")
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus runScript(
			@PathParam("script_name") String script_name);

	@POST
	@Path("/{script_name}/run")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus runScriptVariables(
			@PathParam("script_name") String script_name,
			Map<String, String> variables);

	@GET
	@Path("/{script_name}/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, ScriptRunStatus> getRunsStatus(
			@PathParam("script_name") String script_name,
			@QueryParam("local") Boolean local);

	@GET
	@Path("/{script_name}/status/{run_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ScriptRunStatus getRunStatus(
			@PathParam("script_name") String script_name,
			@PathParam("run_id") String run_id);

	@GET
	@Path("/{script_name}/status/{run_id}/out")
	@Produces(MediaType.TEXT_PLAIN)
	public String getRunOut(@PathParam("script_name") String script_name,
			@PathParam("run_id") String run_id);

	@GET
	@Path("/{script_name}/status/{run_id}/err")
	@Produces(MediaType.TEXT_PLAIN)
	public String getRunErr(@PathParam("script_name") String script_name,
			@PathParam("run_id") String run_id);

}
