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
import com.qwazr.utils.process.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class DynamicRestart implements Closeable {

	private final List<DirectoryWatcher> directorWatchers;

	private final static Logger logger = LoggerFactory.getLogger(DynamicRestart.class);

	DynamicRestart(ExecutorService executorService, File... directories) throws IOException {

		final Map<URL, Path> urlCollector = new LinkedHashMap<URL, Path>();
		for (File directory : directories)
			urlCollector.put(directory.toURI().toURL(), directory.toPath());

		directorWatchers = new ArrayList<DirectoryWatcher>(directories.length);
		for (Path path : urlCollector.values()) {
			DirectoryWatcher directorWatcher = DirectoryWatcher.register(path, new Consumer<Path>() {
				@Override
				public void accept(Path path) {
					ProcessUtils.Restart.restart();
				}
			});
			directorWatchers.add(directorWatcher);
			executorService.execute(directorWatcher);
		}
	}

	@Override
	public void close() throws IOException {
		for (DirectoryWatcher directorWatcher : directorWatchers)
			IOUtils.closeQuietly(directorWatcher);
	}
}
