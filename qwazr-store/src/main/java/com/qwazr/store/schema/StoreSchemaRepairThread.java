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
package com.qwazr.store.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.store.data.StoreDataManager;
import com.qwazr.store.data.StoreDataReplicationClient;
import com.qwazr.store.data.StoreDataSingleClient;
import com.qwazr.store.data.StoreDataSingleClient.PrefixPath;
import com.qwazr.store.data.StoreFileResult;
import com.qwazr.store.data.StoreMasterDataService;
import com.qwazr.utils.IOUtils;

public class StoreSchemaRepairThread extends Thread {

	private static final Logger logger = LoggerFactory
			.getLogger(StoreSchemaRepairThread.class);

	private final String schemaName;
	private final Integer msTimeout;
	private final AtomicBoolean abort;
	private final Date startTime;
	private volatile Date endTime;
	private String currentPath;
	private final AtomicInteger checkedDirectories;
	private final AtomicInteger checkedFiles;
	private final AtomicInteger repairedFiles;
	private final AtomicInteger errorFiles;
	private volatile Exception error;
	private final StoreMasterDataService masterDataService;

	StoreSchemaRepairThread(String schemaName, Integer msTimeout) {
		super("Schema repair " + schemaName);
		setDaemon(true);
		this.schemaName = schemaName;
		this.msTimeout = msTimeout;
		startTime = new Date();
		endTime = null;
		abort = new AtomicBoolean();
		error = null;
		checkedDirectories = new AtomicInteger();
		checkedFiles = new AtomicInteger();
		repairedFiles = new AtomicInteger();
		errorFiles = new AtomicInteger();
		currentPath = null;
		masterDataService = new StoreMasterDataService();
		start();
	}

	void abort() {
		abort.set(true);
	}

	StoreSchemaRepairStatus getRepairStatus() {
		return new StoreSchemaRepairStatus(startTime, getState(), abort.get(),
				endTime, currentPath, checkedDirectories.get(),
				checkedFiles.get(), repairedFiles.get(), error);
	}

	@Override
	public void run() {
		try {
			logger.info("Repair starts - schema: " + schemaName);
			StoreSchemaDefinition schemaDefinition = StoreSchemaManager.INSTANCE
					.getNewSchemaClient(msTimeout).getSchema(schemaName, false,
							msTimeout);
			StoreDataReplicationClient dataClient = StoreDataManager.INSTANCE
					.getNewDataClient(schemaDefinition.nodes, msTimeout);
			checkDirectory(schemaDefinition.nodes, dataClient, "");
		} catch (Exception e) {
			error = e;
			logger.error(
					"Repair failed (" + schemaName + ") : " + e.getMessage(), e);
		} finally {
			endTime = new Date();
			logger.info("Repair ends - schema: " + schemaName);
		}
	}

	private void checkDirectory(String[][] dataNodes,
			StoreDataReplicationClient dataClient, String path) {
		if (abort.get())
			return;
		currentPath = path;
		StoreFileResult dirResult = dataClient.getDirectory(schemaName,
				currentPath, msTimeout);

		// Check the files first
		if (dirResult.files != null) {
			for (Map.Entry<String, Map<String, StoreFileResult>> entry : dirResult.files
					.entrySet())
				checkFile(dataNodes, dataClient, path + "/" + entry.getKey(),
						entry.getValue());
			currentPath = path;
		}

		// Check the directories
		if (dirResult.directories != null) {
			for (Map.Entry<String, StoreFileResult> entry : dirResult.directories
					.entrySet())
				checkDirectory(dataNodes, dataClient,
						path + "/" + entry.getKey());
			currentPath = path;
		}
		checkedDirectories.incrementAndGet();
	}

