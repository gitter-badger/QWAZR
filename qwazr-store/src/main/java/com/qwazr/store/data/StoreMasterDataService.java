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
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.qwazr.store.schema.StoreSchemaDefinition;
import com.qwazr.store.schema.StoreSchemaManager;
import com.qwazr.store.schema.StoreSchemaMultiClient;
import com.qwazr.utils.HashUtils;
import com.qwazr.utils.server.ServerException;

@Path("/store")
public class StoreMasterDataService implements StoreDataServiceInterface {

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
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		try {
			return getDataClient(
					StoreSchemaManager.INSTANCE.getSchema(schemaName).nodes,
					msTimeout).getFile(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreFileResult getDirectory(String schemaName, String path,
			Integer msTimeout) {
		try {
			return getDataClient(
					StoreSchemaManager.INSTANCE.getSchema(schemaName).nodes,
					msTimeout).getDirectory(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		try {
			return getDataClient(
					StoreSchemaManager.INSTANCE.getSchema(schemaName).nodes,
					msTimeout).headFile(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	final public StoreFileResult getDirectory(String schemaName,
			Integer msTimeout) {
		return getDirectory(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	final public Response getFile(String schemaName, Integer msTimeout) {
		return getFile(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	final public Response headFile(String schemaName, Integer msTimeout) {
		return headFile(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		try {
			StoreSchemaDefinition schemaDef = StoreSchemaManager.INSTANCE
					.getSchema(schemaName);
			if (target == null)
				target = HashUtils.getMurmur3Mod(path,
						schemaDef.distribution_factor);
			return getDataClient(schemaDef.nodes, msTimeout).putFile(
					schemaName, path, inputStream, lastModified, msTimeout,
					target);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		try {
			return getDataClient(
					StoreSchemaManager.INSTANCE.getSchema(schemaName).nodes,
					msTimeout).deleteFile(schemaName, path, msTimeout);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> getSchemas(Integer msTimeout) {
		try {
			StoreSchemaMultiClient client = StoreSchemaManager.INSTANCE
					.getNewSchemaClient(msTimeout);
			if (client == null)
				return StoreDataManager.INSTANCE.getSchemas();
			else
				return client.getSchemas(false, msTimeout);
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response createSchema(String schemaName, Integer msTimeout) {
		return Response.status(Status.NOT_IMPLEMENTED).build();
	}

	@Override
	public Response deleteSchema(String schemaName, Integer msTimeout) {
		return Response.status(Status.NOT_IMPLEMENTED).build();
	}

}
