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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.json.client.JsonMultiClientAbstract;
import com.qwazr.utils.server.ServerException;

public class ScriptMultiClient extends
		JsonMultiClientAbstract<String, ScriptSingleClient> implements
		ScriptServiceInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(ScriptMultiClient.class);

	public ScriptMultiClient(ExecutorService executor, String[] urls,
			int msTimeOut) throws URISyntaxException {
		super(executor, new ScriptSingleClient[urls.length], urls, msTimeOut);
	}

	@Override
	protected ScriptSingleClient newClient(String url, int msTimeOut)
			throws URISyntaxException {
		return new ScriptSingleClient(url, msTimeOut);
	}

	@Override
	public TreeMap<String, ScriptFileStatus> getScripts(Boolean local) {
		// If not global, just request the local node
		if (local != null && local) {
			ScriptSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
			if (client == null)
				throw new ServerException(Status.NOT_ACCEPTABLE,
						"Node not valid: " + ClusterManager.INSTANCE.myAddress)
						.getJsonException();
			return client.getScripts(true);
		}

		// We merge the result of all the nodes
		TreeMap<String, ScriptFileStatus> globalMap = new TreeMap<String, ScriptFileStatus>();
		for (ScriptSingleClient client : this) {
			try {
				TreeMap<String, ScriptFileStatus> localMap = client
						.getScripts(true);
				if (localMap == null)
					continue;
				ScriptFileStatus.merge(globalMap, client.url, localMap);
			} catch (WebApplicationException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return globalMap;
	}

	@Override
	public String getScript(String script_name) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				return client.getScript(script_name);
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404)
					logger.warn(e.getMessage(), e);
				else
					exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return StringUtils.EMPTY;
	}

	@Override
	public Response deleteScript(String script_name, Boolean local) {

		if (local != null && local) {
			ScriptSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
			if (client == null)
				throw new ServerException(Status.NOT_ACCEPTABLE,
						"Node not valid: " + ClusterManager.INSTANCE.myAddress)
						.getJsonException();
			return client.deleteScript(script_name, true);
		}

		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		boolean deleted = false;
		for (ScriptSingleClient client : this) {
			try {
				if (client.deleteScript(script_name, true).getStatus() == 200)
					deleted = true;
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404)
					logger.warn(e.getMessage(), e);
				else
					exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		if (!deleted)
			throw new WebApplicationException("Script not found",
					Status.NOT_FOUND);
		return Response.ok("Script deleted").build();
	}

	@Override
	public Response setScript(String script_name, Long last_modified,
			Boolean local, String script) {

		if (local != null && local) {
			ScriptSingleClient client = getClientByUrl(ClusterManager.INSTANCE.myAddress);
			if (client == null)
				throw new ServerException(Status.NOT_ACCEPTABLE,
						"Node not valid: " + ClusterManager.INSTANCE.myAddress)
						.getJsonException();
			return client.setScript(script_name, last_modified, true, script);
		}

		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				client.setScript(script_name, last_modified, true, script);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return Response.ok().build();
	}

	@Override
	public ScriptRunStatus runScript(String script_name) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				return client.runScript(script_name);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return null;
	}

	@Override
	public ScriptRunStatus runScriptVariables(String script_name,
			Map<String, String> variables) {
		WebAppExceptionHolder exceptionHolder = new WebAppExceptionHolder(
				logger);
		for (ScriptSingleClient client : this) {
			try {
				return client.runScriptVariables(script_name, variables);
			} catch (WebApplicationException e) {
				exceptionHolder.switchAndWarn(e);
			}
		}
		if (exceptionHolder.getException() != null)
			throw exceptionHolder.getException();
		return null;
	}

	public ScriptRunStatus runScript(String script_name, String... variables) {
		if (variables == null || variables.length == 0)
			return runScript(script_name);
		HashMap<String, String> variablesMap = new HashMap<String, String>();
		int l = variables.length / 2;
		for (int i = 0; i < l; i++)
			variablesMap.put(variables[i * 2], variables[i * 2 + 1]);
		return runScriptVariables(script_name, variablesMap);
	}

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(String script_name,
			Boolean local) {
		if (local != null && local)
			return getClientByUrl(ClusterManager.INSTANCE.myAddress)
					.getRunsStatus(script_name, true);
		TreeMap<String, ScriptRunStatus> results = new TreeMap<String, ScriptRunStatus>();
		for (ScriptSingleClient client : this) {
			try {
				results.putAll(client.getRunsStatus(script_name, true));
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() != 404)
					throw e;
			}
		}
		return results;
	}

	@Override
	public ScriptRunStatus getRunStatus(String script_name, String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunStatus(script_name, run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunOut(String script_name, String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunOut(script_name, run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

	@Override
	public String getRunErr(String script_name, String run_id) {
		for (ScriptSingleClient client : this) {
			try {
				return client.getRunErr(script_name, run_id);
			} catch (WebApplicationException e) {
				throw e;
			}
		}
		return null;
	}

}
