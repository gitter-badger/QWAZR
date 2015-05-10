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
package com.qwazr.utils.json.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.json.JsonHttpResponseHandler;
import com.qwazr.utils.json.JsonMapper;

public abstract class JsonClientAbstract implements JsonClientInterface {

	private static final Logger logger = LoggerFactory
			.getLogger(JsonClientAbstract.class);

	public final String url;
	protected final String scheme;
	protected final String host;
	protected final String fragment;
	protected final String path;
	protected final int port;
	protected final int msTimeOut;

	protected JsonClientAbstract(String url, int msTimeOut)
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
		this.msTimeOut = msTimeOut;
	}

	/**
	 * Helper for URL building. The URL is built by concatening the url
	 * parameters given in the constructor and an array of pathes.
	 * 
	 * @param paths
	 *            An array of path
	 * @return a prepared URIBuilder
	 * @throws URISyntaxException
	 *             if the final URI is not valid.
	 */
	public URIBuilder getBaseUrl(String... paths) throws URISyntaxException {
		StringBuilder sb = new StringBuilder();
		if (path != null)
			sb.append(path);
		if (paths != null)
			for (String path : paths)
				if (path != null)
					sb.append(path);
		URIBuilder uriBuilder = new URIBuilder().setScheme(scheme)
				.setHost(host).setPort(port).setFragment(fragment);
		if (sb.length() > 0)
			uriBuilder.setPath(sb.toString());
		return uriBuilder;
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
				.addHeader("accept", ContentType.APPLICATION_JSON.toString())
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
			else
				request = request.bodyString(
						JsonMapper.MAPPER.writeValueAsString(bodyObject),
						ContentType.APPLICATION_JSON);
		}
		return request.connectTimeout(msTimeOut).socketTimeout(msTimeOut)
				.execute().returnResponse();
	}

}
