/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.store.store;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.qwazr.store.StoreServer;

public class StoreManager {

	public static volatile StoreManager INSTANCE = null;

	public final File rootDir;

	public static void load(File directory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StoreManager(new File(directory,
				StoreServer.SERVICE_NAME_STORE));
	}

	private StoreManager(File rootDir) throws IOException {
		this.rootDir = rootDir;
	}

	public final File getRootDir() {
		return rootDir;
	}

	/**
	 * Get a File with a path relative to the ROOT_DIR. This API checks that the
	 * file is a child of the ROOT_DIR
	 * 
	 * @param relativePath
	 *            a relative path
	 * @return a file relative to the ROOT_DIR
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	public final File getFile(String relativePath) throws IOException {
		if (StringUtils.isEmpty(relativePath) || relativePath.equals("/"))
			return rootDir;
		File finalFile = new File(rootDir, relativePath);
		File file = finalFile;
		while (file != null) {
			if (file.equals(rootDir))
				return finalFile;
			file = file.getParentFile();
		}
		throw new IOException("Permission denied.");
	}
}
