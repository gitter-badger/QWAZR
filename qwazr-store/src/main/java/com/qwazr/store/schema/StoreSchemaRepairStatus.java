/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.store.schema;

import java.lang.Thread.State;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class StoreSchemaRepairStatus {

	public final Date start_time;
	public final Date end_time;
	public final Long duration;
	public final String current_path;
	public final Integer checked_directories;
	public final Integer checked_files;
	public final Integer repaired_files;
	public final Boolean running;
	public final Boolean terminated;
	public final Boolean aborting;
	public final String error;

	public StoreSchemaRepairStatus() {
		this(null, null, false, null, null, null, null, null, null);
	}

	StoreSchemaRepairStatus(Date startTime, State state, boolean aborting,
			Date endTime, String currentPath, Integer checkedDirectories,
			Integer checkedFiles, Integer repairedFiles, Exception error) {
		this.start_time = startTime;
		this.end_time = endTime;
		this.running = (state != null && state != State.TERMINATED) ? true
				: null;
		this.terminated = state == State.TERMINATED ? true : null;
		this.aborting = aborting ? true : null;
		if (startTime != null && endTime != null)
			this.duration = endTime.getTime() - startTime.getTime();
		else
			this.duration = startTime == null ? null : startTime.getTime()
					- System.currentTimeMillis();
		this.current_path = currentPath;
		this.checked_directories = checkedDirectories;
		this.checked_files = checkedFiles;
		this.repaired_files = repairedFiles;
		this.error = error == null ? null : error.getMessage();
	}
}
