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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.store.data.StoreFileResult.DirMerger;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

public abstract class StoreDataMultiClientAbstract<K, V extends StoreDataServiceInterface>
		extends JsonMultiClientAbstract<K, V> implements
		StoreDataServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreDataMultiClientAbstract.class);

	protected StoreDataMultiClientAbstract(ExecutorService executor,
			V[] clientArray, K[] clientKeys, int msTimeOut, boolean childLocal)
			throws URISyntaxException {
		super(executor, clientArray, clientKeys, msTimeOut);
	}

	@Override
	final public StoreFileResult getDirectory(String schemaName, String path,
			Integer msTimeout) {

		try {

			final DirMerger dirMerger = new DirMerger();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (StoreDataServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {

						try {
							dirMerger.syncMerge(client.getDirectory(schemaName,
									path, msTimeout));
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() != 404)
								throw e;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			if (dirMerger.mergedDirResult == null)
				throw new ServerException(Status.NOT_FOUND, "File not found: "
						+ path);
			return dirMerger.mergedDirResult;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Set<String> getSchemas(Integer msTimeout) {

		try {

			final TreeSet<String> schemaSet = new TreeSet<String>();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (StoreDataServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							synchronized (this) {
								schemaSet.addAll(client.getSchemas(msTimeout));
							}
						} catch (WebApplicationException e) {
							switch (e.getResponse().getStatus()) {
							case 404:
								break;
							default:
								throw e;
							}
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return schemaSet;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
	}

	@Override
	final public Response headFile(String schemaName, String path,
			Integer msTimeout) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (V client : this) {
			try {
				return client.headFile(schemaName, path, msTimeout);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		throw exceptionHolder.getException();
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
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
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		throw new ServerException(Status.NOT_IMPLEMENTED).getTextException();
	}

	@Override
	final public Response createSchema(String schemaName, Integer msTimeout) {
		try {

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (V client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						client.createSchema(schemaName, msTimeout);
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return Response.ok("Schema created: " + schemaName).build();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	final public Response deleteSchema(String schemaName, Integer msTimeout) {
		try {

			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (V client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							client.deleteSchema(schemaName, msTimeout);
						} catch (WebApplicationException e) {
							if (e.getResponse().getStatus() != 404)
								throw e;
						}
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return Response.ok("Schema deleted: " + schemaName).build();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
