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
 */
package com.qwazr.store;

import java.io.InputStream;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

public interface StoreDataServiceInterface {

	@GET
	@Path("/{shema_name}/{path : .+}")
	public Response getFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@HEAD
	@Path("/{shema_name}/{path : .+}")
	public Response headFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@PUT
	@Path("/{shema_name}/{path : .+}")
	public Response putFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path, InputStream inputStream);

	@POST
	@Path("/{shema_name}/{path : .+}")
	public Response createDirectory(
			@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@DELETE
	@Path("/{shema_name}/{path : .+}")
	public Response deleteFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

}
