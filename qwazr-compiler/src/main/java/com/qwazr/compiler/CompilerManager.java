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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class CompilerManager {

	public final static String SERVICE_NAME_COMPILER = "compiler";

	private static CompilerManager INSTANCE = null;

	private final static Logger logger = LoggerFactory.getLogger(CompilerManager.class);

	public static void load(ExecutorService executor, File dataDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new CompilerManager(executor, dataDirectory);
	}

	private final File compilerDirectory;
	
	private CompilerManager(ExecutorService executor, File dataDirectory) {
		compilerDirectory = new File(dataDirectory, SERVICE_NAME_COMPILER);
		if (!compilerDirectory.exists())
			compilerDirectory.mkdir();
	}
}
