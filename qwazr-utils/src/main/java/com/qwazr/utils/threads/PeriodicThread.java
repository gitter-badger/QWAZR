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
package com.qwazr.utils.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public abstract class PeriodicThread extends Thread {

	protected final int monitoring_period;

	private volatile Long lastExecutionTime = null;

	private static final Logger logger = LoggerFactory.getLogger(PeriodicThread.class);

	protected PeriodicThread(String threadName, int monitoring_period_seconds) {
		super(threadName);
		this.monitoring_period = monitoring_period_seconds * 1000;
	}

	protected abstract void runner();

	@Override
	public void run() {
		// This loop is suppose to execute every minute

		for (; ; ) {
			long start = System.currentTimeMillis();
			lastExecutionTime = start;

			runner();

			// Now we can wait until the next run
			sleepMs(monitoring_period - (System.currentTimeMillis() - start));
		}
	}

	protected void sleepMs(long sleepMs) {
		if (sleepMs <= 0)
			return;
		try {
			sleep(monitoring_period);
		} catch (InterruptedException e) {
			if (logger.isWarnEnabled())
				logger.warn(e.getMessage(), e);
		}
	}

	public Date getLastExecutionDate() {
		Long time = lastExecutionTime;
		if (time == null)
			return null;
		return new Date(time);
	}
}
