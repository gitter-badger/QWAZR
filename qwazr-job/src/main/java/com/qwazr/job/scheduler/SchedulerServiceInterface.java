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
package com.qwazr.job.scheduler;

import com.qwazr.job.JobServer;

import java.util.TreeMap;

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
import javax.ws.rs.core.Response;

@RolesAllowed(JobServer.SERVICE_NAME_SCHEDULER)
@Path("/schedulers")
public interface SchedulerServiceInterface {

	public enum ActionEnum {
		enable, disable, run
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public TreeMap<String, String> list();

	@GET
	@Path("/{scheduler_name}")
	@Produces(MediaType.APPLICATION_JSON)
	public SchedulerStatus get(
			@PathParam("scheduler_name") String scheduler_name,
			@QueryParam("action") ActionEnum action);

	@DELETE
	@Path("/{scheduler_name}")
	public Response delete(@PathParam("scheduler_name") String scheduler_name);

	@POST
	@PUT
	@Path("/{scheduler_name}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SchedulerDefinition set(
			@PathParam("scheduler_name") String scheduler_name,
			SchedulerDefinition scheduler);

}
