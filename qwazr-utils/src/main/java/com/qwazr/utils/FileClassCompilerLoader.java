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
package com.qwazr.utils;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class FileClassCompilerLoader implements Closeable {

	private final static Logger logger = LoggerFactory.getLogger(FileClassCompilerLoader.class);

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JavacDefinition {
		public final String source_root = null;
		public final String classes_root = null;
		public final List<String> classpath = null;
	}

	private volatile URLClassLoader classLoader;
	private volatile UUID currentVersion;

	private final String sourceRootPrefix;
	private final File classesRootFile;
	private final int sourceRootPrefixLength;
	private final URL[] sourceRootURLs;
	private final String classPath;

	private final Map<File, Long> lastModifiedMap;
	private final LockUtils.ReadWriteLock mapRwl;

	private final DirectoryWatcher directorWatcher;

	private FileClassCompilerLoader(ExecutorService executorService, Path sourceRootPath, Path classesRootPath,
					String classPath, Collection<URL> urlList) throws IOException {
		this.classPath = classPath;
		this.sourceRootPrefix = sourceRootPath.toFile().getAbsolutePath();
		this.classesRootFile = sourceRootPath != null ? classesRootPath.toFile() : null;
		if (this.classesRootFile != null && !this.classesRootFile.exists())
			this.classesRootFile.mkdir();
		this.sourceRootURLs = urlList.toArray(new URL[urlList.size()]);
		this.sourceRootPrefixLength = sourceRootPrefix.length();
		lastModifiedMap = new HashMap<File, Long>();
		currentVersion = UUIDs.timeBased();
		mapRwl = new LockUtils.ReadWriteLock();
		directorWatcher = DirectoryWatcher.register(sourceRootPath, new Consumer<Path>() {
			@Override
			public void accept(Path path) {
				mapRwl.w.lock();
				try {
					currentVersion = UUIDs.timeBased();
					if (logger.isInfoEnabled())
						logger.info("Path changes: " + sourceRootPrefix + " " + currentVersion);
					lastModifiedMap.clear();
				} finally {
					mapRwl.w.unlock();
				}
			}
		});
		executorService.execute(directorWatcher);
	}

	public static FileClassCompilerLoader newInstance(ExecutorService executorService, JavacDefinition javacDefinition)
					throws IOException, URISyntaxException {
		if (javacDefinition == null)
			throw new NullPointerException("No JavacDefinition has been given (null)");
		if (javacDefinition.source_root == null)
			throw new NullPointerException("No source_root has been given (null)");
		final FileSystem fs = FileSystems.getDefault();
		final Path sourceRootPath = fs.getPath(javacDefinition.source_root);
		final Path classesRootPath = fs.getPath(javacDefinition.classes_root);
		final List<URL> urlList = new ArrayList<URL>();
		urlList.add(classesRootPath.toUri().toURL());
		final String classPath = buildClassPath(javacDefinition.classpath, urlList);
		return new FileClassCompilerLoader(executorService, sourceRootPath, classesRootPath, classPath, urlList);
	}

	private final static String buildClassPath(Collection<String> classPath, Collection<URL> urlCollection)
					throws MalformedURLException, URISyntaxException {
		final List<String> classPathes = new ArrayList<>();

		URLClassLoader classLoader = (URLClassLoader) URLClassLoader.getSystemClassLoader();
		if (classLoader != null && classLoader.getURLs() != null) {
			for (URL url : classLoader.getURLs()) {
				String path = new File(url.toURI()).getAbsolutePath();
				classPathes.add(path);
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
		return StringUtils.join(classPathes, File.pathSeparator);
	}

	private synchronized void resetClassLoader(boolean closeOnly) throws IOException {
		if (classLoader != null) {
			classLoader.close();
			classLoader = null;
		}
		if (!closeOnly)
			classLoader = new URLClassLoader(sourceRootURLs);
	}

	private String getBaseName(File sourceFile) throws IOException {
		final String sourcePath = sourceFile.getAbsolutePath();
		if (!sourcePath.startsWith(sourceRootPrefix))
			throw new IOException("The file is not in the source root: " + sourceFile + " / " + sourceRootPrefix);
		final String baseName = FilenameUtils.removeExtension(sourcePath.substring(sourceRootPrefixLength));
		final String className = StringUtils.join(StringUtils.split(baseName, File.separator), '.');
		return className;
	}

	public <T> Class<T> loadClass(File sourceFile) throws IOException, ReflectiveOperationException {

		final long sourceFileLastModified = sourceFile.lastModified();
		mapRwl.r.lock();
		try {
			Long time = lastModifiedMap.get(sourceFile);
			if (time != null && time == sourceFileLastModified)
				return (Class<T>) classLoader.loadClass(getBaseName(sourceFile));
		} finally {
			mapRwl.r.unlock();
		}

		mapRwl.w.lock();
		try {
			Long time = lastModifiedMap.get(sourceFile);
			if (time != null && time == sourceFileLastModified)
				return (Class<T>) classLoader.loadClass(getBaseName(sourceFile));
			compile(sourceFile);
			currentVersion = UUIDs.timeBased();
			lastModifiedMap.put(sourceFile, sourceFileLastModified);
			resetClassLoader(false);
			return (Class<T>) classLoader.loadClass(getBaseName(sourceFile));
		} finally {
			mapRwl.w.unlock();
		}
	}

	public UUID getCurrentVersion() {
		return currentVersion;
	}

	private void compile(File sourceFile) throws IOException {
		if (logger.isInfoEnabled())
			logger.info("Recompile " + sourceFile);
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
			if (classesRootFile != null) {
				options.add("-d");
				options.add(classesRootFile.getAbsolutePath());
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
		IOUtils.closeQuietly(directorWatcher);
		resetClassLoader(true);
	}

	public final static <T> Class<T> findClass(String[] classPrefixes, String suffix) throws ClassNotFoundException {
		ClassNotFoundException firstClassException = null;
		for (String prefix : classPrefixes) {
			try {
				return (Class<T>) Class.forName(prefix + suffix);
			} catch (ClassNotFoundException e) {
				if (firstClassException == null)
					firstClassException = e;
			}
		}
		throw firstClassException;
	}

	public final static <T> Class<T> findClass(FileClassCompilerLoader compilerLoader, String classDef,
					String[] classPrefixes) throws ReflectiveOperationException, IOException {
		if (compilerLoader != null && classDef.endsWith(".java"))
			return compilerLoader.loadClass(new File(classDef));
		if (classPrefixes == null)
			return (Class<T>) Class.forName(classDef);
		return (Class<T>) findClass(classPrefixes, classDef);
	}
}
