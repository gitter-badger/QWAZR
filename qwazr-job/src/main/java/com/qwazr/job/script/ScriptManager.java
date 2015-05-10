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
 **/
package com.qwazr.job.script;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.job.JobServer;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.LockUtils.ReadWriteLock;
import com.qwazr.utils.server.ServerException;

public class ScriptManager {

	private static final Logger logger = LoggerFactory
			.getLogger(ScriptManager.class);

	public static volatile ScriptManager INSTANCE = null;

	public static void load(File directory) throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		try {
			INSTANCE = new ScriptManager(directory);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private final File scriptDirectory;
	private final ScriptEngine scriptEngine;

	private final ReadWriteLock runsMapLock = new ReadWriteLock();
	private final HashMap<String, HashMap<String, ScriptRunThread>> runsMap;
	private final ExecutorService executorService;

	private ScriptManager(File rootDirectory) throws IOException,
			URISyntaxException {

		// Load Nashorn
		ScriptEngineManager manager = new ScriptEngineManager();
		scriptEngine = manager.getEngineByName("nashorn");
		scriptDirectory = new File(rootDirectory, JobServer.SERVICE_NAME_SCRIPT);
		if (!scriptDirectory.exists())
			scriptDirectory.mkdir();

		runsMap = new HashMap<String, HashMap<String, ScriptRunThread>>();
		executorService = Executors.newFixedThreadPool(100);
	}

	public TreeMap<String, ScriptFileStatus> getScripts() {
		TreeMap<String, ScriptFileStatus> map = new TreeMap<String, ScriptFileStatus>();
		File[] files = scriptDirectory
				.listFiles((FileFilter) FileFileFilter.FILE);
		if (files == null)
			return map;
		for (File file : files)
			if (!file.isHidden())
				map.put(file.getName(), new ScriptFileStatus(file));
		return map;
	}

	private File getScriptFile(String script_name) throws ServerException {
		if (StringUtils.isEmpty(script_name))
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"No script name given");
		File scriptFile = new File(scriptDirectory, script_name);
		if (!scriptFile.exists())
			throw new ServerException(Status.NOT_FOUND, "Script not found: "
					+ script_name);
		if (!scriptFile.isFile())
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"Script is not a file: " + script_name);
		return scriptFile;
	}

	public String getScript(String script_name) throws IOException,
			ServerException {
		File scriptFile = getScriptFile(script_name);
		FileReader fileReader = new FileReader(scriptFile);
		try {
			return IOUtils.toString(fileReader);
		} finally {
			if (fileReader != null)
				IOUtils.closeQuietly(fileReader);
		}
	}

	public void deleteScript(String script_name) throws ServerException {
		getScriptFile(script_name).delete();
	}

	public long setScript(String script_name, Long last_modified, String script)
			throws IOException {
		File scriptFile = new File(scriptDirectory, script_name);
		FileWriter fileWriter = new FileWriter(scriptFile);
		try {
			fileWriter.write(script);
			IOUtils.closeQuietly(fileWriter);
			fileWriter = null;
			if (last_modified != null)
				scriptFile.setLastModified(last_modified);
			return scriptFile.lastModified();
		} finally {
			if (fileWriter != null)
				IOUtils.closeQuietly(fileWriter);
		}
	}

	private ScriptRunThread getNewScriptRunThread(String script_name,
			Map<String, ? extends Object> objects) throws ServerException {
		ScriptRunThread scriptRunThread = new ScriptRunThread(scriptEngine,
				getScriptFile(script_name), objects,
				ConnectorManager.INSTANCE.getReadOnlyMap(),
				ToolsManager.INSTANCE.getReadOnlyMap());
		addScriptRunThread(script_name, scriptRunThread);
		return scriptRunThread;
	}

	public ScriptRunThread runSync(String script_name, Map<String, ?> objects)
			throws ServerException {
		logger.info("Run sync: " + script_name);
		ScriptRunThread scriptRunThread = getNewScriptRunThread(script_name,
				objects);
		scriptRunThread.run();
		expireScriptRunThread(script_name);
		return scriptRunThread;
	}

	public ScriptRunStatus runAsync(String script_name,
			Map<String, ? extends Object> objects) throws ServerException {
		logger.info("Run async: " + script_name);
		ScriptRunThread scriptRunThread = getNewScriptRunThread(script_name,
				objects);
		executorService.execute(scriptRunThread);
		expireScriptRunThread(script_name);
		return scriptRunThread.getStatus();
	}

	private void addScriptRunThread(String script_name,
			ScriptRunThread scriptRunThread) {
		if (scriptRunThread == null)
			return;
		runsMapLock.w.lock();
		try {
			HashMap<String, ScriptRunThread> scriptRunThreadMap = runsMap
					.get(script_name);
			if (scriptRunThreadMap == null) {
				scriptRunThreadMap = new HashMap<String, ScriptRunThread>();
				runsMap.put(script_name, scriptRunThreadMap);
			}
			scriptRunThreadMap.put(scriptRunThread.getUUID().toString(),
					scriptRunThread);
		} finally {
			runsMapLock.w.unlock();
		}
	}

	private void expireScriptRunThread(String script_name) {
		runsMapLock.w.lock();
		try {
			HashMap<String, ScriptRunThread> scriptRunThreadMap = runsMap
					.get(script_name);
			if (scriptRunThreadMap == null)
				return;
			List<String> uuidsToDelete = new ArrayList<String>();
			for (ScriptRunThread scriptRunThread : scriptRunThreadMap.values())
				if (scriptRunThread.hasExpired())
					uuidsToDelete.add(scriptRunThread.getUUID().toString());
			for (String uuid : uuidsToDelete)
				scriptRunThreadMap.remove(uuid);
			logger.info("Expire " + script_name + ": " + uuidsToDelete.size());
			if (scriptRunThreadMap.isEmpty())
				runsMap.remove(script_name);
		} finally {
			runsMapLock.w.unlock();
		}
	}

	public Map<String, ScriptRunStatus> getRunsStatus(String script_name) {
		runsMapLock.r.lock();
		try {
			Map<String, ScriptRunThread> runThreadMap = runsMap
					.get(script_name);
			if (runThreadMap == null)
				return null;
			LinkedHashMap<String, ScriptRunStatus> runStatusMap = new LinkedHashMap<String, ScriptRunStatus>();
			for (Map.Entry<String, ScriptRunThread> entry : runThreadMap
					.entrySet())
				runStatusMap.put(entry.getKey(), entry.getValue().getStatus());
			return runStatusMap;
		} finally {
			runsMapLock.r.unlock();
		}
	}

	public ScriptRunThread getRunThread(String script_name, String uuid) {
		runsMapLock.r.lock();
		try {
			Map<String, ScriptRunThread> runs = runsMap.get(script_name);
			if (runs == null)
				return null;
			return runs.get(uuid);
		} finally {
			runsMapLock.r.unlock();
		}
	}

	/**
	 * Copy the missing files on the nodes
	 * 
	 * @param client
	 *            the current client
	 * @param globalFiles
	 *            a map with the files of all the cluster
	 * @throws ServerException
	 *             if any server exception occurs
	 * @throws IOException
	 *             if any I/O exception occurs
	 */
	public void repair(ScriptMultiClient client,
			TreeMap<String, ScriptFileStatus> globalFiles) throws IOException,
			ServerException {
		if (globalFiles == null)
			return;
		int size = client.size() + 1;
		Set<String> repairSet = new HashSet<String>();
		for (Map.Entry<String, ScriptFileStatus> entry : globalFiles.entrySet()) {

			String scriptName = entry.getKey();
			ScriptFileStatus fileStatus = entry.getValue();

			// Check if this file should be synchronized with any node
			if (!fileStatus.isRepairRequired(size))
				continue;

			// Find the leading version of the script file
			Map.Entry<String, ScriptFileStatus> leadEntry = fileStatus
					.findLead();
			// Lead entry can be null if a file is empty (length == 0)
			if (leadEntry == null)
				continue;
			String leadNode = leadEntry.getKey();
			ScriptFileStatus leadFileStatus = leadEntry.getValue();

			// Find which node must be repaired
			repairSet.clear();
			repairSet.add(ClusterManager.INSTANCE.myAddress);
			client.fillClientUrls(repairSet);
			fileStatus.buildRepairSet(leadFileStatus, repairSet);

			// Read the script content
			String content;
			if (leadNode.equals(ClusterManager.INSTANCE.myAddress))
				content = getScript(scriptName);
			else
				content = client.getClientByUrl(leadNode).getScript(scriptName);

			// Write the script content to the node to repair
			for (String repair : repairSet) {
				if (repair.equals(ClusterManager.INSTANCE.myAddress))
					setScript(scriptName,
							leadFileStatus.last_modified.getTime(), content);
				else
					client.getClientByUrl(repair).setScript(scriptName,
							leadFileStatus.last_modified.getTime(), true,
							content);
			}

		}
	}

	public ScriptMultiClient getNewClient() throws URISyntaxException {
		// pass executor
		return new ScriptMultiClient(null, ClusterManager.INSTANCE
				.getClusterClient().getActiveNodes(
						JobServer.SERVICE_NAME_SCRIPT), 60000);
	}
}
