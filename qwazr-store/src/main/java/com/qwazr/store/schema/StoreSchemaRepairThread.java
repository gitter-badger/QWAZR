package com.qwazr.store.schema;


public class StoreSchemaRepairThread {

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
	// public void repairDirectory(ScriptMultiClient client,
	// TreeMap<String, StoreFileResult> globalFiles) throws IOException,
	// ServerException {
	// if (globalFiles == null)
	// return;
	// int size = client.size() + 1;
	// Set<String> repairSet = new HashSet<String>();
	// for (Map.Entry<String, StoreFileResult> entry : globalFiles.entrySet()) {
	//
	// String scriptName = entry.getKey();
	// StoreFileResult fileStatus = entry.getValue();
	//
	// // Check if this file should be synchronized with any node
	// if (!fileStatus.isRepairRequired(size))
	// continue;
	//
	// // Find the leading version of the script file
	// Map.Entry<String, ScriptFileStatus> leadEntry = fileStatus
	// .findLead();
	// // Lead entry can be null if a file is empty (length == 0)
	// if (leadEntry == null)
	// continue;
	// String leadNode = leadEntry.getKey();
	// ScriptFileStatus leadFileStatus = leadEntry.getValue();
	//
	// // Find which node must be repaired
	// repairSet.clear();
	// repairSet.add(ClusterManager.INSTANCE.myAddress);
	// client.fillClientUrls(repairSet);
	// fileStatus.buildRepairSet(leadFileStatus, repairSet);
	//
	// // Read the script content
	// String content;
	// if (leadNode.equals(ClusterManager.INSTANCE.myAddress))
	// content = getScript(scriptName);
	// else
	// content = client.getClientByUrl(leadNode).getScript(scriptName);
	//
	// // Write the script content to the node to repair
	// for (String repair : repairSet) {
	// if (repair.equals(ClusterManager.INSTANCE.myAddress))
	// setScript(scriptName,
	// leadFileStatus.last_modified.getTime(), content);
	// else
	// client.getClientByUrl(repair).setScript(scriptName,
	// leadFileStatus.last_modified.getTime(), true,
	// content);
	// }
	//
	// }
	// }

	/*
	 * Check if this item should be repaired
	 * 
	 * @param size the expected number of nodes
	 * 
	 * @return false if not repair is required
	 */
	// boolean isRepairRequired(int size) {
	// if (size != nodes.size())
	// return true;
	// Iterator<ScriptFileStatus> iterator = nodes.values().iterator();
	//
	// ScriptFileStatus fileStatus = iterator.next();
	// while (iterator.hasNext()) {
	// if (!fileStatus.equals(iterator.next()))
	// return true;
	// }
	// return false;
	// }
	//
	// boolean equals(ScriptFileStatus fileStatus) {
	// if (fileStatus.size == null || this.size == null)
	// return false;
	// if (fileStatus.last_modified == null || this.last_modified == null)
	// return false;
	// return size.equals(fileStatus.size)
	// && last_modified.equals(last_modified);
	// }

	/**
	 * @return the map entry which represents the last version of the file
	 */
	// static Map.Entry<String, ScriptFileStatus> findLead() {
	// long last_modified = 0;
	// Map.Entry<String, ScriptFileStatus> lead = null;
	// for (Map.Entry<String, ScriptFileStatus> entry : nodes.entrySet()) {
	// ScriptFileStatus fileStatus = entry.getValue();
	// if (fileStatus.last_modified == null || fileStatus.size == null
	// || fileStatus.size == 0)
	// continue;
	// if (lead == null
	// || (fileStatus.last_modified.getTime() > last_modified)) {
	// last_modified = fileStatus.last_modified.getTime();
	// lead = entry;
	// continue;
	// }
	// }
	// return lead;
	// }

	/**
	 * @param leadFileStatus
	 *            the leading file
	 * @param repairSet
	 *            a set of nodes which do not have the last version
	 */
	// static void buildRepairSet(ScriptFileStatus leadFileStatus,
	// Set<String> repairSet) {
	// for (Map.Entry<String, ScriptFileStatus> entry : nodes.entrySet())
	// if (leadFileStatus.equals(entry.getValue()))
	// repairSet.remove(entry.getKey());
	// }
}
