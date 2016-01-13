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
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

public class SemaphoresMultiClient extends JsonMultiClientAbstract<String, SemaphoresServiceInterface>
				implements SemaphoresServiceInterface {

	private static final Logger logger = LoggerFactory.getLogger(SemaphoresMultiClient.class);

	SemaphoresMultiClient(ExecutorService executor, String[] urls, Integer msTimeout) throws URISyntaxException {
		super(executor, new SemaphoresSingleClient[urls.length], urls, msTimeout);
	}

	@Override
	protected SemaphoresServiceInterface newClient(String url, Integer msTimeOut) throws URISyntaxException {
		if (url == ClusterManager.getInstance().myAddress)
			return new SemaphoresNodeServiceImpl();
		return new SemaphoresSingleClient(url, msTimeOut);
	}

	@Override
	public Set<String> getSemaphores() {

		try {

			final TreeSet<String> semaphores = new TreeSet<String>();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (SemaphoresServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							synchronized (this) {
								semaphores.addAll(client.getSemaphores());
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
			return semaphores;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> getSemaphoreOwners(String semaphore_id) {

		try {

			final TreeSet<String> ownerSet = new TreeSet<String>();
			List<ProcedureExceptionCatcher> threads = new ArrayList<>(size());
			for (SemaphoresServiceInterface client : this) {
				threads.add(new ProcedureExceptionCatcher() {
					@Override
					public void execute() throws Exception {
						try {
							synchronized (this) {
								ownerSet.addAll(client.getSemaphoreOwners(semaphore_id));
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
			return ownerSet;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw ServerException.getJsonException(e);
		}
	}

}
