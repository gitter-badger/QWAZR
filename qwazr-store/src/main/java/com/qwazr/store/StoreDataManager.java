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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.server.ServerException;

class StoreDataManager {

	public static volatile StoreDataManager INSTANCE = null;

	public static void load(File storeDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StoreDataManager(storeDirectory);
	}

	private final LockUtils.ReadWriteLock rwlSchemas = new LockUtils.ReadWriteLock();
	private final Map<String, File> schemaDataDirectoryMap;
	private final File storeDirectory;

	private StoreDataManager(File storeDirectory) throws IOException {
		this.schemaDataDirectoryMap = new ConcurrentHashMap<String, File>();
		this.storeDirectory = storeDirectory;
		File[] schemaFiles = storeDirectory
				.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
		for (File schemaFile : schemaFiles)
			INSTANCE.addSchema(schemaFile);
	}

	private final void addSchema(File schemaFile) throws IOException {
		if (!schemaFile.exists()) {
			schemaFile.mkdir();
			if (!schemaFile.exists())
				throw new IOException("Unable to create the directory: "
						+ schemaFile.getAbsolutePath());
		}
		File dataFile = new File(schemaFile, "data");
		if (!dataFile.exists()) {
			dataFile.mkdir();
			if (!dataFile.exists())
				throw new IOException("Unable to create the directory: "
						+ schemaFile.getAbsolutePath());
		}
		schemaDataDirectoryMap.put(schemaFile.getName(), dataFile);
	}

	public void createSchema(String schemaName) throws IOException {
		rwlSchemas.r.lock();
		try {
			if (schemaDataDirectoryMap.containsKey(schemaName))
				return;
		} finally {
			rwlSchemas.r.unlock();
		}
		rwlSchemas.w.lock();
		try {
			if (schemaDataDirectoryMap.containsKey(schemaName))
				return;
			addSchema(new File(storeDirectory, schemaName));
		} finally {
			rwlSchemas.w.unlock();
		}
	}

	public void deleteSchema(String schemaName) throws IOException {
		rwlSchemas.r.lock();
		try {
			if (!schemaDataDirectoryMap.containsKey(schemaName))
				return;
		} finally {
			rwlSchemas.r.unlock();
		}
		rwlSchemas.w.lock();
		try {
			File schemaFile = schemaDataDirectoryMap.remove(schemaName);
			if (schemaFile == null)
				return;
			FileUtils.deleteDirectory(schemaFile);
		} finally {
			rwlSchemas.w.unlock();
		}
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
		File schemaDataDir;
		rwlSchemas.r.lock();
		try {
			schemaDataDir = schemaDataDirectoryMap.get(schema);
			if (schemaDataDir == null)
				throw new ServerException(Status.NOT_FOUND,
						"Schema not found: " + schema);
		} finally {
			rwlSchemas.r.unlock();
		}
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
