/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.store.data;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.qwazr.store.data.StoreDataSingleClient.PrefixPath;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.server.ServerException;

public class StoreDataManager {

	public static volatile StoreDataManager INSTANCE = null;

	public static void load(File storeDirectory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		INSTANCE = new StoreDataManager(storeDirectory);
	}

	private final LockUtils.ReadWriteLock rwlSchemas = new LockUtils.ReadWriteLock();
	private final Map<String, File> schemaDataDirectoryMap;
	private final File storeDirectory;
	private final ExecutorService executor;

	private StoreDataManager(File storeDirectory) throws IOException {
		executor = Executors.newFixedThreadPool(8);
		this.schemaDataDirectoryMap = new ConcurrentHashMap<String, File>();
		this.storeDirectory = storeDirectory;
		File[] schemaFiles = storeDirectory
				.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
		for (File schemaFile : schemaFiles)
			addSchema(schemaFile);
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

	void createSchema(String schemaName) throws IOException {
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

	void deleteSchema(String schemaName) throws IOException, ServerException {
		rwlSchemas.r.lock();
		try {
			if (!schemaDataDirectoryMap.containsKey(schemaName))
				throw new ServerException(Status.NOT_FOUND,
						"Schema not found: " + schemaName);
		} finally {
			rwlSchemas.r.unlock();
		}
		rwlSchemas.w.lock();
		try {
			File schemaFile = schemaDataDirectoryMap.remove(schemaName);
			if (schemaFile == null)
				throw new ServerException(Status.NOT_FOUND,
						"Schema not found: " + schemaName);
			FileUtils.deleteDirectory(schemaFile);
		} finally {
			rwlSchemas.w.unlock();
		}
	}

	private File getSchemaDataDir(String schemaName) throws ServerException {
		rwlSchemas.r.lock();
		try {
			File schemaDataDir = schemaDataDirectoryMap.get(schemaName);
			if (schemaDataDir != null)
				return schemaDataDir;
			throw new ServerException(Status.NOT_FOUND, "Schema not found: "
					+ schemaName);
		} finally {
			rwlSchemas.r.unlock();
		}
	}

	private File getFile(File schemaDataDir, String relativePath)
			throws ServerException {
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
		File schemaDataDir = getSchemaDataDir(schema);
		File file = getFile(schemaDataDir, relativePath);
		if (!file.exists())
			throw new ServerException(Status.NOT_FOUND, "File not found: "
					+ relativePath);
		return file;
	}

	final File putFile(String schema, String relativePath,
			InputStream inputStream, Long lastModified) throws ServerException,
			IOException {
		File schemaDataDir = getSchemaDataDir(schema);
		File file = getFile(schemaDataDir, relativePath);
		if (file.exists() && file.isDirectory())
			throw new ServerException(Status.CONFLICT,
					"Error. A directory already exists: " + relativePath);
		File tmpFile = null;
		try {
			tmpFile = IOUtils.storeAsTempFile(inputStream);
			if (lastModified != null)
				tmpFile.setLastModified(lastModified);
			File parent = file.getParentFile();
			if (!parent.exists())
				parent.mkdir();
			tmpFile.renameTo(file);
			tmpFile = null;
			return file;
		} finally {
			if (tmpFile != null)
				tmpFile.delete();
		}
	}

	/**
	 * Delete the file, and prune the parent directory if empty.
	 * 
	 * @param schema
	 *            the name of the schema
	 * @param relativePath
	 *            the path of the file, relative to the schema
	 * @return the file instance of the deleted file
	 * @throws ServerException
	 *             is thrown is the file does not exists or if deleting the file
	 *             was not possible
	 */
	final File deleteFile(String schema, String relativePath)
			throws ServerException {
		File schemaDataDir = getSchemaDataDir(schema);
		File file = getFile(schemaDataDir, relativePath);
		if (!file.exists())
			throw new ServerException(Status.NOT_FOUND, "File not found: "
					+ relativePath);
		file.delete();
		if (file.exists())
			throw new ServerException(Status.INTERNAL_SERVER_ERROR,
					"Unable to delete the file: " + relativePath);
		File parent = file.getParentFile();
		if (parent.equals(schemaDataDir))
			return file;
		if (parent.list().length == 0)
			parent.delete();
		return file;
	}

	public StoreDataReplicationClient getNewDataClient(String[][] nodes,
			Integer msTimeOut) throws URISyntaxException {
		return new StoreDataReplicationClient(executor, nodes, PrefixPath.data,
				msTimeOut == null ? 60000 : msTimeOut);
	}
}
