/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.job.script;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.ConnectorContextAbstract.ConnectorMap;
import com.qwazr.job.script.ScriptRunStatus.ScriptState;

@JsonInclude(Include.NON_EMPTY)
public class ScriptRunThread extends SimpleScriptContext implements Runnable {

	private static final Logger logger = LoggerFactory
			.getLogger(ScriptRunThread.class);

	private final String uuid;
	private volatile ScriptState state;
	private volatile Long startTime;
	private volatile Long endTime;
	private volatile Exception exception;

	private final Map<String, ? extends Object> bindings;
	private final ScriptEngine scriptEngine;
	private final File scriptFile;

	ScriptRunThread(ScriptEngine scriptEngine, File scriptFile,
			Map<String, ? extends Object> bindings, ConnectorMap connectors) {
		uuid = UUIDs.timeBased().toString();
		state = ScriptState.ready;
		startTime = null;
		endTime = null;
		this.bindings = bindings;
		this.scriptEngine = scriptEngine;
		if (bindings != null)
			engineScope.putAll(bindings);
		if (connectors != null)
			engineScope.put("connectors", connectors);
		this.scriptFile = scriptFile;
		this.setWriter(new StringWriter());
		this.setErrorWriter(new StringWriter());
		removeAttributeIfAny("quit", "exit");
	}

	private void removeAttributeIfAny(String... names) {
		if (names == null)
			return;
		for (String name : names) {
			int scope = getAttributesScope(name);
			if (scope != -1)
				removeAttribute(name, scope);
		}
	}

	@Override
	public void run() {
		logger.info("Execute: " + scriptFile.getName());
		state = ScriptState.running;
		startTime = System.currentTimeMillis();
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(scriptFile);
			scriptEngine.eval(fileReader, this);
			state = ScriptState.terminated;
		} catch (Exception e) {
			state = ScriptState.error;
			exception = e;
			logger.error(
					"Error on " + scriptFile.getName() + " - " + e.getMessage(),
					e);
		} finally {
			endTime = System.currentTimeMillis();
			if (fileReader != null)
				IOUtils.closeQuietly(fileReader);
		}
	}

	public Exception getException() {
		return exception;
	}

	public String getUUID() {
		return uuid;
	}

	public String getOut() {
		return getWriter().toString();
	}

	public String getErr() {
		return getErrorWriter().toString();
	}

	ScriptState getState() {
		return state;
	}

	public ScriptRunStatus getStatus() {
		return new ScriptRunStatus(ClusterManager.INSTANCE.myAddress,
				scriptFile.getName(), uuid, state, startTime, endTime,
				bindings == null ? null : bindings.keySet(), exception);
	}

	boolean hasExpired() {
		return endTime != null;
	}
}
