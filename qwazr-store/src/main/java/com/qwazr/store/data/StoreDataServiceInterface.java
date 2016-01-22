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
 */
package com.qwazr.store.data;

import com.qwazr.store.StoreServer;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Set;

@RolesAllowed(StoreServer.SERVICE_NAME_STORE)
@ServiceName(StoreServer.SERVICE_NAME_STORE)
public interface StoreDataServiceInterface extends ServiceInterface {

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response getFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
					@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response getFile(@PathParam("schema_name") String schemaName, @QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreFileResult getDirectory(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
					@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreFileResult getDirectory(@PathParam("schema_name") String schemaName, @QueryParam("timeout") Integer msTimeout);

	@HEAD
	@Path("/{schema_name}/{path : .+}")
	Response headFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
					@QueryParam("timeout") Integer msTimeout);

	@HEAD
	@Path("/{schema_name}")
	Response headFile(@PathParam("schema_name") String schemaName, @QueryParam("timeout") Integer msTimeout);

	@PUT
	@POST
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	Response putFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
					InputStream inputStream, @QueryParam("last_modified") Long lastModified,
					@QueryParam("timeout") Integer msTimeout, @QueryParam("target") Integer target);

	@DELETE
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	Response deleteFile(@PathParam("schema_name") String schemaName, @PathParam("path") String path,
					@QueryParam("timeout") Integer msTimeout);

	@POST
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Response createSchema(@PathParam("schema_name") String schemaName, @QueryParam("timeout") Integer msTimeout);

	@DELETE
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Response deleteSchema(@PathParam("schema_name") String schemaName, @QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> getSchemas(@QueryParam("timeout") Integer msTimeout);

}
