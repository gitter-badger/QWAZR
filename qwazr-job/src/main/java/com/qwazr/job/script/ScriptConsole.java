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
package com.qwazr.job.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptConsole {

	private static final Logger logger = LoggerFactory
			.getLogger(ConsoleLogger.class);

	public void log(Object object) {
		log().info(object);
	}

	public ConsoleLogger log() {
		return ConsoleLogger.INSTANCE;
	}

	public static class ConsoleLogger {

		private static final ConsoleLogger INSTANCE = new ConsoleLogger();

		public void info(Object object) {
			if (object == null)
				return;
			if (logger.isInfoEnabled())
				logger.info(object.toString());
		}

		public void warn(Object object) {
			if (object == null)
				return;
			if (logger.isWarnEnabled())
				logger.warn(object.toString());
		}

		public void error(Object object) {
			if (object == null)
				return;
			if (logger.isErrorEnabled())
				logger.error(object.toString());
		}

		public void debug(Object object) {
			if (object == null)
				return;
			if (logger.isDebugEnabled())
				logger.debug(object.toString());
		}

		public void trace(Object object) {
			if (object == null)
				return;
			if (logger.isTraceEnabled())
				logger.trace(object.toString());
		}

	}
}
