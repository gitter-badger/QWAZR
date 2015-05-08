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
 **/
package com.qwazr.job.script;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.server.ServerException;

public class ScriptServiceImpl implements ScriptServiceInterface {

	@Override
	public TreeMap<String, ScriptFileStatus> getScripts(Boolean local) {

		// Read the file in the local node
		if (local != null && local)
			return ScriptManager.INSTANCE.getScripts();

		// Read the files present in the remote nodes
		try {
			TreeMap<String, ScriptFileStatus> globalFiles = new TreeMap<String, ScriptFileStatus>();
			ScriptMultiClient client = ScriptManager.getClient();
			ScriptFileStatus.merge(globalFiles, null, client.getScripts(false));

			ScriptManager.INSTANCE.repair(client, globalFiles);

			return globalFiles;
		} catch (URISyntaxException | IOException | ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public String getScript(String script_name) {
		try {
			return ScriptManager.INSTANCE.getScript(script_name);
		} catch (IOException | ServerException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response deleteScript(String script_name, Boolean local) {
		try {
			if (local != null && local) {
				ScriptManager.INSTANCE.deleteScript(script_name);
				return Response.ok().build();
			} else
				return ScriptManager.getClient()
						.deleteScript(script_name, true);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response setScript(String script_name, Long last_modified,
			Boolean local, String script) {
		try {
			if (local != null && local)
				last_modified = ScriptManager.INSTANCE.setScript(script_name,
						last_modified, script);
			else
				ScriptManager.getClient().setScript(script_name, last_modified,
						false, script);
			return Response.ok().build();
		} catch (IOException | URISyntaxException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public ScriptRunStatus runScript(String script_name) {
		return runScriptVariables(script_name, null);
	}

	@Override
	public ScriptRunStatus runScriptVariables(String script_name,
			Map<String, String> variables) {
		try {
			return ScriptManager.INSTANCE.runAsync(script_name, variables);
		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	private ScriptRunThread getRunThread(String script_name, String run_id)
			throws ServerException {
		ScriptRunThread runThread = ScriptManager.INSTANCE.getRunThread(
				script_name, run_id);
		if (runThread == null)
			throw new ServerException(Status.NOT_FOUND, "No status found");
		return runThread;
	}

	@Override
	public ScriptRunStatus getRunStatus(String script_name, String run_id) {
		try {
			return getRunThread(script_name, run_id).getStatus();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunOut(String script_name, String run_id) {
		try {
			return getRunThread(script_name, run_id).getOut();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public String getRunErr(String script_name, String run_id) {
		try {
			return getRunThread(script_name, run_id).getErr();
		} catch (ServerException e) {
			throw e.getTextException();
		}
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(String script_name,
			Boolean local) {
		try {
			if (local != null && local) {
				Map<String, ScriptRunStatus> localRunStatusMap = ScriptManager.INSTANCE
						.getRunsStatus(script_name);
				if (localRunStatusMap == null)
					localRunStatusMap = Collections.emptyMap();
				return localRunStatusMap;
			}
			TreeMap<String, ScriptRunStatus> globalRunStatusMap = new TreeMap<String, ScriptRunStatus>();
			globalRunStatusMap.putAll(ScriptManager.getClient().getRunsStatus(
					script_name, false));
			return globalRunStatusMap;
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}
}
