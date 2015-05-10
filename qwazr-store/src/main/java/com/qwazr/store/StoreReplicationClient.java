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

public class StoreReplicationClient extends
		JsonMultiClientAbstract<String[], StoreDistributionClient> implements
		StoreServiceInterface {

	private final PrefixPath prefixPath;

	protected StoreReplicationClient(ExecutorService executor,
			String[][] urlMap, PrefixPath prefixPath, int msTimeOut)
			throws URISyntaxException {
		super(executor, new StoreDistributionClient[urlMap.length], urlMap,
				msTimeOut);
		this.prefixPath = prefixPath;
	}

	@Override
	protected StoreDistributionClient newClient(String[] urls, int msTimeOut)
			throws URISyntaxException {
		return new StoreDistributionClient(executor, urls, prefixPath,
				msTimeOut);
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
