/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils.http;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;

public class HttpResponseEntityException extends HttpResponseException {

	private static final long serialVersionUID = 1958648159987063347L;

	private final String contentType;
	private final String contentMessage;

	public HttpResponseEntityException(HttpResponse response, String message) {
		super(getStatusCode(response.getStatusLine()), message);
		HttpEntity entity = response.getEntity();
		String cm = null;
		String ct = null;
		if (entity != null) {
			try {
				cm = IOUtils.toString(entity.getContent());
				Header header = entity.getContentType();
				if (header != null)
					ct = header.getValue();
			} catch (IllegalStateException | IOException e) {
			}
		}
		contentMessage = cm == null ? message : cm;
		contentType = ct == null ? MediaType.TEXT_PLAIN : ct;
	}

	private static int getStatusCode(StatusLine statusLine) {
		if (statusLine == null)
			return 0;
		return statusLine.getStatusCode();
	}

	public WebApplicationException getWebApplicationException() {
		int code = getStatusCode();
		if (code == 0)
			return new WebApplicationException(this);
		ResponseBuilder response = Response.status(code);
		if (contentMessage == null)
			return new WebApplicationException(this, response.build());
		response.type(contentType).entity(contentMessage);
		return new WebApplicationException(contentMessage, this, response.build());

	}

	public static HttpResponseEntityException findFirstCause(Throwable e) {
		if (e == null)
			return null;
		if (e instanceof HttpResponseEntityException)
			return (HttpResponseEntityException) e;
		return findFirstCause(e.getCause());
	}

}
