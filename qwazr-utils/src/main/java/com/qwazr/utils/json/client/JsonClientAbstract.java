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
package com.qwazr.utils.json.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.http.HttpResponseEntityException;
import com.qwazr.utils.json.JsonHttpResponseHandler;
import com.qwazr.utils.json.JsonMapper;

public abstract class JsonClientAbstract implements JsonClientInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(JsonClientAbstract.class);

	private final static int DEFAULT_TIMEOUT;

	static {
		String s = System
				.getProperty("com.qwazr.utils.json.client.default_timeout");
		DEFAULT_TIMEOUT = s == null ? 60000 : Integer.parseInt(s);
	}

	public final String url;
	protected final String scheme;
	protected final String host;
	protected final String fragment;
	protected final String path;
	protected final int port;
	protected final int msTimeOut;

	protected JsonClientAbstract(String url, Integer msTimeOut)
			throws URISyntaxException {
		this.url = url;
		URI u = new URI(url);
		String path = u.getPath();
		if (path != null && path.endsWith("/"))
			u = new URI(u.getScheme(), null, u.getHost(), u.getPort(),
					path.substring(0, path.length() - 1), u.getQuery(),
					u.getFragment());
		this.scheme = u.getScheme() == null ? "http" : u.getScheme();
		this.host = u.getHost();
		this.fragment = u.getFragment();
		this.path = u.getPath();
		this.port = u.getPort() == -1 ? 80 : u.getPort();
		this.msTimeOut = msTimeOut == null ? DEFAULT_TIMEOUT : msTimeOut;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final public <T> T execute(Request request, Object bodyObject,
			Integer msTimeOut, Class<T> jsonResultClass, int... expectedCodes)
			throws IOException {
		if (logger.isDebugEnabled())
			logger.debug(request.toString());
		if (msTimeOut == null)
			msTimeOut = this.msTimeOut;
		if (bodyObject != null)
			request = request.bodyString(
					JsonMapper.MAPPER.writeValueAsString(bodyObject),
					ContentType.APPLICATION_JSON);
		JsonHttpResponseHandler.JsonValueResponse<T> responseHandler = new JsonHttpResponseHandler.JsonValueResponse<T>(
				ContentType.APPLICATION_JSON, jsonResultClass, expectedCodes);
		return request.connectTimeout(msTimeOut).socketTimeout(msTimeOut)
				.addHeader("Accept", ContentType.APPLICATION_JSON.toString())
				.execute().handleResponse(responseHandler);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final public <T> T execute(Request request, Object bodyObject,
			Integer msTimeOut, TypeReference<T> typeRef, int... expectedCodes)
			throws IOException {
		if (logger.isDebugEnabled())
			logger.debug(request.toString());
		if (msTimeOut == null)
			msTimeOut = this.msTimeOut;
		if (bodyObject != null)
			request = request.bodyString(
					JsonMapper.MAPPER.writeValueAsString(bodyObject),
					ContentType.APPLICATION_JSON);
		return request
				.connectTimeout(msTimeOut)
				.socketTimeout(msTimeOut)
				.addHeader("accept", ContentType.APPLICATION_JSON.toString())
				.execute()
				.handleResponse(
						new JsonHttpResponseHandler.JsonValueTypeRefResponse<T>(
								ContentType.APPLICATION_JSON, typeRef,
								expectedCodes));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final public HttpResponse execute(Request request, Object bodyObject,
			Integer msTimeOut) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug(request.toString());
		if (msTimeOut == null)
			msTimeOut = this.msTimeOut;
		if (bodyObject != null) {
			if (bodyObject instanceof String)
				request = request.bodyString(bodyObject.toString(),
						ContentType.TEXT_PLAIN);
			else if (bodyObject instanceof InputStream)
				request = request.bodyStream((InputStream) bodyObject,
						ContentType.APPLICATION_OCTET_STREAM);
			else
				request = request.bodyString(
						JsonMapper.MAPPER.writeValueAsString(bodyObject),
						ContentType.APPLICATION_JSON);
		}
		return request.connectTimeout(msTimeOut).socketTimeout(msTimeOut)
				.execute().returnResponse();
	}

	final public <T> T commonServiceRequest(Request request, Object body,
			Integer msTimeout, Class<T> objectClass, int... expectedCodes) {
		try {
			return execute(request, body, msTimeOut, objectClass, expectedCodes);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	final public <T> T commonServiceRequest(Request request, Object body,
			Integer msTimeout, TypeReference<T> typeRef, int... expectedCodes) {
		try {
			return execute(request, body, msTimeOut, typeRef, expectedCodes);
		} catch (HttpResponseEntityException e) {
			throw e.getWebApplicationException();
		} catch (IOException e) {
			throw new WebApplicationException(e.getMessage(), e,
					Status.INTERNAL_SERVER_ERROR);
		}
	}

	public class UBuilder extends URIBuilder {

		/**
		 * Helper for URL building. The URL is built by concatening the url
		 * parameters given in the constructor and an array of pathes.
		 * 
		 * @param paths
		 *            An array of path
		 */
		public UBuilder(String... paths) {
			StringBuilder sb = new StringBuilder();
			if (path != null)
				sb.append(path);
			if (paths != null)
				for (String path : paths)
					if (path != null)
						sb.append(path);
			setScheme(scheme).setHost(host).setPort(port).setFragment(fragment);
			if (sb.length() > 0)
				setPath(sb.toString());
		}

		/**
		 * Add the query parameters if the object parameter is not null
		 * 
		 * @param object
		 *            an optional parameter
		 * @return the current UBuilder
		 */
		public UBuilder setParameterObject(String param, Object object) {
			if (object == null)
				return this;
			setParameter(param, object.toString());
			return this;
		}

		/**
		 * Set common parameters for QWAZR services
		 * 
		 * @param local
		 *            an optional local parameter
		 * @param msTimeout
		 *            an optional timeout parameter in milliseconds
		 * @return the current UBuilder
		 */
		public UBuilder setParameters(Boolean local, Integer msTimeout) {
			setParameterObject("local", local);
			setParameterObject("timeout", msTimeout);
			return this;
		}

		@Override
		public URI build() {
			try {
				return super.build();
			} catch (URISyntaxException e) {
				throw new WebApplicationException(e.getMessage(), e,
						Status.INTERNAL_SERVER_ERROR);
			}
		}

	}
}
