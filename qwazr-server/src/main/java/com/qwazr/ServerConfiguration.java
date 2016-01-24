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
 **/
package com.qwazr;

import com.qwazr.utils.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ServerConfiguration {

	public enum ServiceEnum {

		webcrawler,

		extractor,

		scripts,

		schedulers,

		semaphores,

		webapps,

		search,

		graph,

		store,

		table,

		compiler,

		connectors,

		tools;

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

	public final Set<ServiceEnum> services;
	public final Set<String> groups;
	public final File logs_directory;

	public final Integer scheduler_max_threads;

	ServerConfiguration() {

		final String logs_env = System.getenv("QWAZR_LOGS");
		if (StringUtils.isEmpty(logs_env)) {
			logs_directory = null;
		} else {
			logs_directory = new File(logs_env);
			if (!logs_directory.exists())
				logs_directory.mkdirs();
			if (!logs_directory.exists())
				throw new IllegalArgumentException("Cannot create the QWAZR_LOGS directory: " + logs_directory);
		}

		final String services_env = System.getenv("QWAZR_SERVICES");
		if (StringUtils.isEmpty(services_env)) {
			services = null;
		} else {
			services = new HashSet<ServiceEnum>();
			String[] services_array = StringUtils.split(services_env, ',');
			for (String service : services_array) {
				try {
					services.add(ServiceEnum.valueOf(service.trim()));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Unknown service in QWAZR_SERVICES: " + service);
				}
			}
		}
		final String groups_env = System.getenv("QWAZR_GROUPS");
		if (StringUtils.isEmpty(groups_env)) {
			groups = null;
		} else {
			groups = new HashSet<String>();
			String[] groups_array = StringUtils.split(groups_env, ',');
			for (String group : groups_array)
				groups.add(group);
		}
		String s = System.getenv("QWAZR_SCHEDULER_MAX_THREADS");
		scheduler_max_threads = StringUtils.isEmpty(s) ? 100 : Integer.parseInt(s);
	}

	/**
	 * @return the number of allowed threads. The default value is 1000.
	 */
	int getSchedulerMaxThreads() {
		return scheduler_max_threads == null ? 100 : scheduler_max_threads;
	}

}
