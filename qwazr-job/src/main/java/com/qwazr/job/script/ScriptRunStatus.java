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
package com.qwazr.job.script;

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class ScriptRunStatus {

	public enum ScriptState {
		ready, running, terminated, error;
	}

	public final String node;
	public final String _status;
	public final String _std_out;
	public final String _std_err;
	public final String uuid;
	public final ScriptState state;
	public final Date start;
	public final Date end;
	public final Set<String> bindings;
	public final String error;

	public ScriptRunStatus() {
		node = null;
		_status = null;
		_std_out = null;
		_std_err = null;
		uuid = null;
		state = null;
		start = null;
		end = null;
		bindings = null;
		error = null;
	}

	ScriptRunStatus(String node, String name, String uuid, ScriptState state,
			Long startTime, Long endTime, Set<String> bindings,
			Exception exception) {
		this.node = node;
		this.start = startTime == null ? null : new Date(startTime);
		this.end = endTime == null ? null : new Date(endTime);
		this.bindings = bindings;
		this._status = node + "/scripts/" + name + "/status/" + uuid;
		this._std_out = node + "/scripts/" + name + "/status/" + uuid + "/out";
		this._std_err = node + "/scripts/" + name + "/status/" + uuid + "/err";
		this.uuid = uuid.toString();
		this.state = state;
		this.error = exception == null ? null : exception.getMessage();
	}
}
