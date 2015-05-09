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

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.core.Response;

import com.qwazr.store.StoreSingleClient.PrefixPath;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;

public class StoreDistributionClient extends
		JsonMultiClientAbstract<String, StoreSingleClient> implements
		StoreServiceInterface {

	private final PrefixPath prefixPath;

	protected StoreDistributionClient(ExecutorService executor, String[] urls,
			PrefixPath prefixPath, int msTimeOut) throws URISyntaxException {
		super(executor, new StoreSingleClient[urls.length], urls, msTimeOut);
		this.prefixPath = prefixPath;
	}

	@Override
	protected StoreSingleClient newClient(String url, int msTimeOut)
			throws URISyntaxException {
		return new StoreSingleClient(url, prefixPath, msTimeOut);
	}

	@Override
	public Response getFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
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
