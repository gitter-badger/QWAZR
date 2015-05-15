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
package com.qwazr.job.scheduler;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class SchedulerDefinition {

	/**
	 * The variables passed to the script.
	 */
	public final Map<String, String> variables;

	/**
	 * Name of the script
	 */
	public final String script_name;

	/**
	 * The cron expression
	 */
	public final String cron;

	/**
	 * The time zone
	 */
	public final String time_zone;

	/**
	 * Cron enabled
	 */
	public Boolean enabled;

	public SchedulerDefinition() {
		variables = null;
		script_name = null;
		cron = null;
		time_zone = null;
		enabled = null;
	}

	public SchedulerDefinition(SchedulerDefinition scheduler) {
		this.variables = scheduler.variables;
		this.script_name = scheduler.script_name;
		this.cron = scheduler.cron;
		this.time_zone = scheduler.time_zone;
		this.enabled = scheduler.enabled;
	}

}
