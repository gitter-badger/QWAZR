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
 */
package com.qwazr.store;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;

public class StoreSingleClient extends JsonClientAbstract implements
		StoreServiceInterface {

	static enum PrefixPath {

		name("/store/"), data("/store_local/");

		final String path;

		PrefixPath(String path) {
			this.path = path;
		}
	}

	private final PrefixPath prefixPath;

	StoreSingleClient(String url, PrefixPath prefixPath, int msTimeOut)
			throws URISyntaxException {
		super(url, msTimeOut);
		this.prefixPath = prefixPath;
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		// TODO redirect!
		return null;
	}

	@Override
	public Response getFile(String schemaName, Integer msTimeout) {
		return getFile(schemaName, "/", msTimeout);
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getBaseUrl(prefixPath.path, schemaName,
					"/", path);
			if (msTimeout != null)
				uriBuilder.setParameter("timeout", msTimeout.toString());
			Request request = Request.Head(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			return StoreFileResult.buildHeaders(response, Response.ok())
					.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		try {
			URIBuilder uriBuilder = getBaseUrl(prefixPath.path, schemaName,
					"/", path);
			if (lastModified != null)
				uriBuilder.setParameter("last_modified",
						lastModified.toString());
			if (msTimeout != null)
				uriBuilder.setParameter("timeout", msTimeout.toString());
			if (target != null)
				uriBuilder.setParameter("target", target.toString());
			Request request = Request.Post(uriBuilder.build());
			HttpResponse response = execute(request, inputStream, msTimeout);
			HttpUtils.checkStatusCodes(response, 200);
			return StoreFileResult.buildHeaders(response,
					Response.ok("File created: " + path, MediaType.TEXT_PLAIN))
					.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getBaseUrl(prefixPath.path, schemaName,
					"/", path);
			if (msTimeout != null)
				uriBuilder.setParameter("timeout", msTimeout.toString());
			Request request = Request.Delete(uriBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			return StoreFileResult.buildHeaders(
					response,
					Response.ok(response.getStatusLine().getReasonPhrase(),
							MediaType.TEXT_PLAIN)).build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	private URIBuilder getSchemaBaseUrl(String schemaName, Boolean local,
			Integer msTimeout) throws URISyntaxException {
		URIBuilder uriBuilder = getBaseUrl(prefixPath.path, schemaName);
		if (local != null)
			uriBuilder.setParameter("local", local.toString());
		if (msTimeout != null)
			uriBuilder.setParameter("timeout", msTimeout.toString());
		return uriBuilder;
	}

	public final static TypeReference<TreeSet<String>> SetStringTypeRef = new TypeReference<TreeSet<String>>() {
	};

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getSchemaBaseUrl(null, local, msTimeout);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut, SetStringTypeRef, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getSchemaBaseUrl(schemaName, local,
					msTimeout);
			Request request = Request.Get(uriBuilder.build());
			return execute(request, null, msTimeOut,
					StoreSchemaDefinition.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDef, Boolean local, Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getSchemaBaseUrl(schemaName, local,
					msTimeout);
			Request request = Request.Post(uriBuilder.build());
			return execute(request, schemaDef, msTimeOut,
					StoreSchemaDefinition.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		try {
			URIBuilder uriBuilder = getSchemaBaseUrl(schemaName, local,
					msTimeout);
			Request request = Request.Delete(uriBuilder.build());
			return execute(request, null, msTimeOut,
					StoreSchemaDefinition.class, 200);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (URISyntaxException | IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

}
