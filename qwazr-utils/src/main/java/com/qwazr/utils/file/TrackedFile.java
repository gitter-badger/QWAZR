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

import java.io.File;

public class TrackedFile extends TrackedAbstract<TrackedFile.FileChange> {

	private volatile Long lastModified;

	public TrackedFile(File file) {
		super(file);
		this.lastModified = null;
	}

	@Override
	final protected void apply(FileChange change) {
		lastModified = change.lastModified;
		notify(change.reason, trackedFile);
	}

	@Override
	final protected FileChange getChanges() {
		if (trackedFile.exists()) {
			final Long newLastModified = trackedFile.lastModified();
			if (newLastModified == lastModified)
				return null;
			return new FileChange(ChangeReason.UPDATED, newLastModified);
		} else {
			if (lastModified == null)
				return null;
			return new FileChange(ChangeReason.DELETED, null);
		}
	}

	final static class FileChange {

		final ChangeReason reason;
		final Long lastModified;

		FileChange(ChangeReason reason, Long lastModified) {
			this.reason = reason;
			this.lastModified = lastModified;
		}
	}

}
