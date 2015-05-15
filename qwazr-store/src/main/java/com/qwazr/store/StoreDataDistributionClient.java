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
package com.qwazr.store;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.store.StoreSingleClient.PrefixPath;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;

public class StoreDataDistributionClient extends
		StoreMultiClientAbstract<String, StoreSingleClient> implements
		StoreServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreDataDistributionClient.class);

	protected StoreDataDistributionClient(ExecutorService executor,
			String[] urls, int msTimeOut) throws URISyntaxException {
		super(executor, new StoreSingleClient[urls.length], urls, msTimeOut,
				true);
	}

	@Override
	protected StoreSingleClient newClient(String url, int msTimeOut)
			throws URISyntaxException {
		return new StoreSingleClient(url, PrefixPath.data, msTimeOut);
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {

		try {
			return getClientByPos(target).putFile(schemaName, path,
					inputStream, lastModified, msTimeout, target);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getTextException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {

		try {

			List<FunctionExceptionCatcher<Response>> threads = new ArrayList<>(
					size());
			for (StoreSingleClient client : this) {
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

}
