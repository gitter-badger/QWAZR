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

import org.apache.commons.io.IOUtils;

import com.qwazr.store.StoreSingleClient.PrefixPath;
import com.qwazr.utils.server.ServerException;

public class StoreDataDistributionClient extends
		StoreMultiClientAbstract<String, StoreSingleClient> implements
		StoreServiceInterface {

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
			throw new ServerException(e).getTextException();
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

}
