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
package com.qwazr.compiler;

import com.qwazr.utils.DirectoryWatcher;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class JavaCompiler implements Closeable {

	private final static Logger logger = LoggerFactory.getLogger(JavaCompiler.class);

	private final int javaSourcePrefixSize;
	private final File javaSourceDirectory;
	private final File javaClassesDirectory;
	private final String classPath;

	private final LockUtils.ReadWriteLock compilerLock;

	private final DirectoryWatcher directorWatcher;

	private JavaCompiler(ExecutorService executorService, File javaSourceDirectory, File javaClassesDirectory,
					String classPath, Collection<URL> urlList) throws IOException {
		this.classPath = classPath;
		this.javaSourceDirectory = javaSourceDirectory;
		String javaSourcePrefix = javaSourceDirectory.getAbsolutePath();
		javaSourcePrefixSize = javaSourcePrefix.endsWith("/") ?
						javaSourcePrefix.length() :
						javaSourcePrefix.length() + 1;
		this.javaClassesDirectory = javaClassesDirectory;
		if (this.javaClassesDirectory != null && !this.javaClassesDirectory.exists())
			this.javaClassesDirectory.mkdir();
		compilerLock = new LockUtils.ReadWriteLock();
		compileDirectory(javaSourceDirectory);
		directorWatcher = DirectoryWatcher.register(javaSourceDirectory.toPath(), new Consumer<Path>() {
			@Override
			public void accept(Path path) {
				try {
					compileDirectory(path.toFile());
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
		executorService.execute(directorWatcher);
	}

	@Override
	public void close() {
		IOUtils.close(directorWatcher);
	}

	static JavaCompiler newInstance(ExecutorService executorService, File javaSourceDirectory,
					File javaClassesDirectory, File... classPathDirectories) throws IOException, URISyntaxException {
		Objects.requireNonNull(javaSourceDirectory, "No source directory given (null)");
		Objects.requireNonNull(javaClassesDirectory, "No class directory given (null)");
		final FileSystem fs = FileSystems.getDefault();
		final List<URL> urlList = new ArrayList<URL>();
		urlList.add(javaClassesDirectory.toURI().toURL());
		final String classPath = buildClassPath(classPathDirectories, urlList);
		return new JavaCompiler(executorService, javaSourceDirectory, javaClassesDirectory, classPath, urlList);
	}

	private final static String buildClassPath(File[] classPathArray, Collection<URL> urlCollection)
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

		if (classPathArray != null) {
			for (File classPathFile : classPathArray) {
				if (classPathFile.isDirectory()) {
					for (File f : classPathFile.listFiles((FileFilter) FileFileFilter.FILE)) {
						classPathes.add(f.getAbsolutePath());
						urlCollection.add(f.toURI().toURL());
					}
				} else if (classPathFile.isFile()) {
					classPathes.add(classPathFile.getAbsolutePath());
					urlCollection.add(classPathFile.toURI().toURL());
				}
			}
		}
		if (classPathes.isEmpty())
			return null;
		return StringUtils.join(classPathes, File.pathSeparator);
	}

	private void compile(javax.tools.JavaCompiler compiler, Collection<File> javaFiles) throws IOException {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		try {
			Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			final List<String> options = new ArrayList<String>();
			if (classPath != null) {
				options.add("-classpath");
				options.add(classPath);
			}
			options.add("-d");
			options.add(javaClassesDirectory.getAbsolutePath());
			options.add("-sourcepath");
			options.add(javaSourceDirectory.getAbsolutePath());
			javax.tools.JavaCompiler.CompilationTask task = compiler
							.getTask(pw, fileManager, diagnostics, options, null, sourceFileObjects);
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
			IOUtils.close(fileManager);
		}
	}

	private Collection<File> filterUptodate(File parentDir, File[] javaSourceFiles) {
		if (javaSourceFiles == null)
			return null;
		final Collection<File> finalJavaFiles = new ArrayList<File>();
		if (javaSourceFiles.length == 0)
			return finalJavaFiles;
		final File parentClassDir = new File(javaClassesDirectory,
						parentDir.getAbsolutePath().substring(javaSourcePrefixSize));
		for (File javaSourceFile : javaSourceFiles) {
			final File classFile = new File(parentClassDir,
							FilenameUtils.removeExtension(javaSourceFile.getName()) + ".class");
			if (classFile.exists() && classFile.lastModified() > javaSourceFile.lastModified())
				continue;
			finalJavaFiles.add(javaSourceFile);
		}
		return finalJavaFiles;
	}

	private void compileDirectory(javax.tools.JavaCompiler compiler, File sourceDirectory) throws IOException {
		final Collection<File> javaFiles = filterUptodate(sourceDirectory, sourceDirectory.listFiles(javaFileFilter));
		if (javaFiles != null && javaFiles.size() > 0) {
			if (logger.isInfoEnabled())
				logger.info("Compile " + javaFiles.size() + " JAVA file(s) at " + sourceDirectory);
			compile(compiler, javaFiles);
		}
		for (File dir : sourceDirectory.listFiles((FileFilter) DirectoryFileFilter.INSTANCE))
			compileDirectory(compiler, dir);
	}

	private void compileDirectory(File sourceDirectory) throws IOException {
		if (sourceDirectory == null)
			return;
		if (!sourceDirectory.isDirectory())
			return;
		compilerLock.w.lock();
		try {
			javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			Objects.requireNonNull(compiler, "No compiler is available. This feature requires a JDK (not a JRE).");
			compileDirectory(compiler, sourceDirectory);
		} finally {
			compilerLock.w.unlock();
		}
	}

	private final JavaFileFilter javaFileFilter = new JavaFileFilter();

	private class JavaFileFilter implements FileFilter {

		@Override
		final public boolean accept(File file) {
			if (!file.isFile())
				return false;
			return file.getName().endsWith(".java");
		}
	}
}
