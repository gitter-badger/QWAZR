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
import com.qwazr.connectors.ConnectorManager.ConnectorMap;
import com.qwazr.job.script.ScriptRunStatus.ScriptState;
import com.qwazr.tools.ToolsManager.ToolMap;

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
			Map<String, ? extends Object> bindings, ConnectorMap connectors,
			ToolMap tools) {
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
		if (tools != null)
			engineScope.put("tools", tools);
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
