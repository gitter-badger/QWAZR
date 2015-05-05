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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.ServerException;

public class ScriptServiceImpl implements ScriptServiceInterface {

	@Override
	public TreeMap<String, ScriptFileStatus> getScripts(Boolean local) {

		// Read the file in the local node
		TreeMap<String, ScriptFileStatus> localFiles = ScriptManager.INSTANCE
				.getScripts();
		if (local != null && local)
			return localFiles;

		// Read the files present in the remote nodes
		try {
			TreeMap<String, ScriptFileStatus> globalFiles = new TreeMap<String, ScriptFileStatus>();
			ScriptMultiClient client = ScriptManager.getClient(true);
			ScriptFileStatus.merge(globalFiles,
					ClusterManager.INSTANCE.myAddress, localFiles);
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
				return ScriptManager.getClient(false).deleteScript(script_name,
						true);
		} catch (ServerException | URISyntaxException e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public Response setScript(String script_name, Long last_modified,
			Boolean local, String script) {
		try {
			last_modified = ScriptManager.INSTANCE.setScript(script_name,
					last_modified, script);
			if (local == null || !local)
				ScriptManager.getClient(true).setScript(script_name,
						last_modified, false, script);
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
			Map<String, ScriptRunStatus> localRunStatusMap = ScriptManager.INSTANCE
					.getRunsStatus(script_name);
			if (local != null && local) {
				if (localRunStatusMap == null)
					localRunStatusMap = Collections.emptyMap();
				return localRunStatusMap;
			}
			TreeMap<String, ScriptRunStatus> globalRunStatusMap = new TreeMap<String, ScriptRunStatus>();
			if (localRunStatusMap != null)
				globalRunStatusMap.putAll(localRunStatusMap);
			globalRunStatusMap.putAll(ScriptManager.getClient(true)
					.getRunsStatus(script_name, false));
			return globalRunStatusMap;
		} catch (URISyntaxException e) {
			throw ServerException.getJsonException(e);
		}
	}
}
