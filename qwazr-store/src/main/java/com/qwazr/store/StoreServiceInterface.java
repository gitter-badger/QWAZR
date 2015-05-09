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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.qwazr.utils.server.RestApplication;

public interface StoreServiceInterface {

	@GET
	@Path("/{schema_name}/{path : .+}")
	public Response getFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@GET
	@Path("/{schema_name}")
	public Response getFile(@PathParam("schema_name") String schemaName);

	@HEAD
	@Path("/{schema_name}/{path : .+}")
	public Response headFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@HEAD
	@Path("/{schema_name}")
	public Response headFile(@PathParam("schema_name") String schemaName);

	@PUT
	@Path("/{schema_name}/{path : .+}")
	public Response putFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path, InputStream inputStream);

	@POST
	@Path("/{schema_name}/{path : .+}")
	public Response createDirectory(
			@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@DELETE
	@Path("/{schema_name}/{path : .+}")
	public Response deleteFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path);

	@GET
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public StoreSchemaDefinition getSchema(
			@PathParam("schema_name") String schemaName);

	@POST
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public StoreSchemaDefinition createSchema(
			@PathParam("schema_name") String schemaName,
			@QueryParam("local") Boolean local, StoreSchemaDefinition schemaDef);

	@DELETE
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public StoreSchemaDefinition deleteSchema(
			@PathParam("schema_name") String schemaName,
			@QueryParam("local") Boolean local);

}
