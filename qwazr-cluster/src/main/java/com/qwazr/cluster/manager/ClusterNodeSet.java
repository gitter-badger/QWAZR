/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.cluster.manager;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.StringUtils;

import java.util.HashMap;
import java.util.TreeSet;

public class ClusterNodeSet {

	private final LockUtils.ReadWriteLock readWriteLock = new LockUtils.ReadWriteLock();

	private volatile String electedLeader;

	private final HashMap<String, ClusterNode> activeMap;
	private final HashMap<String, ClusterNode> inactiveMap;

	public class Cache {

		final ClusterNode[] activeArray;
		final ClusterNode[] inactiveArray;
		public final String leader;

		private Cache(boolean electNewLeader) {
			this.activeArray = activeMap.values().toArray(new ClusterNode[activeMap.size()]);
			this.inactiveArray = inactiveMap.values().toArray(new ClusterNode[inactiveMap.size()]);
			this.leader = checkElectedLeader(electNewLeader, activeArray);
		}
	}

	private Cache cache;

	ClusterNodeSet() {
		cache = null;
		activeMap = new HashMap<String, ClusterNode>();
		inactiveMap = new HashMap<String, ClusterNode>();
		electedLeader = null;
	}

	/**
	 * Move the node to the active set
	 *
	 * @param node The cluster not to insert
	 */
	private void active(ClusterNode node) {
		// We check first if it is not already present in the right list
		readWriteLock.r.lock();
		try {
			if (activeMap.containsKey(node.address))
				return;
		} finally {
			readWriteLock.r.unlock();
		}
		readWriteLock.w.lock();
		try {
			inactiveMap.remove(node.address);
			activeMap.put(node.address, node);
			cache = new Cache(false);
		} finally {
			readWriteLock.w.unlock();
		}
	}

	/**
	 * Move the node to the inactive set
	 *
	 * @param node The cluster not to insert
	 */
	private void inactive(ClusterNode node) {
		// We check first if it is not already present in the right list
		readWriteLock.r.lock();
		try {
			if (inactiveMap.containsKey(node.address))
				return;
		} finally {
			readWriteLock.r.unlock();
		}
		readWriteLock.w.lock();
		try {
			activeMap.remove(node.address);
			inactiveMap.put(node.address, node);
			cache = new Cache(electedLeader == node.address);
		} finally {
			readWriteLock.w.unlock();
		}
	}

	private static String checkElectedLeader(String currentMaster, boolean force, ClusterNode[] activeArray) {
		if (!force && currentMaster != null)
			return currentMaster;
		if (activeArray == null || activeArray.length == 0)
			return StringUtils.EMPTY;
		if (activeArray.length == 1)
			return activeArray[0].address;
		TreeSet<String> set = new TreeSet<String>();
		for (ClusterNode node : activeArray)
			set.add(node.address);
		return set.first();
	}

	private String checkElectedLeader(boolean force, ClusterNode[] activeArray) {
		return checkElectedLeader(electedLeader, force, activeArray);
	}

	/**
	 * @param node The clusterNode to insert
	 */
	void insert(ClusterNode node) {
		if (node.isActive())
			active(node);
		else
			inactive(node);
	}

	/**
	 * @param node The ClusterNode to remove
	 */
	void remove(ClusterNode node) {
		readWriteLock.w.lock();
		try {
			activeMap.remove(node.address);
			inactiveMap.remove(node.address);
			cache = new Cache(electedLeader == node.address);
		} finally {
			readWriteLock.w.unlock();
		}
	}

	/**
	 * @return if the set is empty
	 */
	boolean isEmpty() {
		readWriteLock.r.lock();
		try {
			return activeMap.isEmpty() && inactiveMap.isEmpty();
		} finally {
			readWriteLock.r.unlock();
		}
	}

	/**
	 * @return a cached list of active nodes and inactive nodes
	 */
	Cache getCache() {
		return cache;
	}

}
