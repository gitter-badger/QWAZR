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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.store.data.StoreDataSingleClient.PrefixPath;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;

public class StoreDataReplicationClient extends
		StoreDataMultiClientAbstract<String[], StoreDataDistributionClient>
		implements StoreDataServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreDataReplicationClient.class);

	protected StoreDataReplicationClient(ExecutorService executor,
			String[][] urlMap, PrefixPath prefixPath, int msTimeOut)
			throws URISyntaxException {
		super(executor, new StoreDataDistributionClient[urlMap.length], urlMap,
				msTimeOut, false);
	}

	@Override
	protected StoreDataDistributionClient newClient(String[] urls, int msTimeOut)
			throws URISyntaxException {
		return new StoreDataDistributionClient(executor, urls, msTimeOut);
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {

		try {

			Response response = headFile(schemaName, path, msTimeout);
			switch (StoreFileResult.getType(response)) {
			case FILE:
				return Response.status(Status.TEMPORARY_REDIRECT)
						.location(StoreFileResult.getAddr(response)).build();
			case DIRECTORY:
				StoreFileResult directoryResult = getDirectory(schemaName,
						path, msTimeout);
				ResponseBuilder builder = Response.ok();
				directoryResult.buildHeader(builder);
				directoryResult.buildEntity(builder);
				return builder.build();
			default:
				throw new ServerException(Status.INTERNAL_SERVER_ERROR,
						"Unknown file type: " + schemaName + '/' + path);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		try {
			List<FunctionExceptionCatcher<Response>> threads = new ArrayList<>(
					size());
			for (StoreDataDistributionClient client : this) {
				threads.add(new FunctionExceptionCatcher<Response>() {
					@Override
					public Response execute() throws Exception {
						try {
							return client.deleteFile(schemaName, path,
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
			Response response = ThreadUtils.getFirstResult(threads);
			if (response == null)
				throw new ServerException(Status.NOT_FOUND, "File not found: "
						+ path);
			return response;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {

		File tmpFile = null;

		try {
			tmpFile = IOUtils.storeAsTempFile(inputStream);

			final File file = tmpFile;

			List<FunctionExceptionCatcher<Response>> threads = new ArrayList<>(
					size());
			for (StoreDataDistributionClient client : this) {
				threads.add(new FunctionExceptionCatcher<Response>() {
					@Override
					public Response execute() throws Exception {
						return client.putFile(schemaName, path,
								new FileInputStream(file), lastModified,
								msTimeout, target);
					}
				});
			}

			ThreadUtils.invokeAndJoin(executor, threads);
			return ThreadUtils.getFirstResult(threads);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getTextException(e);
		} finally {
			if (tmpFile != null)
				tmpFile.delete();
		}
	}
}
