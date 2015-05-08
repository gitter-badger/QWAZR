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
package com.qwazr.store;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.qwazr.utils.server.ServerException;

public class StoreDataManager {

	public static volatile StoreDataManager INSTANCE = null;

	public static void load(File storeDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StoreDataManager(storeDirectory);
	}

	public final Map<String, File> schemaDataDirectoryMap;

	private StoreDataManager(File storeDirectory) {
		this.schemaDataDirectoryMap = new ConcurrentHashMap<String, File>();
		File[] schemaFiles = storeDirectory
				.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
		for (File schemaFile : schemaFiles)
			INSTANCE.addSchema(schemaFile);
	}

	final void addSchema(File schemaFile) {
		File dataFile = new File(schemaFile, "data");
		if (!dataFile.exists())
			dataFile.mkdir();
		schemaDataDirectoryMap.put(schemaFile.getName(), dataFile);
	}

	/**
	 * Get a File with a path relative to the schema directory. This method also
	 * checks that the resolved path is a child of the schema directory
	 * 
	 * @param relativePath
	 *            a relative path
	 * @return a file relative to the ROOT_DIR
	 * @throws ServerException
	 *             if the schema does not exists, or if there is a permission
	 *             issue
	 */
	final File getFile(String schema, String relativePath)
			throws ServerException {
		File schemaDataDir = schemaDataDirectoryMap.get(schema);
		if (schemaDataDir == null)
			throw new ServerException(Status.NOT_FOUND, "Schema not found: "
					+ schema);
		if (StringUtils.isEmpty(relativePath) || relativePath.equals("/"))
			return schemaDataDir;
		File finalFile = new File(schemaDataDir, relativePath);
		File file = finalFile;
		while (file != null) {
			if (file.equals(schemaDataDir))
				return finalFile;
			file = file.getParentFile();
		}
		throw new ServerException(Status.FORBIDDEN, "Permission denied.");
	}
}
