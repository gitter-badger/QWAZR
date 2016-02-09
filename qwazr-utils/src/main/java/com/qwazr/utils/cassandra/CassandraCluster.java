/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.SocketOptions;
import com.qwazr.utils.LockUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CassandraCluster implements Closeable {

	private final static Logger logger = LoggerFactory.getLogger(CassandraCluster.class);

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();
	private Cluster cluster;

	private final String login;
	private final String password;
	private final List<String> hosts;
	private CassandraSession rootSession;
	private final LinkedHashMap<String, CassandraSession> sessions;
	private final Integer connectTimeoutMs;
	private final Integer readTimeoutMs;
	private final Integer poolTimeoutMs;
	private final Integer poolConnections;

	public CassandraCluster(String login, String password, List<String> hosts, Integer connectTimeoutMs,
			Integer readTimeoutMs, Integer poolTimeoutMs, Integer poolConnections) {
		cluster = null;
		this.login = login;
		this.password = password == null ? login : password;
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
		this.poolTimeoutMs = poolTimeoutMs;
		this.poolConnections = poolConnections;
		this.hosts = hosts;
		sessions = new LinkedHashMap<String, CassandraSession>();
	}

	@Override
	public void finalize() throws Throwable {
		closeNoLock();
		super.finalize();
	}

	private void closeNoLock() {
		if (cluster != null) {
			IOUtils.closeQuietly(cluster);
			cluster = null;
		}
	}

	@Override
	public void close() throws IOException {
		rwl.w.lock();
		try {
			rootSession = null;
			sessions.clear();
			closeNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	private void checkCluster() {
		rwl.r.lock();
		try {
			if (cluster != null && !cluster.isClosed())
				return;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			if (cluster != null && !cluster.isClosed())
				return;
			Builder builder = Cluster.builder();
			if (hosts != null)
				for (String host : hosts)
					builder.addContactPoint(host);
			builder.withCredentials(login, password);
			SocketOptions socketOptions = builder.getConfiguration().getSocketOptions();
			if (connectTimeoutMs != null)
				socketOptions.setConnectTimeoutMillis(connectTimeoutMs);
			if (readTimeoutMs != null)
				socketOptions.setReadTimeoutMillis(readTimeoutMs);
			PoolingOptions poolingOptions = builder.getConfiguration().getPoolingOptions();
			if (poolTimeoutMs != null)
				poolingOptions.setPoolTimeoutMillis(poolTimeoutMs);
			if (poolConnections != null) {
				poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, poolConnections);
				poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, poolConnections);
				poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, poolConnections);
				poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, poolConnections);
			}
			cluster = builder.build();
			logger.info("New Cluster " + hosts);
			rootSession = null;
			sessions.clear();
		} finally {
			rwl.w.unlock();
		}
	}

	public CassandraSession getSession() {
		checkCluster();
		rwl.r.lock();
		try {
			if (rootSession != null)
				return rootSession;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			if (rootSession != null)
				return rootSession;
			rootSession = new CassandraSession(cluster);
			return rootSession;
		} finally {
			rwl.w.unlock();
		}
	}

	public CassandraSession getSession(String keySpace) {
		if (keySpace == null)
			return getSession();
		checkCluster();
		keySpace = keySpace.intern();
		rwl.r.lock();
		try {
			CassandraSession session = sessions.get(keySpace);
			if (session != null)
				return session;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			CassandraSession session = sessions.get(keySpace);
			if (session != null)
				return session;
			session = new CassandraSession(cluster, keySpace);
			sessions.put(keySpace, session);
			return session;
		} finally {
			rwl.w.unlock();
		}
	}

	public void expireUnusedSince(int minutes) {
		rwl.r.lock();
		try {
			long time = System.currentTimeMillis() - (minutes * 60 * 1000);
			for (CassandraSession session : sessions.values())
				if (session.getLastUse() < time)
					IOUtils.closeQuietly(session);
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			List<String> keys = new ArrayList<String>();
			for (Map.Entry<String, CassandraSession> entry : sessions.entrySet())
				if (entry.getValue().isClosed())
					keys.add(entry.getKey());
			for (String key : keys)
				sessions.remove(key);
		} finally {
			rwl.w.unlock();
		}
	}

	public int getSessionCount() {
		rwl.r.lock();
		try {
			return sessions.size();
		} finally {
			rwl.r.unlock();
		}
	}

}
