/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class DirectoryWatcher implements Runnable, Closeable, AutoCloseable {

	private final static Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

	private final Path rootPath;
	private final WatchService watcher;
	private final HashSet<Consumer<Path>> consumers;

	private volatile Consumer<Path>[] consumersCache;
	private volatile Exception exception;

	private final HashMap<WatchKey, Path> keys;

	private DirectoryWatcher(Path rootPath) throws IOException {
		FileSystem fs = FileSystems.getDefault();
		this.watcher = fs.newWatchService();
		this.rootPath = rootPath;
		this.keys = new HashMap<WatchKey, Path>();
		this.consumers = new HashSet<Consumer<Path>>();
		this.consumersCache = new Consumer[0];
		exception = null;
	}

	private final static HashMap<Path, DirectoryWatcher> watchers = new HashMap<Path, DirectoryWatcher>();

	/**
	 * <p>Create a new DirectoryWatcher instance.</p>
	 * <p>A DirectoryWatcher is a running thread listening for events in the file system.</p>
	 *
	 * @param rootPath The path of the monitored directory
	 * @throws IOException
	 */
	public static DirectoryWatcher register(Path rootPath, Consumer<Path> consumer) throws IOException {
		synchronized (watchers) {

			DirectoryWatcher watcher = watchers.get(rootPath);
			if (watcher == null) {
				logger.info("New directory watcher: " + rootPath);
				watcher = new DirectoryWatcher(rootPath);
				watchers.put(rootPath, watcher);
			}
			watcher.register(consumer);
			return watcher;
		}
	}

	private synchronized void register(Consumer<Path> consumer) {
		synchronized (consumers) {
			if (consumers.add(consumer))
				consumersCache = consumers.toArray(new Consumer[consumers.size()]);
		}
	}

	public synchronized void unregister(Consumer<Path> consumer) throws IOException {
		synchronized (consumers) {
			if (consumers.remove(consumer))
				consumersCache = consumers.toArray(new Consumer[consumers.size()]);
			synchronized (watchers) {
				if (consumersCache.length == 0) {
					watchers.remove(rootPath);
					close();
				}
			}
		}
	}

	final public static void registerDirectory(Path rootPath, WatchService watcher, HashMap<WatchKey, Path> keys)
			throws IOException {
		Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
			@Override
			final public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
				if (attrs.isDirectory()) {
					keys.put(file.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public void run() {
		exception = null;
		try {
			registerDirectory(rootPath, watcher, keys);
			// Infinite loop.
			for (; ; ) {
				WatchKey key = watcher.take();
				Path dir = keys.get(key);
				if (dir != null) {

					for (WatchEvent<?> watchEvent : key.pollEvents()) {
						WatchEvent.Kind<?> kind = watchEvent.kind();
						if (kind == StandardWatchEventKinds.OVERFLOW)
							continue;
						Object o = watchEvent.context();
						Path file = (o instanceof Path) ? (Path) o : null;
						if (file == null)
							continue;

						Path child = dir.resolve(file);
						// If this is a new directory, we have to register it
						if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS))
							if (kind == StandardWatchEventKinds.ENTRY_CREATE)
								registerDirectory(child, watcher, keys);
					}
					for (Consumer consumer : consumersCache)
						consumer.accept(dir.toAbsolutePath());
				}
				if (!key.reset()) {
					keys.remove(key);
					if (keys.isEmpty())
						break;
				}
			}
		} catch (IOException | InterruptedException e) {
			exception = e;
			if (logger.isWarnEnabled())
				logger.warn("Directory watcher ends: " + rootPath, e);
		}
	}

	@Override
	public void close() throws IOException {
		if (watcher != null)
			watcher.close();
	}
}