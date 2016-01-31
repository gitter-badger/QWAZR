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

import com.qwazr.classloader.ClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;

public class CompilerManager {

	public final static String SERVICE_NAME_COMPILER = "compiler";

	private static volatile CompilerManager INSTANCE = null;

	private final static Logger logger = LoggerFactory.getLogger(CompilerManager.class);

	public static Class<? extends CompilerServiceInterface> load(ExecutorService executor, File dataDirectory)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new CompilerManager(executor, dataDirectory);
			return CompilerServiceImpl.class;
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static void unload(File directory) throws IOException {
		CompilerManager oldInstance = INSTANCE;
		if (oldInstance == null)
			return;
		INSTANCE = null;
	}

	public static CompilerManager getInstance() {
		return INSTANCE;
	}

	private final File javaSourceDirectory;

	private final JavaCompiler javaCompiler;

	private CompilerManager(ExecutorService executor, File dataDirectory) throws IOException, URISyntaxException {
		ClassLoaderManager classLoaderManager = ClassLoaderManager.getInstance();
		javaSourceDirectory = new File(dataDirectory, "src/main/java");
		if (!classLoaderManager.javaClassesDirectory.exists())
			classLoaderManager.javaClassesDirectory.mkdirs();
		javaCompiler = JavaCompiler.newInstance(executor, javaSourceDirectory, classLoaderManager.javaClassesDirectory,
				classLoaderManager.javaLibrariesDirectory);
	}

}
