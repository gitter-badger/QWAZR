/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
import com.qwazr.utils.LockUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class SemaphoresManager {

	public static final String SERVICE_NAME_SEMAPHORES = "semaphores";

	private static final Logger logger = LoggerFactory.getLogger(SemaphoresManager.class);

	static SemaphoresManager INSTANCE = null;

	public synchronized static Class<? extends SemaphoresServiceInterface> load(ExecutorService executorService)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new SemaphoresManager(executorService);
			return ClusterManager.INSTANCE.isCluster() ?
					SemaphoresClusterServiceImpl.class :
					SemaphoresServiceImpl.class;
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static SemaphoresManager getInstance() {
		if (SemaphoresManager.INSTANCE == null)
			throw new RuntimeException("The semaphore service is not enabled");
		return SemaphoresManager.INSTANCE;
	}

	final ExecutorService executorService;

	private final LockUtils.ReadWriteLock semaphoreMapLock = new LockUtils.ReadWriteLock();
	private final HashMap<String, Set<String>> semaphoreMap;

	private SemaphoresManager(ExecutorService executorService) throws IOException, URISyntaxException {
		this.executorService = executorService;
		semaphoreMap = new HashMap<String, Set<String>>();
	}

	void getSemaphores(Collection<String> semaphores) {
		semaphoreMapLock.r.lock();
		try {
			semaphores.addAll(semaphoreMap.keySet());
		} finally {
			semaphoreMapLock.r.unlock();
		}
	}

	void getSemaphoreOwners(String semaphore_id, Collection<String> owners) {
		semaphoreMapLock.r.lock();
		try {
			Set<String> ows = semaphoreMap.get(semaphore_id);
			if (ows == null)
				return;
			for (String owner : ows)
				owners.add(owner);
		} finally {
			semaphoreMapLock.r.unlock();
		}
	}

	public void register(String semaphore_id, String owner_id) {
		semaphoreMapLock.w.lock();
		try {
			if (logger.isInfoEnabled())
				logger.info("Register semaphore: " + semaphore_id + " to owner: " + owner_id);
			Set<String> owners = semaphoreMap.get(semaphore_id);
			if (owners == null) {
				owners = new HashSet<String>();
				semaphoreMap.put(semaphore_id, owners);
			}
			owners.add(owner_id);
		} finally {
			semaphoreMapLock.w.unlock();
		}
	}

	public void unregister(String semaphore_id, String owner_id) {
		semaphoreMapLock.w.lock();
		try {
			if (logger.isInfoEnabled())
				logger.info("Unregister semaphore: " + semaphore_id + " to owner: " + owner_id);
			Set<String> owners = semaphoreMap.get(semaphore_id);
			if (owners == null)
				return;
			owners.remove(owner_id);
			if (owners.isEmpty())
				semaphoreMap.remove(semaphore_id);
		} finally {
			semaphoreMapLock.w.unlock();
		}
	}
}
