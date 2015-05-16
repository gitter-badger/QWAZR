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
package com.qwazr.store;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.server.ServerException;

@Path("/store_schema")
public class StoreMasterSchemaService implements StoreSchemaServiceInterface {

	/**
	 * Create a new name multi client
	 * 
	 * @param msTimeOut
	 *            the optional time out for remote connection
	 * @param local
	 *            set to true if this should be local
	 * @return a new multi client instance, or null if it is local
	 * @throws URISyntaxException
	 */
	final static StoreSchemaMultiClient getSchemaClient(Integer msTimeOut,
			Boolean local) throws URISyntaxException {
		if (local != null && local)
			return null;
		return StoreSchemaManager.INSTANCE.getNewSchemaClient(msTimeOut);
	}

	final static StoreDataReplicationClient getDataClient(String[][] nodes,
			Integer msTimeOut) throws URISyntaxException, ServerException {
		StoreDataReplicationClient dataClient = StoreDataManager.INSTANCE
				.getNewDataClient(nodes, msTimeOut);
		if (dataClient == null)
			throw new ServerException(Status.INTERNAL_SERVER_ERROR,
					"No data nodes");
		return dataClient;
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		try {
			StoreSchemaMultiClient client = getSchemaClient(msTimeout, local);
			if (client != null)
				return client.getSchemas(false, msTimeout);
			else
				return StoreSchemaManager.INSTANCE.getSchemas();
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			StoreSchemaMultiClient client = getSchemaClient(msTimeout, local);
			if (client != null)
				return client.getSchema(schemaName, false, msTimeout);
			return StoreSchemaManager.INSTANCE.getSchema(schemaName);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDefinition, Boolean local,
			Integer msTimeout) {
		try {
			StoreSchemaManager.checkSchemaDefinition(schemaDefinition);
			StoreSchemaMultiClient client = getSchemaClient(msTimeout, local);
			if (client == null)
				StoreSchemaManager.INSTANCE.createSchema(schemaName,
						schemaDefinition);
			else {
				client.createSchema(schemaName, schemaDefinition, false,
						msTimeout);
				if (schemaDefinition.nodes == null)
					return schemaDefinition;
				getDataClient(schemaDefinition.nodes, msTimeout).createSchema(
						schemaName, msTimeout);
			}
			return schemaDefinition;
		} catch (IOException | ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			StoreSchemaMultiClient nameClient = getSchemaClient(msTimeout,
					local);
			if (nameClient == null)
				return StoreSchemaManager.INSTANCE.deleteSchema(schemaName);
			else {
				StoreSchemaDefinition schemaDefinition = StoreSchemaManager.INSTANCE
						.getSchema(schemaName);
				if (schemaDefinition.nodes == null)
					return schemaDefinition;
				getDataClient(schemaDefinition.nodes, msTimeout).deleteSchema(
						schemaName, msTimeout);
				return schemaDefinition;
			}
		} catch (ServerException | URISyntaxException | IOException e) {
			throw ServerException.getJsonException(e);
		}
	}

}
