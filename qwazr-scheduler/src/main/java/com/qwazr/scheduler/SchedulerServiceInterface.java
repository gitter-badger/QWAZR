/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.scheduler;

import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import java.util.TreeMap;

@RolesAllowed(SchedulerManager.SERVICE_NAME_SCHEDULER)
@Path("/schedulers")
@ServiceName(SchedulerManager.SERVICE_NAME_SCHEDULER)
public interface SchedulerServiceInterface extends ServiceInterface {

	enum ActionEnum {
		run
	}

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	TreeMap<String, String> list();

	@GET
	@Path("/{scheduler_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	SchedulerStatus get(@PathParam("scheduler_name") String scheduler_name, @QueryParam("action") ActionEnum action);

}
