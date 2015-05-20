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
package com.qwazr.store.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import com.qwazr.store.schema.StoreSchemaServiceInterface;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.http.HttpUtils;
import com.qwazr.utils.json.client.JsonClientAbstract;

public class StoreDataSingleClient extends JsonClientAbstract implements
		StoreDataServiceInterface {

	public static enum PrefixPath {

		name("/store/"), data("/store_local/");

		final String path;

		PrefixPath(String path) {
			this.path = path;
		}

	}

	private final PrefixPath prefixPath;

	public StoreDataSingleClient(String url, PrefixPath prefixPath,
			Integer msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
		this.prefixPath = prefixPath;
	}

	@Override
	public StoreFileResult getDirectory(String schemaName, String path,
			Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName, "/", path)
				.setParameters(null, msTimeout);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				StoreFileResult.class, 200);
	}

	@Override
	public Response getFile(String schemaName, String path, Integer msTimeout) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName, "/",
					path).setParameters(null, msTimeout);
			Request request = Request.Get(uBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok();
			StoreFileResult.buildHeaders(response, null, builder);
			builder.type(response.getEntity().getContentType().getValue());
			builder.entity(response.getEntity().getContent());
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response headFile(String schemaName, String path, Integer msTimeout) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName, "/",
					path).setParameters(null, msTimeout);
			URI uri = uBuilder.build();
			Request request = Request.Head(uri);
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok();
			StoreFileResult.buildHeaders(response, uri, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	final public StoreFileResult getDirectory(String schemaName,
			Integer msTimeout) {
		return getDirectory(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	final public Response getFile(String schemaName, Integer msTimeout) {
		return getFile(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	final public Response headFile(String schemaName, Integer msTimeout) {
		return headFile(schemaName, StringUtils.EMPTY, msTimeout);
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream, Long lastModified, Integer msTimeout,
			Integer target) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName, "/",
					path).setParameters(null, msTimeout);
			uBuilder.setParameterObject("last_modified", lastModified);
			uBuilder.setParameterObject("target", target);
			Request request = Request.Post(uBuilder.build());
			HttpResponse response = execute(request, inputStream, msTimeout);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("File created: " + path,
					MediaType.TEXT_PLAIN);
			StoreFileResult.buildHeaders(response, null, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response deleteFile(String schemaName, String path, Integer msTimeout) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName, "/",
					path).setParameters(null, msTimeout);
			Request request = Request.Delete(uBuilder.build());
			HttpResponse response = execute(request, null, msTimeOut);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok(response.getStatusLine()
					.getReasonPhrase(), MediaType.TEXT_PLAIN);
			StoreFileResult.buildHeaders(response, null, builder);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response createSchema(String schemaName, Integer msTimeout) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName)
					.setParameters(null, msTimeout);
			Request request = Request.Post(uBuilder.build());
			HttpResponse response = execute(request, null, msTimeout);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("Schema created: "
					+ schemaName, MediaType.TEXT_PLAIN);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Response deleteSchema(String schemaName, Integer msTimeout) {
		try {
			UBuilder uBuilder = new UBuilder(prefixPath.path, schemaName)
					.setParameters(null, msTimeout);
			Request request = Request.Delete(uBuilder.build());
			HttpResponse response = execute(request, null, msTimeout);
			HttpUtils.checkStatusCodes(response, 200);
			ResponseBuilder builder = Response.ok("Schema deleted: "
					+ schemaName, MediaType.TEXT_PLAIN);
			return builder.build();
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Set<String> getSchemas(Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(prefixPath.path).setParameters(null,
				msTimeout);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				StoreSchemaServiceInterface.SetStringTypeRef, 200);
	}
}
