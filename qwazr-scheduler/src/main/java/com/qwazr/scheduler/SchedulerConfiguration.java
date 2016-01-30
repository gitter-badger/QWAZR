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
package com.qwazr.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerConfiguration {

	public final Map<String, SchedulerDefinition> schedulers;

	public SchedulerConfiguration() {
		schedulers = null;
	}

	@JsonIgnore
	static Map<String, SchedulerDefinition> merge(Collection<SchedulerConfiguration> schedulerConfigurations) {
		if (schedulerConfigurations == null)
			return null;
		final Map<String, SchedulerDefinition> schedulers = new HashMap<>();
		schedulerConfigurations.forEach((scheduler) -> schedulers.putAll(scheduler.schedulers));
		return schedulers;
	}
}
