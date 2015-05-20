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
package com.qwazr.store.schema;

import java.io.Closeable;
import java.io.File;
import java.lang.Thread.State;

import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.server.ServerException;

public class StoreSchemaInstance implements Closeable {

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();
	private final String schemaName;
	private StoreSchemaRepairThread repairThread;

	StoreSchemaInstance(File directory, String schemaName) {
		this.schemaName = schemaName;
		repairThread = null;
	}

	@Override
	public void close() {
		rwl.r.lock();
		try {
			if (repairThread != null)
				repairThread.abort();
		} finally {
			rwl.r.unlock();
		}
	}

	private void checkNotExistsOrAlive() throws ServerException {
		if (repairThread == null)
			return;
		if (repairThread.getState() == State.TERMINATED)
			return;
		throw new ServerException(Status.CONFLICT,
				"A repair process is already running.");
	}

	StoreSchemaRepairStatus startRepair(Integer msTimeout)
			throws ServerException {
		rwl.r.lock();
		try {
			checkNotExistsOrAlive();
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			checkNotExistsOrAlive();
			repairThread = new StoreSchemaRepairThread(schemaName, msTimeout);
			return repairThread.getRepairStatus();
		} finally {
			rwl.w.unlock();
		}
	}

	private void checkRepairExistsOrNotFound() throws ServerException {
		if (repairThread == null)
			throw new ServerException(Status.NOT_FOUND,
					"No repair process found for schema: " + schemaName);
	}

	StoreSchemaRepairStatus getRepairStatus() throws ServerException {
		rwl.r.lock();
		try {
			checkRepairExistsOrNotFound();
			return repairThread.getRepairStatus();
		} finally {
			rwl.r.unlock();
		}
	}

	StoreSchemaRepairStatus stopRepair() throws ServerException {
		rwl.r.lock();
		try {
			checkRepairExistsOrNotFound();
			repairThread.abort();
			return repairThread.getRepairStatus();
		} finally {
			rwl.r.unlock();
		}
	}
}
