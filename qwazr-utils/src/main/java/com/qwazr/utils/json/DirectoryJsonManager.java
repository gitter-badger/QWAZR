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
package com.qwazr.utils.json;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.utils.server.ServerException;

public class DirectoryJsonManager<T> {

	private final ReadWriteLock rwl = new ReentrantReadWriteLock();

	protected final File directory;

	private final Map<String, T> instancesMap;

	private volatile Map<String, T> instancesCache;

	private final Class<T> instanceClass;

	protected DirectoryJsonManager(File directory, Class<T> instanceClass)
			throws JsonGenerationException, JsonMappingException,
			JsonParseException, IOException, ServerException {
		this.instanceClass = instanceClass;
		this.directory = directory;
		this.instancesMap = new LinkedHashMap<String, T>();
		load();
	}

	private File getFile(String name) {
		return new File(directory, name + ".json");
	}

	protected void load() throws JsonGenerationException, JsonMappingException,
			JsonParseException, IOException, ServerException {
		try {
			File[] files = directory.listFiles(JsonFileFilter.INSTANCE);
			if (files == null)
				return;
			for (File file : files) {
				String name = file.getName();
				name = name.substring(0, name.length() - 5);
				set(name, JsonMapper.MAPPER.readValue(file, instanceClass));
			}
		} finally {
			buildCache();
		}
	}

	private void buildCache() {
		instancesCache = new LinkedHashMap<String, T>(instancesMap);
	}

	protected T delete(String name) throws ServerException {
		if (StringUtils.isEmpty(name))
			return null;
		name = name.intern();
		rwl.writeLock().lock();
		try {
			getFile(name).delete();
			T instance = instancesMap.remove(name);
			buildCache();
			return instance;
		} finally {
			rwl.writeLock().unlock();
		}
	}

	protected void set(String name, T instance) throws JsonGenerationException,
			JsonMappingException, IOException, ServerException {
		if (instance == null)
			return;
		if (StringUtils.isEmpty(name))
			return;
		name = name.intern();
		rwl.writeLock().lock();
		try {
			JsonMapper.MAPPER.writeValue(getFile(name), instance);
			instancesMap.put(name, instance);
			buildCache();
		} finally {
			rwl.writeLock().unlock();
		}
	}

	protected T get(String name) {
		return instancesCache.get(name.intern());
	}

	protected Set<String> nameSet() {
		return instancesCache.keySet();
	}
}
