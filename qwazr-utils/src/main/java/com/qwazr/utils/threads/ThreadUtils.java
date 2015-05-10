/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class ThreadUtils {

	public static class ThreadGroupFactory implements ThreadFactory {

		private final ThreadGroup group;

		public ThreadGroupFactory(ThreadGroup group) {
			this.group = group;
		}

		@Override
		public Thread newThread(Runnable target) {
			return new Thread(group, target);
		}

	}

	public static Thread[] getThreadArray(ThreadGroup group) {
		Thread[] threads = new Thread[group.activeCount()];
		for (;;) {
			int l = group.enumerate(threads);
			if (l == threads.length)
				break;
			threads = new Thread[l];
		}
		return threads;
	}

	public final static void sleepMs(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static interface WaitInterface {

		boolean done();

		boolean abort();
	}

	public static boolean waitUntil(long secTimeOut, WaitInterface waiter) {
		long finalTime = System.currentTimeMillis() + secTimeOut * 1000;
		while (!waiter.done()) {
			if (waiter.abort())
				return false;
			if (secTimeOut != 0)
				if (System.currentTimeMillis() > finalTime)
					return false;
			sleepMs(200);
		}
		return true;
	}

	public static abstract class CallableExceptionCatcher<T> implements
			Callable<T> {

		protected Exception exception = null;

		public void checkException() throws Exception {
			if (exception != null)
				throw exception;
		}
	}

	public static abstract class FunctionExceptionCatcher<T> extends
			CallableExceptionCatcher<T> {

		private T result = null;

		public abstract T execute() throws Exception;

		@Override
		final public T call() throws Exception {
			try {
				return result = execute();
			} catch (Exception e) {
				exception = e;
				throw e;
			}
		}

		public T getResult() {
			return result;
		}
	}

	public static abstract class ProcedureExceptionCatcher extends
			CallableExceptionCatcher<Object> {

		public abstract void execute() throws Exception;

		@Override
		final public Object call() throws Exception {
			try {
				execute();
				return null;
			} catch (Exception e) {
				exception = e;
				throw e;
			}
		}
	}

	public static <T> void invokeAndJoin(ExecutorService executor,
			Collection<? extends CallableExceptionCatcher<T>> callables)
			throws Exception {
		executor.invokeAll(callables);
		for (CallableExceptionCatcher<?> callable : callables)
			callable.checkException();
	}

	public static <T> T getFirstResult(
			Collection<? extends FunctionExceptionCatcher<T>> callables) {
		for (FunctionExceptionCatcher<T> callable : callables)
			if (callable.getResult() != null)
				return callable.getResult();
		return null;
	}

}
