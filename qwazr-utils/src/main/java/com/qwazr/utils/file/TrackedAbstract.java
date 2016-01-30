/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.file;

import com.qwazr.utils.LockUtils;

import java.io.File;

abstract class TrackedAbstract<T> implements TrackedInterface {

	private final LockUtils.ReadWriteLock rwl;

	protected final TrackedInterface.FileChangeConsumer consumer;
	protected final File trackedFile;

	public TrackedAbstract(TrackedInterface.FileChangeConsumer consumer, File trackedFile) {
		this.rwl = new LockUtils.ReadWriteLock();
		this.consumer = consumer;
		this.trackedFile = trackedFile;
	}

	protected abstract void apply(T status);

	protected abstract T getChanges();

	@Override
	final public synchronized void check() {
		rwl.r.lock();
		try {
			if (getChanges() == null)
				return;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			final T changes = getChanges();
			if (changes != null)
				apply(changes);
		} finally {
			rwl.w.unlock();
		}
	}

	final public File getFile() {
		return trackedFile;
	}

}
