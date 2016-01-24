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

import java.io.IOException;

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
}
