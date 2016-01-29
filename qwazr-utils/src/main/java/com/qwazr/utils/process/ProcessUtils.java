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
 */
package com.qwazr.utils.process;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ProcessUtils {

	public final static String NOT_SUPPORTED_ERROR = "Process command unsupported on this operating system";

	public static Integer kill(Number pid) throws IOException, InterruptedException {
		if (pid == null)
			return null;
		final String commandLine;
		if (SystemUtils.IS_OS_UNIX)
			commandLine = "kill  " + pid;
		else if (SystemUtils.IS_OS_WINDOWS)
			commandLine = "taskkill /PID " + pid;
		else
			throw new IOException(NOT_SUPPORTED_ERROR);
		return run(commandLine);
	}

	public static Integer forceKill(Number pid) throws IOException, InterruptedException {
		if (pid == null)
			return null;
		final String commandLine;
		if (SystemUtils.IS_OS_UNIX)
			commandLine = "kill -9 " + pid;
		else if (SystemUtils.IS_OS_WINDOWS)
			commandLine = "taskkill /F /PID " + pid;
		else
			throw new IOException(NOT_SUPPORTED_ERROR);
		return run(commandLine);
	}

	public static Boolean isRunning(Number pid) throws IOException, InterruptedException {
		if (pid == null)
			return null;
		final String commandLine;
		if (SystemUtils.IS_OS_UNIX)
			commandLine = "kill -0 " + pid;
		else
			throw new IOException(NOT_SUPPORTED_ERROR);
		Integer res = run(commandLine);
		if (res == null)
			return null;
		return res == 0;
	}

	public static Integer run(String commandLine) throws InterruptedException, IOException {
		Process process = Runtime.getRuntime().exec(commandLine);
		try {
			return process.waitFor();
		} finally {
			process.destroy();
			if (process.isAlive())
				process.destroyForcibly();
		}
	}

	public static class Restart implements Runnable {

		private final Class<?> mainClass;
		private final String[] args;

		public static Restart INSTANCE;

		public static synchronized void init(Class<?> mainClass, String[] args) {
			INSTANCE = new Restart(mainClass, args);
		}

		public static synchronized void restart() {
			Objects.requireNonNull(INSTANCE, "The restart object is not initialized" );
			Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE));
			System.exit(2);
		}

		private Restart(Class<?> mainClass, String[] args) {
			this.mainClass = mainClass;
			this.args = args;
		}

		private void start() throws URISyntaxException, IOException {

			List<String> arguments = new ArrayList<>();

			String path = System.getProperty("java.home");
			if (path == null)
				path = "java";
			else
				path = path + File.separator + "bin" + File.separator + "java";
			arguments.add(path);

			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

			Collection<String> cmdLineArgs = runtimeMXBean.getInputArguments();
			if (cmdLineArgs != null)
				arguments.addAll(cmdLineArgs);

			String classpath = runtimeMXBean.getClassPath();
			if (classpath != null) {
				arguments.add("-cp");
				arguments.add(classpath);
			}

			final File currentJar = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (currentJar.getName().endsWith(".jar")) {
				arguments.add("-jar");
				arguments.add(currentJar.getPath());
			} else
				arguments.add(mainClass.getName());

			if (args != null)
				for (String arg : args)
					arguments.add(arg);

			System.out.println("Restarting... " + arguments);

			ProcessBuilder pb = new ProcessBuilder(arguments);
			pb.inheritIO();
			pb.start();
		}

		public void run() {
			try {
				start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
