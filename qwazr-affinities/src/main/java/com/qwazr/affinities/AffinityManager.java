/**
 * Copyright 2015 OpenSearchServer Inc.
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
package com.qwazr.affinities;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.qwazr.affinities.model.Affinity;
import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.ServerException;

public class AffinityManager extends DirectoryJsonManager<Affinity> {

	public volatile static AffinityManager INSTANCE = null;

	public static void load(File directory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new AffinityManager(directory);
		} catch (ServerException e) {
			throw new RuntimeException(e);
		}
	}

	protected AffinityManager(File directory) throws ServerException,
			IOException {
		super(directory, Affinity.class);
	}

	@Override
	public Set<String> nameSet() {
		return super.nameSet();
	}

	@Override
	public Affinity get(String name) {
		return super.get(name);
	}

	@Override
	public void set(String name, Affinity affinity) throws IOException,
			ServerException {
		super.set(name, affinity);
	}

	@Override
	public Affinity delete(String name) throws ServerException, IOException {
		return super.delete(name);
	}
}
