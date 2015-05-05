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
package com.qwazr.job.scheduler;

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
