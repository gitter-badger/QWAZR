/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
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
	public void load(ConnectorContext context) {
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
	public void unload(ConnectorContext context) {
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

}