	/**
	 * Compare the files between the hosts
	 * 
	 * @param dataClient
	 *            the data client
	 * @param path
	 *            the path of the current file
	 * @param nodeMap
	 *            a map of the file on the nodes
	 */
	private void checkFile(String[][] dataNodes,
			StoreDataReplicationClient dataClient, String path,
			Map<String, StoreFileResult> nodeMap) {
		if (abort.get())
			return;
		this.currentPath = path;
		boolean needRepair = false;

		Map.Entry<String, StoreFileResult> leadNodeEntry = findLeadNode(nodeMap);
		if (leadNodeEntry == null) {
			logger.error("Unable to repair the file: No lead node for "
					+ schemaName + "/" + path);
			errorFiles.incrementAndGet();
			return;
		}

		// Check that we have one instance on each replicated group
		for (String[] replicatGroup : dataNodes) {
			int count = 0;
			for (String node : replicatGroup)
				if (nodeMap.containsKey(node))
					count++;
			if (count == 0)
				needRepair = true;
		}

		// Check that all item are identical
		Iterator<StoreFileResult> iterator = nodeMap.values().iterator();
		StoreFileResult fileResult = iterator.next();
		while (iterator.hasNext()) {
			if (!fileResult.repairCheckFileEquals(iterator.next())) {
				needRepair = true;
				break;
			}
		}

		// Do we need to repair ?
		if (needRepair) {
			try {
				repairFile(leadNodeEntry, path);
				repairedFiles.incrementAndGet();
			} catch (IOException | URISyntaxException e) {
				logger.error("Unable to repair the file: No lead node for "
						+ schemaName + "/" + path, e);
				errorFiles.incrementAndGet();
				return;
			}
		}

		checkedFiles.incrementAndGet();
	}

	/**
	 * Find the lead which contains the best version of the file
	 * 
	 * @return the address of the node
	 */
	static Map.Entry<String, StoreFileResult> findLeadNode(
			Map<String, StoreFileResult> nodeMap) {
		long last_modified = 0;
		Map.Entry<String, StoreFileResult> leadNode = null;
		for (Map.Entry<String, StoreFileResult> entry : nodeMap.entrySet()) {
			StoreFileResult fileStatus = entry.getValue();
			if (fileStatus.last_modified == null || fileStatus.size == null
					|| fileStatus.size == 0)
				continue;
			if (leadNode == null
					|| (fileStatus.last_modified.getTime() > last_modified)) {
				last_modified = fileStatus.last_modified.getTime();
				leadNode = entry;
				continue;
			}
		}
		return leadNode;
	}

	/**
	 * Copy the missing files instance on the nodes
	 * 
	 * @param leadNodeEntry
	 *            the entry of the node which contains the best version of the
	 *            file
	 * @param path
	 *            the path of the file to repair
	 * @throws URISyntaxException
	 *             thrown if the node address is not valid
	 * @throws IOException
	 *             if the response is wrong
	 */
	public void repairFile(Map.Entry<String, StoreFileResult> leadNodeEntry,
			String path) throws URISyntaxException, IOException {

		String leadNodeAddress = leadNodeEntry.getKey();
		logger.info("Repair file - schema: " + schemaName + " - path: " + path
				+ " - from: " + leadNodeAddress);

		// TODO A faster repair method would initiate a push from the lead to
		// the missing nodes

		// Retrieve the reference file as temp file
		StoreDataSingleClient dataClient = new StoreDataSingleClient(
				leadNodeAddress, PrefixPath.data, msTimeout);
		Response response = dataClient.getFile(schemaName, path, msTimeout);
		Object entity = response.getEntity();
		if (entity == null)
			throw new IOException("The response entity is empty");
		if (!(entity instanceof InputStream))
			throw new IOException(
					"The response does not contains an inputstream: "
							+ entity.getClass().getName());
		InputStream entityStream = (InputStream) entity;

		// Upload the file
		File tempFile = null;
		FileInputStream fis = null;
		try {
			tempFile = IOUtils.storeAsTempFile(entityStream);
			fis = new FileInputStream(tempFile);
			masterDataService.putFile(schemaName, path, fis,
					leadNodeEntry.getValue().last_modified.getTime(),
					msTimeout, null);
		} finally {
			IOUtils.closeQuietly(fis);
			if (tempFile != null)
				tempFile.delete();
			IOUtils.closeQuietly(entityStream);
		}
	}
}
