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
package com.qwazr.utils.http;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ContentType;

import com.qwazr.utils.StringUtils;

public class HttpResponseEntityException extends HttpResponseException {

	private static final long serialVersionUID = 1958648159987063347L;

	private final HttpEntity entity;

	public HttpResponseEntityException(HttpResponse response, String message) {
		super(getStatusCode(response.getStatusLine()), setMessage(message,
				response.getEntity()));
		entity = response.getEntity();
	}

	private static int getStatusCode(StatusLine statusLine) {
		if (statusLine == null)
			return 0;
		return statusLine.getStatusCode();
	}

	private static String setMessage(String message, HttpEntity entity) {
		StringBuilder sb = new StringBuilder();
		if (message != null)
			sb.append(message);
		try {
			if (entity != null) {
				sb.append(' ');
				sb.append(IOUtils.toString(entity.getContent()));
			}
			ContentType contentType = ContentType.get(entity);
			if (contentType != null) {
				sb.append(" - ");
				sb.append(contentType.toString());
			}
		} catch (IllegalStateException | IOException e) {
		}
		return sb.toString();
	}

	public HttpEntity getEntity() {
		return entity;
	}

	public WebApplicationException getWebApplicationException(
			Object... additionalMessages) {
		String message = getMessage();
		if (additionalMessages != null)
			message = StringUtils.fastConcat(message, additionalMessages);
		int code = getStatusCode();
		if (code != 0)
			return new WebApplicationException(message, code);
		return new WebApplicationException(message, this);
	}

}
