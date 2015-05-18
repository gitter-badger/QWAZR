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
 */
package com.qwazr.store.data;

import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.qwazr.utils.server.RestApplication;

public interface StoreDataServiceInterface {

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFile(@PathParam("schema_name") String schemaName,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}/{path : .+}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public StoreFileResult getDirectory(
			@PathParam("schema_name") String schemaName,
			@PathParam("path") String path,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public StoreFileResult getDirectory(
			@PathParam("schema_name") String schemaName,
			@QueryParam("timeout") Integer msTimeout);

	@HEAD
	@Path("/{schema_name}/{path : .+}")
	public Response headFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path,
			@QueryParam("timeout") Integer msTimeout);

	@HEAD
	@Path("/{schema_name}")
	public Response headFile(@PathParam("schema_name") String schemaName,
			@QueryParam("timeout") Integer msTimeout);

	@PUT
	@POST
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response putFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path, InputStream inputStream,
			@QueryParam("last_modified") Long lastModified,
			@QueryParam("timeout") Integer msTimeout,
			@QueryParam("target") Integer target);

	@DELETE
	@Path("/{schema_name}/{path : .+}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response deleteFile(@PathParam("schema_name") String schemaName,
			@PathParam("path") String path,
			@QueryParam("timeout") Integer msTimeout);

	@POST
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Response createSchema(@PathParam("schema_name") String schemaName,
			@QueryParam("timeout") Integer msTimeout);

	@DELETE
	@Path("/{schema_name}")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Response deleteSchema(@PathParam("schema_name") String schemaName,
			@QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/")
	@Produces(RestApplication.APPLICATION_JSON_UTF8)
	public Set<String> getSchemas(@QueryParam("timeout") Integer msTimeout);

}
