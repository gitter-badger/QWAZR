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
package com.qwazr.utils.cassandra;

import java.io.Closeable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.qwazr.utils.LockUtils;

public class CassandraSession implements Closeable {

	private static final Logger logger = LoggerFactory
			.getLogger(CassandraSession.class);

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	private volatile long lastUse;

	private final Cluster cluster;
	private final String keySpace;
	private Session session;

	public CassandraSession(Cluster cluster) {
		this(cluster, null);
	}

	public CassandraSession(Cluster cluster, String keySpace) {
		this.cluster = cluster;
		this.keySpace = keySpace;
		session = null;
		lastUse = System.currentTimeMillis();
	}

	@Override
	public void finalize() {
		closeNoLock();
	}

	private void closeNoLock() {
		if (session != null) {
			if (!session.isClosed())
				IOUtils.closeQuietly(session);
			session = null;
		}
	}

	@Override
	public void close() {
		rwl.w.lock();
		try {
			closeNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	public boolean isClosed() {
		rwl.r.lock();
		try {
			return session == null || session.isClosed();
		} finally {
			rwl.r.unlock();
		}
	}

	private Session checkSession() {
		rwl.r.lock();
		try {
			lastUse = System.currentTimeMillis();
			if (session != null && !session.isClosed())
				return session;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			if (session != null && !session.isClosed())
				return session;
			if (cluster == null || cluster.isClosed())
				throw new DriverException("The cluster is closed");
			logger.info("Create session " + keySpace == null ? StringUtils.EMPTY
					: keySpace);
			session = keySpace == null ? cluster.connect() : cluster
					.connect(keySpace);
			return session;
		} finally {
			rwl.w.unlock();
		}
	}

	private SimpleStatement getStatement(String cql, Integer fetchSize,
			Object... values) {
		SimpleStatement statement = values != null && values.length > 0 ? new SimpleStatement(
				cql, values) : new SimpleStatement(cql);
		if (fetchSize != null)
			statement.setFetchSize(fetchSize);
		return statement;
	}

	private ResultSet executeStatement(SimpleStatement statement) {
		session = checkSession();
		try {
			return session.execute(statement);
		} catch (NoHostAvailableException e1) {
			if (cluster == null || !cluster.isClosed())
				throw e1;
			try {
				return session.execute(statement);
			} catch (DriverException e2) {
				logger.warn(e2.getMessage(), e2);
				throw e1;
			}
		}

	}

	public ResultSet executeWithFetchSize(String cql, int fetchSize,
			Object... values) {
		logger.info("Execute " + cql);
		SimpleStatement statement = getStatement(cql, fetchSize, values);
		return executeStatement(statement);
	}

	public ResultSet execute(String cql, Object... values) {
		logger.info("Execute " + cql);
		SimpleStatement statement = getStatement(cql, null, values);
		return executeStatement(statement);
	}

	long getLastUse() {
		return lastUse;
	}

}
