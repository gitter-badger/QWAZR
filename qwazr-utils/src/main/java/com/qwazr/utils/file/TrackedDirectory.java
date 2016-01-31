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
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TrackedDirectory extends TrackedAbstract<TrackedDirectory.DirectoryChanges> {

	private volatile Map<File, Long> trackedFiles;
	private final FileFilter fileFilter;

	public TrackedDirectory(File directory, FileFilter fileFilter) {
		super(directory);
		this.fileFilter = fileFilter;
		this.trackedFiles = null;
	}

	@Override
	final protected void apply(DirectoryChanges status) {
		trackedFiles = status.trackedFiles;
		if (status.changes != null)
			status.changes.forEach((file, change) -> notify(change.reason, file));
	}

	final private boolean isChanges(File[] files) {
		final int newFileCount = files == null ? 0 : files.length;
		final int oldFileCount = trackedFiles == null ? 0 : trackedFiles.size();
		if (newFileCount != oldFileCount)
			return true;
		if (oldFileCount == 0)
			return false;
		for (File file : files) {
			Long lastModified = trackedFiles.get(file);
			if (lastModified == null || file.lastModified() != lastModified)
				return true;
		}
		return false;
	}

	final private void buildAllDelete(Map<File, TrackedFile.FileChange> changes) {
		trackedFiles
				.forEach((file, aLong) -> changes.put(file, new TrackedFile.FileChange(ChangeReason.DELETED, null)));
	}

	final private void buildChanges(File[] files, final Map<File, Long> newTrackedFiles,
			Map<File, TrackedFile.FileChange> changes) {
		for (File file : files) {
			final long newLastModified = file.lastModified();
			newTrackedFiles.put(file, newLastModified);
			Long lastModified = trackedFiles == null ? null : trackedFiles.get(file);
			if (lastModified == null || lastModified != newLastModified)
				changes.put(file, new TrackedFile.FileChange(ChangeReason.UPDATED, newLastModified));
		}
		if (trackedFiles != null)
			trackedFiles.forEach(new BiConsumer<File, Long>() {
				@Override
				public void accept(File file, Long aLong) {
					if (!newTrackedFiles.containsKey(file))
						new TrackedFile.FileChange(ChangeReason.DELETED, null);
				}
			});
	}

	@Override
	final protected DirectoryChanges getChanges() {
		final File[] files;
		if (trackedFile.exists() && trackedFile.isDirectory())
			files = trackedFile.listFiles(fileFilter);
		else
			files = null;
		if (!isChanges(files))
			return null;

		final Map<File, Long> newTrackedFiles;
		final Map<File, TrackedFile.FileChange> changes = new HashMap<>();

		if (files == null || files.length == 0) {
			newTrackedFiles = null;
			buildAllDelete(changes);
		} else {
			newTrackedFiles = new HashMap<>();
			buildChanges(files, newTrackedFiles, changes);
		}

		return new DirectoryChanges(newTrackedFiles, changes);
	}

	static final class DirectoryChanges {

		final private Map<File, Long> trackedFiles;
		final private Map<File, TrackedFile.FileChange> changes;

		private DirectoryChanges(Map<File, Long> trackedFiles, Map<File, TrackedFile.FileChange> changes) {
			this.trackedFiles = trackedFiles;
			this.changes = changes;
		}
	}
}
