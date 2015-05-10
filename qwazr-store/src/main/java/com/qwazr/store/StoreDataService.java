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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.qwazr.utils.server.ServerException;

@Path("/store_local")
public class StoreDataService extends AbstractService implements
		StoreServiceInterface {

	private File getExistingFile(String schemaName, String path)
			throws ServerException {
		File file = StoreDataManager.INSTANCE.getFile(schemaName, path);
		if (!file.exists())
			throw new ServerException(Status.NOT_FOUND,
					"Error. file not found: " + path);
		return file;
	}

	@Override
	public Response getFile(String schemaName, String path) {
		try {
			File file = getExistingFile(schemaName, path);
			StoreFileResult storeFileResult = new StoreFileResult(file, true);
			if (file.isDirectory())
				return storeFileResult.headerResponse(
						responseJson(storeFileResult)).build();
			else if (file.isFile())
				return storeFileResult.headerResponse(
						responseStream(new FileInputStream(file))).build();
			return responseJson(null).build();
		} catch (ServerException | FileNotFoundException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response getFile(String schemaName) {
		return getFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	public Response headFile(String schemaName, String path) {
		try {
			File file = getExistingFile(schemaName, path);
			return new StoreFileResult(file, false).headerResponse(
					Response.ok()).build();
		} catch (ServerException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response headFile(String schemaName) {
		return headFile(schemaName, StringUtils.EMPTY);
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream) {
		try {
			File file = StoreDataManager.INSTANCE.getFile(schemaName, path);
			if (file.exists() && file.isDirectory())
				throw new ServerException(Status.CONFLICT,
						"Error. A directory already exists: " + path);
			File tmpFile = File.createTempFile("oss-cluster-node", ".upload");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(tmpFile);
				IOUtils.copy(inputStream, fos);
				fos.close();
				if (file.exists())
					file.delete();
				tmpFile.renameTo(file);
				tmpFile = null;
				return new StoreFileResult(file, false).headerResponse(
						Response.status(Status.OK)).build();
			} finally {
				if (fos != null)
					IOUtils.closeQuietly(fos);
				if (tmpFile != null)
					tmpFile.delete();
			}
		} catch (ServerException | IOException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response createDirectory(String schemaName, String path) {
		try {
			File file = StoreDataManager.INSTANCE.getFile(schemaName, path);
			if (file.exists())
				throw new ServerException(Status.CONFLICT,
						"Error. Resource already exists: " + path);
			file.mkdir();
			if (file.exists() && file.isDirectory())
				return new StoreFileResult(file, false).headerResponse(
						Response.status(Status.CREATED)).build();
			File parentFile = file.getParentFile();
			if (parentFile == null || !parentFile.exists())
				throw new ServerException(Status.NOT_ACCEPTABLE,
						"The parent directory does not exists: " + path);
			if (!parentFile.isDirectory())
				throw new ServerException(Status.NOT_ACCEPTABLE,
						"The parent path is not a directory: " + path);
			throw new ServerException(Status.INTERNAL_SERVER_ERROR,
					"Unexpected internal error");
		} catch (ServerException e) {
			return ServerException.getTextException(e).getResponse();
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
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
			return responseText(Status.OK, "File deleted: " + path).build();
		} catch (ServerException e) {
			return ServerException.getTextException(e).getResponse();
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
