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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.cluster.service.TargetRuleEnum;

import java.util.Map;

@JsonInclude(Include.NON_EMPTY)
public class SchedulerDefinition {

	/**
	 * The variables passed to the scripts.
	 */
	public final Map<String, String> variables;

	/**
	 * The path to the scripts
	 */
	public final String script_path;

	/**
	 * Optional timeout in milliseconds
	 */
	public final Integer timeout;

	/**
	 * The optional group targeted
	 */
	public final String group;

	/**
	 * How nodes are targeted
	 */
	public final TargetRuleEnum rule;

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
		script_path = null;
		cron = null;
		time_zone = null;
		timeout = null;
		enabled = null;
		group = null;
		rule = null;
	}

	public SchedulerDefinition(SchedulerDefinition scheduler) {
		this.variables = scheduler.variables;
		this.script_path = scheduler.script_path;
		this.cron = scheduler.cron;
		this.time_zone = scheduler.time_zone;
		this.timeout = scheduler.timeout;
		this.enabled = scheduler.enabled;
		this.group = scheduler.group;
		this.rule = scheduler.rule;
	}

}
