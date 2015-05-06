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
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;

public class ScriptSingleClient extends JsonClientAbstract implements
		ScriptServiceInterface {

	ScriptSingleClient(String url, int msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
	}

	public final static TypeReference<TreeMap<String, ScriptFileStatus>> MapStringFileStatusTypeRef = new TypeReference<TreeMap<String, ScriptFileStatus>>() {
	};

	@Override
	public TreeMap<String, ScriptFileStatus> getScripts(Boolean local) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts");
			if (local != null)
				uriBuilder.setParameter("local", local.toString());
			Request request = Request.Get(uriBuilder.build());
			return (TreeMap<String, ScriptFileStatus>) execute(request, null,
					msTimeOut, MapStringFileStatusTypeRef, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getScript(String script_name) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name);
			Request request = Request.Get(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response deleteScript(String script_name, Boolean local) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name);
			if (local != null)
				uriBuilder.setParameter("local", local.toString());
			Request request = Request.Delete(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			return Response.status(response.getStatusLine().getStatusCode())
					.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response setScript(String script_name, Long last_modified,
			Boolean local, String script) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name);
			if (local != null)
				uriBuilder.setParameter("local", local.toString());
			if (last_modified != null)
				uriBuilder.setParameter("last_modified",
						last_modified.toString());
			Request request = Request.Post(uriBuilder.build());
			HttpResponse response = execute(request, script, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			return Response.ok().build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ScriptRunStatus runScript(String script_name) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name, "/run");
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, ScriptRunStatus.class,
					200, 202);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ScriptRunStatus runScriptVariables(String script_name,
			Map<String, String> variables) {
		if (variables == null)
			return runScript(script_name);
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name, "/run");
			Request request = Request.Post(uriBuilder.build());
			return execute(request, variables, msTimeOut,
					ScriptRunStatus.class, 200, 202);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ScriptRunStatus getRunStatus(String script_name, String run_id) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name,
					"/status/", run_id);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, ScriptRunStatus.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	public final static TypeReference<TreeMap<String, ScriptRunStatus>> MapRunStatusTypeRef = new TypeReference<TreeMap<String, ScriptRunStatus>>() {
	};

	@Override
	public Map<String, ScriptRunStatus> getRunsStatus(String script_name,
			Boolean local) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name,
					"/status");
			if (local != null && local)
				uriBuilder.setParameter("local", local.toString());
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, MapRunStatusTypeRef, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getRunOut(String script_name, String run_id) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name,
					"/status/", run_id, "/out");
			Request request = Request.Get(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getRunErr(String script_name, String run_id) {
		try {
			URIBuilder uriBuilder = getBaseUrl("/scripts/", script_name,
					"/status/", run_id, "/err");
			Request request = Request.Get(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			return HttpUtils.checkTextPlainEntity(response, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}
}
