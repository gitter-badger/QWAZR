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
package com.qwazr.semaphores;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RolesAllowed(SemaphoresManager.SERVICE_NAME_SEMAPHORES)
@Path("/semaphores")
public interface SemaphoresServiceInterface {

	@GET
	@Path("/semaphores")
	@Produces(MediaType.APPLICATION_JSON)
	Set<String> getSemaphores(@QueryParam("local") Boolean local, @QueryParam("group") String group,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/semaphores/{semaphore_id}")
	@Produces(MediaType.APPLICATION_JSON)
	Set<String> getSemaphoreOwners(@PathParam("semaphore_id") String semaphore_id, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout);
}
