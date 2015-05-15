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
package com.qwazr.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

public class ExceptionUtils extends
		org.apache.commons.lang3.exception.ExceptionUtils {

	public final static String getLocation(StackTraceElement[] stackTrace,
			String prefix) {
		for (StackTraceElement element : stackTrace)
			if (element.getClassName().startsWith(prefix))
				return element.toString();
		return null;
	}

	public final static String getFirstLocation(StackTraceElement[] stackTrace) {
		for (StackTraceElement element : stackTrace) {
			String ele = element.toString();
			if (ele != null && ele.length() > 0)
				return ele;
		}
		return null;
	}

	public final static String getFullStackTrace(StackTraceElement[] stackTrace) {
		StringWriter sw = null;
		PrintWriter pw = null;
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			for (StackTraceElement element : stackTrace)
				pw.println(element);
			return sw.toString();
		} finally {
			IOUtils.close(pw, sw);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Exception> T throwException(Exception exception,
			Class<T> exceptionClass) throws T {
		if (exception == null)
			return null;
		if (exceptionClass.isInstance(exception))
			throw (T) exception;
		try {
			return (T) exceptionClass.getConstructor(Exception.class)
					.newInstance(exception);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public static Exception getCauseIfException(Exception e) {
		if (e == null)
			return null;
		Throwable cause = e.getCause();
		return cause instanceof Exception ? (Exception) cause : e;
	}

	public static List<String> getStackTraces(Throwable throwable) {
		StackTraceElement[] stElements = throwable.getStackTrace();
		if (stElements == null)
			return null;
		List<String> stList = new ArrayList<String>(stElements.length);
		for (StackTraceElement stElement : stElements)
			stList.add(stElement.toString());
		return stList;
	}

	public static class ExceptionHolder<T extends Exception> {

		private final Logger logger;
		private volatile T holdException;

		public ExceptionHolder(Logger logger) {
			this.logger = logger;
			this.holdException = null;
		}

		/**
		 * Hold the new exception. If there was a previous exception, a log warn
		 * is emited
		 * 
		 * @param newException
		 *            the new exception to old
		 */
		public void switchAndWarn(T newException) {
			if (holdException != null)
				logger.warn(holdException.getMessage(), holdException);
			holdException = newException;
		}

		/**
		 * @return the previously hold exception
		 */
		public T getException() {
			return holdException;
		}

	}
}
