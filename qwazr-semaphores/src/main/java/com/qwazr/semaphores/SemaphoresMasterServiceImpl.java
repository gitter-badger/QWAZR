/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.semaphores;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.ServerException;

import java.net.URISyntaxException;
import java.util.Set;

public class SemaphoresMasterServiceImpl implements SemaphoresServiceInterface {

	@Override
	public Set<String> getSemaphores() {
		try {
			return getClient().getSemaphores();
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}

	}

	@Override
	public Set<String> getSemaphoreOwners(String semaphore_id) {
		try {
			return getClient().getSemaphoreOwners(semaphore_id);
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}

	private SemaphoresMultiClient getClient() throws URISyntaxException {
		String[] urls = ClusterManager.getInstance().getClusterClient()
				.getActiveNodesByService(SemaphoresManager.SERVICE_NAME_SEMAPHORES, null);
		return new SemaphoresMultiClient(SemaphoresManager.INSTANCE.executorService, urls, null);
	}

}
