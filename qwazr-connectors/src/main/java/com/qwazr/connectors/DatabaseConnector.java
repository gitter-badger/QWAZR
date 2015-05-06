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
package com.qwazr.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.pojodbc.connection.ConnectionManager;
import com.qwazr.utils.pojodbc.connection.JDBCConnection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseConnector extends AbstractConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(DatabaseConnector.class);

	public final String driver = null;

	public final String url = null;

	public final String username = null;

	public final String password = null;

	@JsonIgnore
	private JDBCConnection connectionManager = null;

	@Override
	public void load(String contextId) {
		try {
			connectionManager = new JDBCConnection();
			if (!StringUtils.isEmpty(driver))
				connectionManager.setDriver(driver);
			if (!StringUtils.isEmpty(url))
				connectionManager.setUrl(url);
			if (!StringUtils.isEmpty(username))
				connectionManager.setUsername(username);
			if (!StringUtils.isEmpty(password))
				connectionManager.setPassword(password);
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void unload(String contextId) {
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

}
