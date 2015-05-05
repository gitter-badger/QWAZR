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
