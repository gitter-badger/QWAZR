/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils;

import java.io.File;
import java.io.IOException;

public class TrackedFile {

	private final LockUtils.ReadWriteLock rwl;

	private final FileEventReceiver eventReceiver;
	private final File file;
	private Long lastModified;

	public TrackedFile(FileEventReceiver eventReceiver, File file) {
		this.rwl = new LockUtils.ReadWriteLock();
		this.eventReceiver = eventReceiver;
		this.file = file;
		this.lastModified = null;
	}

	private boolean hasChanged() throws IOException {
		if (file.exists())
			return (lastModified == null || file.lastModified() != lastModified);
		else
			return lastModified != null;
	}

	private void applyChange() throws IOException {
		if (file.exists()) {
			long newLastModified = file.lastModified();
			if (lastModified != null && newLastModified == lastModified)
				return;
			eventReceiver.load();
			lastModified = newLastModified;
		} else {
			if (lastModified == null)
				return;
			eventReceiver.unload();
			lastModified = null;
		}
	}

	public void check() throws IOException {
		rwl.r.lock();
		try {
			if (!hasChanged())
				return;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			applyChange();
		} finally {
			rwl.w.unlock();
		}
	}

	public interface FileEventReceiver {

		void load() throws IOException;

		void unload() throws IOException;

	}
}
