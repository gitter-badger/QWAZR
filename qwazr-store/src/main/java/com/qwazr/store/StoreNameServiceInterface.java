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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

public interface StoreNameServiceInterface extends StoreDataServiceInterface {

	@GET
	@Path("/{shema_name}")
	public StoreSchemaDefinition getSchema(
			@PathParam("schema_name") String schemaName);

	@POST
	@Path("/{shema_name}")
	public StoreSchemaDefinition postSchema(
			@PathParam("schema_name") String schemaName,
			StoreSchemaDefinition schemaDef);

	@DELETE
	@Path("/{shema_name}")
	public StoreSchemaDefinition deleteSchema(
			@PathParam("schema_name") String schemaName);

}
