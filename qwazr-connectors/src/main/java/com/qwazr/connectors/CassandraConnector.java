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
 **/
package com.qwazr.connectors;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.cassandra.CassandraCluster;
import com.qwazr.utils.cassandra.CassandraSession;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CassandraConnector extends AbstractConnector {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class KeySpaceLocator {
		public final String keyspace = null;
		public final String login = null;
		public final String password = null;
		public final String cql = null;
	}

	@JsonIgnore
	public final String default_password = System
			.getProperty("cassandra.default_password");

	// public final KeySpaceLocator keyspaceLocator = null;
	public final List<String> hosts = null;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoginPasswordCredential {
		public final String login = null;
		public final String password = null;
	}

	public final LoginPasswordCredential credentials = null;

	public final Integer timeout_connect_ms = null;
	public final Integer timeout_read_ms = null;

	public final Integer timeout_pool_ms = null;
	public final Integer pool_connections = null;

	@JsonIgnore
	private static CassandraCluster publicCluster = null;

	@JsonIgnore
	private CassandraCluster cluster = null;

	@JsonIgnore
	private String keyspace = null;

	@Override
	public void load(File dataDir) {
		// if (keyspaceLocator != null) {
		// String login = keyspaceLocator.login == null ?
		// keyspaceLocator.keyspace
		// : keyspaceLocator.login;
		// String password = keyspaceLocator.password == null ?
		// keyspaceLocator.login
		// : keyspaceLocator.password;
		// synchronized (keyspaceLocator) {
		// if (publicCluster == null)
		// publicCluster = new CassandraCluster(login, password,
		// hosts, timeout_connect_ms, timeout_read_ms,
		// timeout_pool_ms, pool_connections);
		// }
		// CassandraSession session = publicCluster
		// .getSession(keyspaceLocator.keyspace);
		// Row row = session.execute(keyspaceLocator.cql, contextId).one();
		// if (row == null)
		// return;
		// int size = row.getColumnDefinitions().size();
		// if (size > 0)
		// keyspace = row.getString(0);
		// if (size > 1)
		// login = row.getString(1);
		// if (size > 2)
		// password = row.getString(2);
		// cluster = new CassandraCluster(login,
		// password == null ? default_password : password, hosts,
		// timeout_connect_ms, timeout_read_ms, timeout_pool_ms,
		// pool_connections);
		// }
		if (credentials != null) {
			String login = credentials.login;
			String password = credentials.password == null ? credentials.login
					: credentials.password;
			cluster = new CassandraCluster(login,
					password == null ? default_password : password, hosts,
					timeout_connect_ms, timeout_read_ms, timeout_pool_ms,
					pool_connections);
		} else
			cluster = new CassandraCluster(null, null, hosts,
					timeout_connect_ms, timeout_read_ms, timeout_pool_ms,
					pool_connections);
	}

	@Override
	public void unload() {
		if (cluster != null) {
			IOUtils.closeQuietly(cluster);
			cluster = null;
		}
	}

	public ResultSet executeWithFetchSize(String csql, int fetchSize,
			Object... values) {
		CassandraSession session = cluster.getSession(keyspace);
		return session.executeWithFetchSize(csql, fetchSize, values);
	}

	public ResultSet execute(String csql, Object... values) {
		CassandraSession session = cluster.getSession(keyspace);
		return session.execute(csql, values);
	}

	public UUID getTimeUUID() {
		return UUIDs.timeBased();
	}

	static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

	public long getTimeFromUUID(UUID uuid) {
		return (uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000;
	}

}
