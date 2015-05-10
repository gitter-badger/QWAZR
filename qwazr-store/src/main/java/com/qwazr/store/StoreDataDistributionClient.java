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

import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;

import com.qwazr.store.StoreSingleClient.PrefixPath;

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

}
