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

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import java.net.URISyntaxException;
import java.util.Set;

@RolesAllowed(SemaphoresManager.SERVICE_NAME_SEMAPHORES)
@Path("/semaphores")
@ServiceName(SemaphoresManager.SERVICE_NAME_SEMAPHORES)
public interface SemaphoresServiceInterface extends ServiceInterface {

	@GET
	@Path("/semaphores")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> getSemaphores(@QueryParam("local") Boolean local, @QueryParam("group") String group,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/semaphores/{semaphore_id}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> getSemaphoreOwners(@PathParam("semaphore_id") String semaphore_id, @QueryParam("local") Boolean local,
			@QueryParam("group") String group, @QueryParam("timeout") Integer msTimeout);

	public static SemaphoresServiceInterface getClient() throws URISyntaxException {
		if (ClusterManager.INSTANCE.isCluster())
			return new SemaphoresClusterServiceImpl();
		SemaphoresManager.getInstance();
		return new SemaphoresServiceImpl();
	}
}
