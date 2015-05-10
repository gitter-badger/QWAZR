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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.json.client.JsonClientAbstract;

class StoreSingleClient extends JsonClientAbstract implements
		StoreServiceInterface {

	static enum PrefixPath {

		name("/store/"), data("/store_local/");

		final String path;

		PrefixPath(String path) {
			this.path = path;
		}
	}

	private final PrefixPath prefixPath;

	StoreSingleClient(String url, PrefixPath prefixPath, int msTimeOut)
			throws URISyntaxException {
		super(url, msTimeOut);
		this.prefixPath = prefixPath;
	}

	@Override
	public Response getFile(String schemaName, String path) {
		try {
			URIBuilder uriBuilder = getBaseUrl(prefixPath.path, path);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, Response.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response getFile(String schemaName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response headFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response headFile(String schemaName) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName, Boolean local,
			StoreSchemaDefinition schemaDef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local) {
		// TODO Auto-generated method stub
		return null;
	}

}
