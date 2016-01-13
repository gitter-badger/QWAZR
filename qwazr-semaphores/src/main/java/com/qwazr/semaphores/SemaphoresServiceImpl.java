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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SemaphoresServiceImpl implements SemaphoresServiceInterface {

	@Override
	public Set<String> getSemaphores(Boolean local, String group, Integer msTimeout) {
		if (!ClusterManager.getInstance().isGroup(group))
			return Collections.emptySet();
		Set<String> semaphores = new HashSet<String>();
		SemaphoresManager.INSTANCE.getSemaphores(semaphores);
		return semaphores;
	}

	@Override
	public Set<String> getSemaphoreOwners(String semaphore_id, Boolean local, String group, Integer msTimeout) {
		if (!ClusterManager.getInstance().isGroup(group))
			return Collections.emptySet();
		Set<String> owners = new HashSet<String>();
		SemaphoresManager.INSTANCE.getSemaphoreOwners(semaphore_id, owners);
		return owners;
	}
}
