/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.connectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.IOUtils.CloseableContext;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.jdbc.Transaction;
import com.qwazr.utils.jdbc.connection.ConnectionManager;
import com.qwazr.utils.jdbc.connection.DataSourceConnection;
import com.qwazr.utils.jdbc.connection.JDBCConnection;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseConnector extends AbstractConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(DatabaseConnector.class);

	public final String driver = null;

	public final String url = null;

	public final String username = null;

	public final String password = null;

	public final ConnectionPool pool = null;

	public static class ConnectionPool {

		public final Integer initial_size = null;

		public final Integer max_total = null;

		public final Integer max_idle = null;

		public final Integer min_idle = null;

		public final Long max_wait_millis = null;
	}

	@JsonIgnore
	private ConnectionManager connectionManager = null;
	private BasicDataSource basicDataSource = null;

	@Override
	public void load(File data_directory) {
		try {
			if (pool == null) {
				JDBCConnection cnx = new JDBCConnection();
				if (!StringUtils.isEmpty(driver))
					cnx.setDriver(driver);
				if (!StringUtils.isEmpty(url))
					cnx.setUrl(url);
				if (!StringUtils.isEmpty(username))
					cnx.setUsername(username);
				if (!StringUtils.isEmpty(password))
					cnx.setPassword(password);
				connectionManager = cnx;
			} else {
				BasicDataSource ds = new BasicDataSource();
				if (driver != null)
					ds.setDriverClassName(driver);
				if (url != null)
					ds.setUrl(url);
				if (username != null)
					ds.setUsername(username);
				if (password != null)
					ds.setPassword(password);
				if (pool.initial_size != null)
					ds.setInitialSize(pool.initial_size);
				if (pool.max_idle != null)
					ds.setMaxIdle(pool.max_idle);
				if (pool.max_total != null)
					ds.setMaxTotal(pool.max_total);
				if (pool.max_wait_millis != null)
					ds.setMaxWaitMillis(pool.max_wait_millis);
				connectionManager = new DataSourceConnection(ds);

			}
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void unload() {
		if (basicDataSource != null) {
			try {
				if (!basicDataSource.isClosed())
					basicDataSource.close();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public Transaction getConnection(CloseableContext context) throws SQLException {
		Transaction transaction = connectionManager.getNewTransaction();
		context.add(transaction);
		return transaction;
	}

	public Transaction getConnection(CloseableContext context, boolean autoCommit) throws SQLException {
		Transaction transaction = connectionManager.getNewTransaction(autoCommit);
		context.add(transaction);
		return transaction;
	}

	public Transaction getConnection(CloseableContext context, boolean autoCommit, int transactionIsolation) throws SQLException {
		Transaction transaction = connectionManager.getNewTransaction(autoCommit, transactionIsolation);
		context.add(transaction);
		return transaction;
	}

	/**
	 * The current number of active connections that have been allocated from
	 * this connection pool.
	 *
	 * @return the current number of active connections
	 */
	public Integer getPoolNumActive() {
		if (basicDataSource == null)
			return null;
		return basicDataSource.getNumActive();
	}

	/**
	 * The current number of idle connections that are waiting to be allocated
	 * from this connection pool.
	 *
	 * @return the current number of idle connections
	 */
	public Integer getPoolNumIdle() {
		if (basicDataSource == null)
			return null;
		return basicDataSource.getNumIdle();
	}

}
