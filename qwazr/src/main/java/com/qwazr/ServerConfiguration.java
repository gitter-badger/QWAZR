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
package com.qwazr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ServerConfiguration {

	public static enum ServiceEnum {

		webcrawler,

		extractor,

		scripts,

		schedulers,

		webapps,

		search,

		graph;

		/**
		 * @param serverConfiguration
		 * @return true if the service is present
		 */
		public boolean isActive(ServerConfiguration serverConfiguration) {
			if (serverConfiguration == null)
				return true;
			if (serverConfiguration.services == null)
				return true;
			return serverConfiguration.services.contains(this);
		}
	}

	public final Set<ServiceEnum> services = null;

	public final Integer scheduler_max_threads = null;

	/**
	 * @return the number of allowed threads. The default value is 1000.
	 */
	int getSchedulerMaxThreads() {
		return scheduler_max_threads == null ? 1000 : scheduler_max_threads;
	}

	/**
	 * Load the configuration file.
	 * 
	 * @param file
	 * @return an instance of ServerConfiguration
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static ServerConfiguration getNewInstance(File file)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		return mapper.readValue(file, ServerConfiguration.class);
	}

	/**
	 * Read the configuration file from the resources
	 * 
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static ServerConfiguration getDefaultConfiguration()
			throws JsonParseException, JsonMappingException, IOException {
		InputStream stream = Qwazr.class.getResourceAsStream("server.yaml");
		if (stream == null)
			throw new IOException(
					"Unable to load the default configuration resource: server.yaml");
		try {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			return mapper.readValue(stream, ServerConfiguration.class);
		} finally {
			if (stream != null)
				IOUtils.closeQuietly(stream);
		}

	}
}
