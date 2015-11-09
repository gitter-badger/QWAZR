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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;

import javax.script.ScriptException;
import javax.tools.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class FileClassCompilerLoader implements Closeable, AutoCloseable {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JavacDefinition {
		public final String source_root = null;
		public final List<String> classpath = null;
	}

	private volatile URLClassLoader classLoader;

	private final File sourceRootFile;

	private final String sourceRootPrefix;
	private final int sourceRootPrefixLength;
	private final URL[] sourceRootURLs;
	private final String classPath;

	private final Map<String, Long> lastModifiedMap;
	private final LockUtils.ReadWriteLock mapRwl;

	public FileClassCompilerLoader(JavacDefinition javacDefinition) throws MalformedURLException, URISyntaxException {
		sourceRootFile = new File(javacDefinition.source_root);
		List<URL> urlList = new ArrayList<URL>();
		if (sourceRootFile != null)
			urlList.add(sourceRootFile.toURI().toURL());
		this.classPath = buildClassPath(javacDefinition.classpath, urlList);
		this.sourceRootPrefix = sourceRootFile.getAbsolutePath();
		this.sourceRootURLs = urlList.toArray(new URL[urlList.size()]);
		this.sourceRootPrefixLength = sourceRootPrefix.length();
		lastModifiedMap = new HashMap<String, Long>();
		mapRwl = new LockUtils.ReadWriteLock();
	}

	private final static String buildClassPath(Collection<String> classPath, Collection<URL> urlCollection)
			throws MalformedURLException, URISyntaxException {
		final List<String> classPathes = new ArrayList<String>();

		URLClassLoader classLoader = (URLClassLoader) URLClassLoader.getSystemClassLoader();
		if (classLoader != null && classLoader.getURLs() != null) {
			for (URL url : classLoader.getURLs()) {
				classPathes.add(new File(url.toURI()).getAbsolutePath());
				urlCollection.add(url);
			}
		}

		if (classPath != null) {
			for (String cp : classPath) {
				File file = new File(cp);
				if (file.isDirectory()) {
					for (File f : file.listFiles((FileFilter) FileFileFilter.FILE)) {
						classPathes.add(f.getAbsolutePath());
						urlCollection.add(f.toURI().toURL());
					}
				} else if (file.isFile()) {
					classPathes.add(file.getAbsolutePath());
					urlCollection.add(file.toURI().toURL());
				}
			}
		}
		if (classPathes.isEmpty())
			return null;
		return StringUtils.join(classPathes, ';');
	}

	private synchronized void resetClassLoader(boolean closeOnly) throws IOException {
		if (classLoader != null) {
			classLoader.close();
			classLoader = null;
		}
		if (!closeOnly)
			classLoader = new URLClassLoader(sourceRootURLs);
	}

	public <T> Class<T> loadClass(File sourceFile)
			throws IOException, ReflectiveOperationException, InterruptedException {
		String sourcePath = sourceFile.getAbsolutePath();
		if (!sourcePath.startsWith(sourceRootPrefix))
			throw new IOException("The file is not in the source root: " + sourceFile + " / " + sourceRootFile);
		String baseName = sourcePath.substring(sourceRootPrefixLength);
		baseName = FilenameUtils.getBaseName(StringUtils.join(StringUtils.split(baseName, '\\'), '.'));
		long sourceFileLastModified = sourceFile.lastModified();

		mapRwl.r.lock();
		try {
			Long time = lastModifiedMap.get(baseName);
			if (time != null && time == sourceFileLastModified)
				return (Class<T>) classLoader.loadClass(baseName);
		} finally {
			mapRwl.r.unlock();
		}

		mapRwl.w.lock();
		try {
			Long time = lastModifiedMap.get(baseName);
			if (time != null && time == sourceFileLastModified)
				return (Class<T>) classLoader.loadClass(baseName);
			compile(sourceFile);
			lastModifiedMap.put(baseName, sourceFileLastModified);
			resetClassLoader(false);
			return (Class<T>) classLoader.loadClass(baseName);
		} finally {
			mapRwl.w.unlock();
		}
	}

	private void compile(File sourceFile) throws IOException {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
			throw new IOException("No compiler is available. This feature requires a JDK (not a JRE).");
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		try {
			Iterable<? extends JavaFileObject> sourceFiles = fileManager.getJavaFileObjects(sourceFile);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			final List<String> options = new ArrayList<String>();
			if (classPath != null) {
				options.add("-classpath");
				options.add(classPath);
			}
			options.add("-sourcepath");
			options.add(sourceRootPrefix);
			JavaCompiler.CompilationTask task = compiler
					.getTask(pw, fileManager, diagnostics, options, null, sourceFiles);
			if (!task.call()) {
				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
					pw.format("Error on line %d in %s%n%s%n", diagnostic.getLineNumber(),
							diagnostic.getSource().toUri(), diagnostic.getMessage(null));
				pw.flush();
				pw.close();
				sw.close();
				throw new IOException(sw.toString());
			}
		} finally {
			fileManager.close();
		}
	}

	@Override
	public void close() throws IOException {
		resetClassLoader(true);
	}
}
