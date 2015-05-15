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

import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.store.StoreSingleClient.PrefixPath;
import com.qwazr.utils.server.ServerException;

public class StoreNameMultiClient extends
		StoreMultiClientAbstract<String, StoreSingleClient> implements
		StoreServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreNameMultiClient.class);

	protected StoreNameMultiClient(ExecutorService executor, String[] urls,
			int msTimeOut) throws URISyntaxException {
		super(executor, new StoreSingleClient[urls.length], urls, msTimeOut,
				true);
	}

	@Override
	protected StoreSingleClient newClient(String url, int msTimeOut)
			throws URISyntaxException {
		return new StoreSingleClient(url, PrefixPath.name, msTimeOut);
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		try {

			if (local != null && local)
				throw new ServerException(Status.NOT_IMPLEMENTED);

			return iterator().next().getSchemas(true, msTimeout);

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
				throw new ServerException(Status.NOT_IMPLEMENTED);

			return iterator().next().getSchema(schemaName, true, msTimeout);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
