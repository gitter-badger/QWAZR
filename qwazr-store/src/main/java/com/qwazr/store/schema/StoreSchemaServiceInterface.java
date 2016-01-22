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
package com.qwazr.store.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.store.StoreServer;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@ServiceName(StoreServer.SERVICE_NAME_STORE)
public interface StoreSchemaServiceInterface extends ServiceInterface {

	TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@GET
	@Path("/")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Set<String> getSchemas(@QueryParam("local") Boolean local, @QueryParam("timeout") Integer msTimeout);

	@GET
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreSchemaDefinition getSchema(@PathParam("schema_name") String schemaName, @QueryParam("local") Boolean local,
					@QueryParam("timeout") Integer msTimeout);

	@POST
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreSchemaDefinition createSchema(@PathParam("schema_name") String schemaName, StoreSchemaDefinition schemaDef,
					@QueryParam("local") Boolean local, @QueryParam("timeout") Integer msTimeout);

	@DELETE
	@Path("/{schema_name}")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreSchemaDefinition deleteSchema(@PathParam("schema_name") String schemaName, @QueryParam("local") Boolean local,
					@QueryParam("timeout") Integer msTimeout);

	TypeReference<TreeMap<String, StoreSchemaRepairStatus>> MapStringRepairTypeRef = new TypeReference<TreeMap<String, StoreSchemaRepairStatus>>() {
	};

	@GET
	@Path("/{schema_name}/repair")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Map<String, StoreSchemaRepairStatus> getRepairStatus(@PathParam("schema_name") String schemaName,
					@QueryParam("local") Boolean local, @QueryParam("timeout") Integer msTimeout);

	@POST
	@Path("/{schema_name}/repair")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	StoreSchemaRepairStatus startRepair(@PathParam("schema_name") String schemaName, @QueryParam("local") Boolean local,
					@QueryParam("timeout") Integer msTimeout);

	@DELETE
	@Path("/{schema_name}/repair")
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	Map<String, StoreSchemaRepairStatus> stopRepair(@PathParam("schema_name") String schemaName,
					@QueryParam("local") Boolean local, @QueryParam("timeout") Integer msTimeout);

}
