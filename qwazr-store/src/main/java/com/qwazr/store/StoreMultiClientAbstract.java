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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;

public abstract class StoreMultiClientAbstract<K, V extends StoreServiceInterface>
		extends JsonMultiClientAbstract<K, V> implements StoreServiceInterface {

	private final boolean childLocal;

	protected StoreMultiClientAbstract(ExecutorService executor,
			V[] clientArray, K[] clientKeys, int msTimeOut, boolean childLocal)
			throws URISyntaxException {
		super(executor, clientArray, clientKeys, msTimeOut);
		this.childLocal = childLocal;
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
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
			StoreSchemaDefinition schemaDef, Boolean local, Integer msTimeout) {
		try {

			if (local != null && local)
				throw new ServerException(Status.NOT_IMPLEMENTED);

			List<FunctionExceptionCatcher<StoreSchemaDefinition>> threads = new ArrayList<>(
					size());
			for (V client : this) {
				threads.add(new FunctionExceptionCatcher<StoreSchemaDefinition>() {
					@Override
					public StoreSchemaDefinition execute() throws Exception {
						return client.createSchema(schemaName, schemaDef,
								childLocal, msTimeout);
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return schemaDef;

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {

			if (local != null && local)
				throw new ServerException(Status.NOT_IMPLEMENTED);

			List<FunctionExceptionCatcher<StoreSchemaDefinition>> threads = new ArrayList<>(
					size());
			for (V client : this) {
				threads.add(new FunctionExceptionCatcher<StoreSchemaDefinition>() {
					@Override
					public StoreSchemaDefinition execute() throws Exception {
						try {
							return client.deleteSchema(schemaName, childLocal,
									msTimeout);
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() == 404)
								return null;
							throw e;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return ThreadUtils.getFirstResult(threads);

		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

}
