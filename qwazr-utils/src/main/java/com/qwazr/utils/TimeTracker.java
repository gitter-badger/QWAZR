/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import java.util.LinkedHashMap;
import java.util.Map;

public class TimeTracker {

	private final long start_time;
	private long time;

	private final LinkedHashMap<String, Long> timerMap;

	/**
	 * Initiate the time tracker and collect the starting time.
	 */
	public TimeTracker() {
		timerMap = new LinkedHashMap<String, Long>();
		start_time = time = System.currentTimeMillis();
	}

	/**
	 * Add a new entry, with the given name and the elapsed time.
	 *
	 * @param name the name of the time entry
	 */
	public synchronized void next(String name) {
		long t = System.currentTimeMillis();
		timerMap.put(name, t - time);
		time = t;
	}

	/**
	 * @return the backed map
	 */
	public LinkedHashMap<String, Long> getMap() {
		return timerMap;
	}

	public long getTotalTime() {
		return time - start_time;
	}
}
