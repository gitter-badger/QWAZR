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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.cluster.manager.ClusterManager;

@JsonInclude(Include.NON_EMPTY)
class ScriptFileStatus {

	public final String _run;
	public final String _status;

	public Map<String, ScriptFileStatus> nodes;

	public final Long size;
	public final Date last_modified;

	public ScriptFileStatus() {
		_run = null;
		_status = null;
		nodes = null;
		size = null;
		last_modified = null;
	}

	ScriptFileStatus(File file) {
		this._run = ClusterManager.INSTANCE.myAddress + "/scripts/"
				+ file.getName() + "/run";
		this._status = ClusterManager.INSTANCE.myAddress + "/scripts/"
				+ file.getName() + "/status";
		this.nodes = null;
		this.size = file.length();
		this.last_modified = new Date(file.lastModified());
	}

	ScriptFileStatus add(String node_address, ScriptFileStatus fileStatus) {
		if (nodes == null)
			nodes = new HashMap<String, ScriptFileStatus>();
		nodes.put(node_address, fileStatus);
		return this;
	}

	/**
	 * We merge results from several nodes
	 * 
	 * @param globalFiles
	 *            the destination structure
	 * @param node_address
	 *            the node address of the new structure
	 * @param localFiles
	 *            the new structure to merge
	 */
	static void merge(TreeMap<String, ScriptFileStatus> globalFiles,
			String node_address, TreeMap<String, ScriptFileStatus> localFiles) {
		if (localFiles == null)
			return;
		for (Map.Entry<String, ScriptFileStatus> entry : localFiles.entrySet()) {
			ScriptFileStatus localFileStatus = entry.getValue();
			ScriptFileStatus globalFileStatus = globalFiles.get(entry.getKey());
			if (globalFileStatus == null) {
				globalFileStatus = new ScriptFileStatus();
				globalFiles.put(entry.getKey(), globalFileStatus);
			}
			if (node_address != null)
				globalFileStatus.add(node_address, localFileStatus);
			if (localFileStatus.nodes != null)
				for (Map.Entry<String, ScriptFileStatus> entry2 : localFileStatus.nodes
						.entrySet())
					globalFileStatus.add(entry2.getKey(), entry2.getValue());
		}
	}

	@JsonIgnore
	/**
	 * Check if this item should be repaired
	 * @param size the expected number of nodes
	 * @return false if not repair is required
	 */
	boolean isRepairRequired(int size) {
		if (size != nodes.size())
			return true;
		Iterator<ScriptFileStatus> iterator = nodes.values().iterator();

		ScriptFileStatus fileStatus = iterator.next();
		while (iterator.hasNext()) {
			if (!fileStatus.equals(iterator.next()))
				return true;
		}
		return false;
	}

	@JsonIgnore
	boolean equals(ScriptFileStatus fileStatus) {
		if (fileStatus.size == null || this.size == null)
			return false;
		if (fileStatus.last_modified == null || this.last_modified == null)
			return false;
		return size.equals(fileStatus.size)
				&& last_modified.equals(last_modified);
	}

	@JsonIgnore
	/**
	 * @return the map entry which represents the last version of the file
	 */
	Map.Entry<String, ScriptFileStatus> findLead() {
		long last_modified = 0;
		Map.Entry<String, ScriptFileStatus> lead = null;
		for (Map.Entry<String, ScriptFileStatus> entry : nodes.entrySet()) {
			ScriptFileStatus fileStatus = entry.getValue();
			if (fileStatus.last_modified == null || fileStatus.size == null
					|| fileStatus.size == 0)
				continue;
			if (lead == null
					|| (fileStatus.last_modified.getTime() > last_modified)) {
				last_modified = fileStatus.last_modified.getTime();
				lead = entry;
				continue;
			}
		}
		return lead;
	}

	@JsonIgnore
	/**
	 * @param leadFileStatus the leading file
	 * @param repairSet a set of nodes which do not have the last version
	 */
	void buildRepairSet(ScriptFileStatus leadFileStatus, Set<String> repairSet) {
		for (Map.Entry<String, ScriptFileStatus> entry : nodes.entrySet())
			if (leadFileStatus.equals(entry.getValue()))
				repairSet.remove(entry.getKey());
	}
}
