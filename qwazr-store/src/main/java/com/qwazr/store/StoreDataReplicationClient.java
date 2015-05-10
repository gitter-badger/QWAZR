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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.qwazr.store.StoreSingleClient.PrefixPath;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.FunctionExceptionCatcher;

public class StoreDataReplicationClient extends
		StoreMultiClientAbstract<String[], StoreDataDistributionClient>
		implements StoreServiceInterface {

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
			return Response.ok("OK", MediaType.TEXT_PLAIN).build();
		} catch (Exception e) {
			throw new ServerException(e).getTextException();
		} finally {
			if (tmpFile != null)
				tmpFile.delete();
		}
	}
}
