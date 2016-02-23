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
import com.qwazr.utils.server.ServerConfiguration;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class QwazrConfiguration extends ServerConfiguration {

	public enum VariablesEnum {

		QWAZR_ETC,

		QWAZR_SERVICES,

		QWAZR_GROUPS,

		QWAZR_SCHEDULER_MAX_THREADS
	}

	public enum ServiceEnum {

		webcrawler,

		extractor,

		scripts,

		schedulers,

		semaphores,

		webapps,

		search,

		graph,

		table,

		compiler;

		/**
		 * @param serverConfiguration
		 * @return true if the service is present
		 */
		public boolean isActive(QwazrConfiguration serverConfiguration) {
			if (serverConfiguration == null)
				return true;
			if (serverConfiguration.services == null)
				return true;
			return serverConfiguration.services.contains(this);
		}
	}

	public final Set<ServiceEnum> services;
	public final Set<String> groups;
	public final FileFilter etcFileFilter;
	public final Integer scheduler_max_threads;

	public QwazrConfiguration(Collection<String> etcs, Collection<ServiceEnum> services, Collection<String> groups,
					Integer schedulerMaxThreads) {
		this.etcFileFilter = buildEtcFileFilter(etcs);
		this.services = buildServices(services);
		this.groups = buildGroups(groups);
		this.scheduler_max_threads = buildSchedulerMaxThreads(schedulerMaxThreads);
	}

	QwazrConfiguration() {
		this.etcFileFilter = buildEtcFileFilter(getPropertyOrEnv(null, VariablesEnum.QWAZR_ETC));
		this.services = buildServices(getPropertyOrEnv(null, VariablesEnum.QWAZR_SERVICES));
		this.groups = buildGroups(getPropertyOrEnv(null, VariablesEnum.QWAZR_GROUPS));
		this.scheduler_max_threads = buildSchedulerMaxThreads(
						getPropertyOrEnv(null, VariablesEnum.QWAZR_SCHEDULER_MAX_THREADS));
	}

	private static FileFilter buildEtcFileFilter(String etc) {
		if (StringUtils.isEmpty(etc))
			return FileFileFilter.FILE;
		String[] array = StringUtils.split(etc, ',');
		if (array == null || array.length == 0)
			return FileFileFilter.FILE;
		return new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(array));
	}

	private static FileFilter buildEtcFileFilter(Collection<String> etcs) {
		if (etcs == null || etcs.isEmpty())
			return FileFileFilter.FILE;
		return new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(etcs.toArray(new String[etcs.size()])));
	}

	private static Set<ServiceEnum> buildServices(Collection<ServiceEnum> serviceCollection) {
		if (serviceCollection == null)
			return null;
		Set<ServiceEnum> services = new HashSet<>();
		services.addAll(serviceCollection);
		return services;
	}

	private static Set<ServiceEnum> buildServices(String servicesString) {
		if (servicesString == null)
			return null;
		String[] services_array = StringUtils.split(servicesString, ',');
		if (services_array == null || services_array.length == 0)
			return null;
		Set<ServiceEnum> services = new HashSet<>();
		for (String service : services_array) {
			try {
				services.add(ServiceEnum.valueOf(service.trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown service in QWAZR_SERVICES: " + service);
			}
		}
		return services;
	}

	private static Set<String> buildGroups(String groupString) {
		if (StringUtils.isEmpty(groupString))
			return null;
		String[] groups_array = StringUtils.split(groupString, ',');
		if (groups_array == null || groups_array.length == 0)
			return null;
		Set<String> groups = new HashSet<>();
		for (String group : groups_array)
			groups.add(group.trim());
		return groups;
	}

	private static Set<String> buildGroups(Collection<String> groupCollection) {
		if (groupCollection == null || groupCollection.isEmpty())
			return null;
		Set<String> groups = new HashSet<>();
		groupCollection.forEach((g) -> groups.add(g.trim()));
		return groups;
	}

	/**
	 * @return the number of allowed threads. The default value is 100.
	 */
	private static int buildSchedulerMaxThreads(String value) {
		if (value == null)
			return buildSchedulerMaxThreads((Integer) null);
		return buildSchedulerMaxThreads(Integer.parseInt(value));
	}

	private static int buildSchedulerMaxThreads(Integer value) {
		return value == null ? 100 : value;
	}

}
