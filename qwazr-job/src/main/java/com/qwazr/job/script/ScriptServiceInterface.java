/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
