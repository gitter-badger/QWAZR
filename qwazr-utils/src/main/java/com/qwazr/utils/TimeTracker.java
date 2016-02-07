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
 */
package com.qwazr.utils;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.LinkedHashMap;

public class TimeTracker {

	private final long startTime;
	private volatile long time;
	private volatile long unknownTime;

	private final LinkedHashMap<String, Long> entries;
	private volatile LinkedHashMap<String, Long> cachedEntries;

	/**
	 * Initiate the time tracker and collect the starting time.
	 */
	public TimeTracker() {
		entries = new LinkedHashMap<>();
		startTime = time = System.currentTimeMillis();
		unknownTime = 0;
		cachedEntries = null;
	}

	/**
	 * Add a new entry, with the given name and the elapsed time.
	 *
	 * @param name the name of the time entry
	 */
	public synchronized void next(String name) {
		final long t = System.currentTimeMillis();
		final long elapsed = t - time;
		if (name != null) {
			Long duration = entries.get(name);
			if (duration == null)
				duration = elapsed;
			else
				duration += elapsed;
			entries.put(name, duration);
			cachedEntries = null;
		} else
			unknownTime += elapsed;
		time = t;
	}

	/**
	 * @return the backed map
	 */
	private synchronized LinkedHashMap<String, Long> buildCache() {
		if (cachedEntries == null)
			cachedEntries = new LinkedHashMap<>(entries);
		return cachedEntries;
	}

	public Status getStatus() {
		return new Status(this);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Status {

		final public Date start_time;

		final public Long total_time;

		final public Long unknown_time;

		final public LinkedHashMap<String, Long> durations;

		public Status() {
			start_time = null;
			total_time = null;
			unknown_time = null;
			durations = null;
		}

		private Status(TimeTracker timeTracker) {
			start_time = new Date(timeTracker.startTime);
			total_time = timeTracker.time - timeTracker.startTime;
			unknown_time = timeTracker.unknownTime;
			durations = timeTracker.buildCache();
		}
	}
}
