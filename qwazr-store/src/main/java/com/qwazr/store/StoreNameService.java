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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.qwazr.utils.server.ServerException;

@Path("/store")
public class StoreNameService implements StoreServiceInterface {

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
	final static StoreNameMultiClient getNameClient(Integer msTimeOut,
			Boolean local) throws URISyntaxException {
		if (local != null && local)
			return null;
		return StoreNameManager.INSTANCE.getNewNameClient(msTimeOut);
	}

	final static StoreDataReplicationClient getDataClient(String[][] nodes,
			Integer msTimeOut) throws URISyntaxException, ServerException {
		StoreDataReplicationClient dataClient = StoreNameManager.INSTANCE
				.getNewDataClient(nodes, msTimeOut);
		if (dataClient == null)
			throw new ServerException(Status.INTERNAL_SERVER_ERROR,
					"No data nodes");
		return dataClient;
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		try {
			return getDataClient(getSchema(schemaName, false, msTimeout).nodes,
					msTimeout).getFile(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		try {
			return getDataClient(getSchema(schemaName, false, msTimeout).nodes,
					msTimeout).headFile(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		try {
			StoreSchemaDefinition schemaDef = getSchemaOrNotFound(schemaName);
			if (target == null) {
				HashFunction m3 = Hashing.murmur3_128();
				target = (m3.hashString(path).asInt() % schemaDef.distribution_factor);
			}
			return getDataClient(schemaDef.nodes, msTimeout).putFile(
					schemaName, path, inputStream, lastModified, msTimeout,
					target);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		try {
			StoreNameMultiClient client = getNameClient(msTimeout, local);
			if (client != null)
				return client.getSchemas(false, msTimeout);
			else
				return StoreNameManager.INSTANCE.getSchemas();
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	private StoreSchemaDefinition getSchemaOrNotFound(String schemaName)
			throws ServerException {
		StoreSchemaDefinition schemaDefinition = StoreNameManager.INSTANCE
				.getSchema(schemaName);
		if (schemaDefinition != null)
			return schemaDefinition;
		throw new ServerException(Status.NOT_FOUND, "Schema not found: "
				+ schemaName);
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			StoreNameMultiClient client = getNameClient(msTimeout, local);
			if (client != null)
				return client.getSchema(schemaName, false, msTimeout);
			return getSchemaOrNotFound(schemaName);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDefinition, Boolean local,
			Integer msTimeout) {
		try {
			StoreNameManager.checkSchemaDefinition(schemaDefinition);
			StoreNameMultiClient client = getNameClient(msTimeout, local);
			if (client == null)
				StoreNameManager.INSTANCE.createSchema(schemaName,
						schemaDefinition);
			else {
				client.createSchema(schemaName, schemaDefinition, false,
						msTimeout);
				if (schemaDefinition.nodes == null)
					return schemaDefinition;
				getDataClient(schemaDefinition.nodes, msTimeout).createSchema(
						schemaName, schemaDefinition, false, msTimeout);
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
			StoreNameMultiClient nameClient = getNameClient(msTimeout, local);
			if (nameClient == null)
				return StoreNameManager.INSTANCE.deleteSchema(schemaName);
			else {
				StoreSchemaDefinition schemaDefinition = getSchemaOrNotFound(schemaName);
				if (schemaDefinition.nodes == null)
					return schemaDefinition;
				getDataClient(schemaDefinition.nodes, msTimeout).deleteSchema(
						schemaName, false, msTimeout);
				return schemaDefinition;
			}
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}
}
