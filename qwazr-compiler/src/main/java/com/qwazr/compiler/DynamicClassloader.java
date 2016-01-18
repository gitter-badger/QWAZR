/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.compiler;

import com.qwazr.utils.DirectoryWatcher;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class DynamicClassloader implements Closeable {

	private final List<DirectoryWatcher> directorWatchers;
	private final URL[] classPathURLs;
	private volatile URLClassLoader volatileClassLoader;
	private final Set<Consumer<ClassLoader>> classLoaderConsumers;
	private final LockUtils.ReadWriteLock consumersLock;

	private final static Logger logger = LoggerFactory.getLogger(DynamicClassloader.class);

	DynamicClassloader(ExecutorService executorService, File... directories) throws IOException {

		consumersLock = new LockUtils.ReadWriteLock();
		classLoaderConsumers = new HashSet<Consumer<ClassLoader>>();

		final Map<URL, Path> urlCollector = new LinkedHashMap<URL, Path>();
		for (File directory : directories)
			urlCollector.put(directory.toURI().toURL(), directory.toPath());

		classPathURLs = urlCollector.keySet().toArray(new URL[urlCollector.size()]);

		directorWatchers = new ArrayList<DirectoryWatcher>(directories.length);
		for (Path path : urlCollector.values()) {
			DirectoryWatcher directorWatcher = DirectoryWatcher.register(path, new Consumer<Path>() {
				@Override
				public void accept(Path path) {
					try {
						resetClassLoader(false);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			});
			directorWatchers.add(directorWatcher);
			executorService.execute(directorWatcher);
		}
	}

	private synchronized void resetClassLoader(boolean closeOnly) throws IOException {
		final URLClassLoader oldClassloader = volatileClassLoader;
		volatileClassLoader = closeOnly ? null : new URLClassLoader(classPathURLs);
		if (oldClassloader != null)
			oldClassloader.close();
		if (volatileClassLoader != null) {
			consumersLock.r.lock();
			try {
				classLoaderConsumers.forEach(classLoaderConsumer -> classLoaderConsumer.accept(volatileClassLoader));
			} finally {
				consumersLock.r.unlock();
			}
		}
	}

	void register(Consumer<ClassLoader> consumer) {
		consumersLock.w.lock();
		try {
			classLoaderConsumers.add(consumer);
		} finally {
			consumersLock.w.unlock();
		}
	}

	void unregister(Consumer<ClassLoader> consumer) {
		consumersLock.w.lock();
		try {
			classLoaderConsumers.remove(consumer);
		} finally {
			consumersLock.w.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		for (DirectoryWatcher directorWatcher : directorWatchers)
			IOUtils.closeQuietly(directorWatcher);
		resetClassLoader(true);
	}

	Class<?> loadClass(String name) throws ClassNotFoundException {
		final URLClassLoader classLoader = volatileClassLoader;
		Objects.requireNonNull(classLoader, "The classloader is null");
		return classLoader.loadClass(name);
	}

	ClassLoader getClassLoader() {
		return volatileClassLoader;
	}
}
