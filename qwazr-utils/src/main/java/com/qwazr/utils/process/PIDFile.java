/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils.process;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class PIDFile {

	private final Integer pid;
	private final File pidFile;

	public PIDFile() throws IOException {
		pid = PIDFile.getPid();
		pidFile = PIDFile.getPidFile();
	}

	/**
	 * @return true if a pid file exists
	 */
	public boolean isFileExists() {
		if (pidFile == null)
			return false;
		return pidFile.exists();
	}

	/**
	 * Save the PID number in the PID file
	 *
	 * @return this instance
	 * @throws IOException
	 */
	public PIDFile savePidToFile() throws IOException {
		FileOutputStream fos = new FileOutputStream(pidFile);
		try {
			IOUtils.write(pid.toString(), fos);
			return this;
		} finally {
			fos.close();
		}
	}

	/**
	 * Delete the PID file on exit
	 *
	 * @return this instance
	 */
	public PIDFile deletePidFileOnExit() {
		if (pidFile != null)
			pidFile.deleteOnExit();
		return this;
	}


	/**
	 * Define the location of the PID File using:
	 * - First the JAVA property: "com.qwazr.pid.path"
	 * - Second the Environment Variable QWAZR_PID_PATH
	 *
	 * @return a file instance where to store the PID
	 */
	public static File getPidFile() {
		String pid_path = System.getProperty("com.qwazr.pid.path");
		if (pid_path == null)
			pid_path = System.getenv("QWAZR_PID_PATH");
		if (pid_path == null)
			return null;
		return new File(pid_path);
	}

	/**
	 * Try to locate the PID of the process:
	 * - First by checking the JAVA property "com.qwazr.pid".
	 * - Second by checking the Environment Variable QWAZR_PID.
	 * - Finally by using the RuntimeMXBean method.
	 *
	 * @return the PID of the process
	 */
	public static Integer getPid() {
		String pid = System.getProperty("com.qwazr.pid");
		if (!StringUtils.isEmpty(pid))
			pid = System.getenv("QWAZR_PID");
		if (!StringUtils.isEmpty(pid))
			return Integer.parseInt(pid);
		return getPidFromMxBean();
	}

	/**
	 * @return the PID using the RuntimeMXBean method
	 */
	public static Integer getPidFromMxBean() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if (name == null)
			return null;
		int i = name.indexOf('@');
		if (i == -1)
			return null;
		return Integer.parseInt(name.substring(0, i));
	}

	@Override
	public String toString() {
		return "PID: " + pid + " - File: " + pidFile;
	}
}
