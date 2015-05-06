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