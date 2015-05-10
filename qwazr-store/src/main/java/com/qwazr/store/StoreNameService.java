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

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.qwazr.utils.server.ServerException;

@Path("/store")
public class StoreNameService implements StoreServiceInterface {

	@Override
	public Response getFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response headFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response getFile(String schemaName) {
		return getFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	public Response headFile(String schemaName) {
		return headFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response createDirectory(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName) {
		try {
			StoreSchemaDefinition schemaDefinition = StoreNameManager.INSTANCE
					.getSchema(schemaName);
			if (schemaDefinition == null)
				throw new ServerException(Status.NOT_FOUND,
						"Schema not found: " + schemaName);
			return schemaDefinition;
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName, Boolean local,
			StoreSchemaDefinition schemaDefinition) {
		try {
			StoreNameManager.checkSchemaDefinition(schemaDefinition);
			if (local != null && local) {
				StoreNameManager.INSTANCE.createSchema(schemaName,
						schemaDefinition);
			} else {
				StoreNameManager.INSTANCE.getNewNameClient(60000).createSchema(
						schemaName, false, schemaDefinition);
				StoreNameManager.INSTANCE.getNewDataClient(
						schemaDefinition.nodes, 60000).createSchema(schemaName,
						false, schemaDefinition);
			}
			return schemaDefinition;
		} catch (IOException | ServerException | URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local) {
		try {
			// TODO multi servers
			return StoreNameManager.INSTANCE.deleteSchema(schemaName);
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

}
