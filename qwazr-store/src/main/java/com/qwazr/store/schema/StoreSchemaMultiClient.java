/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.store.schema;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public class StoreSchemaMultiClient extends
		JsonMultiClientAbstract<String, StoreSchemaSingleClient> implements
		StoreSchemaServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreSchemaMultiClient.class);

	protected StoreSchemaMultiClient(ExecutorService executor, String[] urls,
			Integer msTimeOut) throws URISyntaxException {
		super(executor, new StoreSchemaSingleClient[urls.length], urls,
				msTimeOut);
	}

	@Override
	protected StoreSchemaSingleClient newClient(String url, Integer msTimeOut)
			throws URISyntaxException {
		return new StoreSchemaSingleClient(url, msTimeOut);
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {

		try {

			if (local != null && local)
				return StoreSchemaManager.INSTANCE.getSchemas();

			TreeSet<String> finalResult = new TreeSet<String>();

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (StoreSchemaSingleClient client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							Set<String> result = client.getSchemas(true,
									msTimeout);
							if (result != null)
								synchronized (finalResult) {
									finalResult.addAll(result);
								}
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() != 404)
								throw e;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return finalResult;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}

	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {

		try {

			if (local != null && local)
				return StoreSchemaManager.INSTANCE.getSchema(schemaName);

			for (StoreSchemaSingleClient client : this) {
				try {
					return client.getSchema(schemaName, true, msTimeout);
				} catch (WebApplicationException e) {
					if (e.getResponse().getStatus() != 404)
						throw e;
				}
			}

			throw new ServerException("Schema not found: " + schemaName);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	final public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDef, Boolean local, Integer msTimeout) {
		try {

			List<FunctionExceptionCatcher<StoreSchemaDefinition>> threads = new ArrayList<>(
					size());
			for (StoreSchemaSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<StoreSchemaDefinition>() {
					@Override
					public StoreSchemaDefinition execute() throws Exception {
						return client.createSchema(schemaName, schemaDef, true,
								msTimeout);
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return ThreadUtils.getFirstResult(threads);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	final public StoreSchemaDefinition deleteSchema(String schemaName,
			Boolean local, Integer msTimeout) {
		try {

			List<FunctionExceptionCatcher<StoreSchemaDefinition>> threads = new ArrayList<>(
					size());
			for (StoreSchemaSingleClient client : this) {
				threads.add(new FunctionExceptionCatcher<StoreSchemaDefinition>() {
					@Override
					public StoreSchemaDefinition execute() throws Exception {
						try {
							return client.deleteSchema(schemaName, local,
									msTimeout);
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() != 404)
								throw e;
							return null;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			StoreSchemaDefinition result = ThreadUtils.getFirstResult(threads);
			if (result == null)
				throw new ServerException(Status.NOT_FOUND,
						"Schema not found: " + schemaName);
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Map<String, StoreSchemaRepairStatus> getRepairStatus(
			String schemaName, Boolean local, Integer msTimeout) {
		try {

			TreeMap<String, StoreSchemaRepairStatus> results = new TreeMap<String, StoreSchemaRepairStatus>();

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (StoreSchemaSingleClient client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							Map<String, StoreSchemaRepairStatus> result = client
									.getRepairStatus(schemaName, true,
											msTimeout);
							synchronized (results) {
								results.putAll(result);
							}
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() != 404)
								throw e;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return results;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public StoreSchemaRepairStatus startRepair(String schemaName,
			Boolean local, Integer msTimeout) {
		return iterator().next().startRepair(schemaName, true, msTimeout);
	}

	@Override
	public Map<String, StoreSchemaRepairStatus> stopRepair(String schemaName,
			Boolean local, Integer msTimeout) {
		try {

			TreeMap<String, StoreSchemaRepairStatus> results = new TreeMap<String, StoreSchemaRepairStatus>();

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (StoreSchemaSingleClient client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						Map<String, StoreSchemaRepairStatus> result = client
								.stopRepair(schemaName, true, msTimeout);
						synchronized (results) {
							results.putAll(result);
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return results;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
