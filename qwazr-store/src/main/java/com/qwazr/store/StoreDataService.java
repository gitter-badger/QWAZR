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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.server.ServerException;

@Path("/store_local")
public class StoreDataService implements StoreServiceInterface {

	private File getExistingFile(String schemaName, String path)
			throws ServerException {
		File file = StoreDataManager.INSTANCE.getFile(schemaName, path);
		if (!file.exists())
			throw new ServerException(Status.NOT_FOUND,
					"Error. file not found: " + path);
		return file;
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		StoreFileResult storeFile = null;
		try {
			File file = getExistingFile(schemaName, path);
			storeFile = new StoreFileResult(file, file.isDirectory());
			ResponseBuilder builder = Response.ok();
			storeFile.buildHeader(builder);
			storeFile.buildEntity(builder);
			return builder.build();
		} catch (ServerException | IOException e) {
			if (storeFile != null)
				storeFile.free();
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		try {
			File file = getExistingFile(schemaName, path);
			StoreFileResult storeFile = new StoreFileResult(file, false);
			ResponseBuilder builder = Response.ok();
			storeFile.buildHeader(builder);
			return builder.build();
		} catch (ServerException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		try {
			File file = StoreDataManager.INSTANCE.putFile(schemaName, path,
					inputStream, lastModified);
			StoreFileResult storeFile = new StoreFileResult(file, false);
			ResponseBuilder builder = Response.ok("File created: " + path,
					MediaType.TEXT_PLAIN);
			storeFile.buildHeader(builder);
			return builder.build();
		} catch (ServerException | IOException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		try {
			File file = StoreDataManager.INSTANCE.getFile(schemaName, path);
			if (!file.exists())
				throw new ServerException(Status.NOT_FOUND,
						"Error. File not found: " + path);
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null && files.length > 0)
					throw new ServerException(Status.NOT_ACCEPTABLE,
							"The directory is not empty");
			}
			file.delete();
			if (file.exists())
				throw new ServerException(Status.INTERNAL_SERVER_ERROR,
						"Unable to delete the file: " + path);
			File parent = file.getParentFile();
			if (parent.list().length == 0)
				parent.delete();
			return Response.ok("File deleted: " + path, MediaType.TEXT_PLAIN)
					.build();
		} catch (ServerException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getJsonException();
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getJsonException();
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDefinition, Boolean local,
			Integer msTimeout) {
		try {
			StoreDataManager.INSTANCE.createSchema(schemaName);
			return schemaDefinition;
		} catch (IOException e) {
			throw new ServerException(e).getJsonException();
		}
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			StoreDataManager.INSTANCE.deleteSchema(schemaName);
			return new StoreSchemaDefinition();
		} catch (IOException e) {
			throw new ServerException(e).getJsonException();
		}
	}
}
