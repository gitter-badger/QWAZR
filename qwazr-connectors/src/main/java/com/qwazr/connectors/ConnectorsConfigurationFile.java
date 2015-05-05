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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorsConfigurationFile {

	private static final Logger logger = LoggerFactory
			.getLogger(ConnectorsConfigurationFile.class);

	public List<AbstractConnector> connectors;

	/**
	 * This method loads the connectors.
	 * 
	 * @param context
	 *            The connector context
	 * @param configuration
	 *            The configuration definition
	 */
	public static void load(ConnectorContextAbstract context,
			ConnectorsConfigurationFile configuration) {
		if (configuration == null || configuration.connectors == null)
			return;
		for (AbstractConnector connector : configuration.connectors) {
			logger.info("Loading connector: " + connector.name);
			connector.load(context);
			context.add(connector);
		}
	}
}