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
package com.qwazr.classloader;

import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderManager {

	private static volatile ClassLoaderManager INSTANCE = null;

	public static ClassLoaderManager getInstance() {
		return INSTANCE;
	}

	public static void load(File dataDirectory, Thread mainThread) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new ClassLoaderManager(dataDirectory, mainThread);
	}

	public static volatile URLClassLoader classLoader = null;

	public static <T> Class<T> findClass(String className) throws ClassNotFoundException {
		return ClassLoaderUtils.findClass(classLoader, className);
	}

	public final File javaResourceDirectory;
	public final File javaClassesDirectory;
	public final File javaLibrariesDirectory;

	private final ClassLoader parentClassLoader;
	private final Thread mainThread;

	private final URL[] urls;

	private ClassLoaderManager(File dataDirectory, Thread mainThread) throws MalformedURLException {
		this.mainThread = mainThread;
		this.parentClassLoader = mainThread.getContextClassLoader();
		this.javaResourceDirectory = new File(dataDirectory, "src/main/resources");
		this.javaClassesDirectory = new File(dataDirectory, "target/classes");
		if (!javaClassesDirectory.exists())
			javaClassesDirectory.mkdirs();
		this.javaLibrariesDirectory = new File(dataDirectory, "lib");
		urls = new URL[] { javaResourceDirectory.toURI().toURL(), javaClassesDirectory.toURI().toURL(),
				javaLibrariesDirectory.toURI().toURL() };
		reload();
	}

	public synchronized void reload() {
		URLClassLoader oldClassLoader = classLoader;
		URLClassLoader newClassLoader = new URLClassLoader(urls, parentClassLoader);
		mainThread.setContextClassLoader(newClassLoader);
		classLoader = newClassLoader;
		if (oldClassLoader != null)
			IOUtils.close(oldClassLoader);
	}

}
