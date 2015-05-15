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
 */
package com.qwazr.utils.threads;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import com.qwazr.utils.ExceptionUtils;

public class ThreadInfo {

	private final String name;

	private final String location;

	private final State state;

	private final String fullStackTrace;

	public ThreadInfo(Thread thread, String prefix) {
		this.name = thread.getName();
		StackTraceElement[] elements = thread.getStackTrace();
		String l = ExceptionUtils.getLocation(elements, prefix);
		if (l == null)
			l = ExceptionUtils.getFirstLocation(elements);
		this.fullStackTrace = ExceptionUtils.getFullStackTrace(elements);
		this.location = l;
		this.state = thread.getState();
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public State getState() {
		return state;
	}

	public String getFullStackTrace() {
		return fullStackTrace;
	}

	public static List<ThreadInfo> getInfos(String classNamePrefix,
			ThreadGroup... groups) throws NamingException {
		if (groups == null)
			return null;
		int count = 0;
		List<Thread[]> threadsArrayList = new ArrayList<Thread[]>(groups.length);
		for (ThreadGroup group : groups) {
			Thread[] threadArray = ThreadUtils.getThreadArray(group);
			threadsArrayList.add(threadArray);
			count += threadArray.length;
		}

		List<ThreadInfo> threadList = new ArrayList<ThreadInfo>(count);
		for (Thread[] threadArray : threadsArrayList)
			for (Thread thread : threadArray)
				threadList.add(new ThreadInfo(thread, classNamePrefix));
		return threadList;
	}

}
