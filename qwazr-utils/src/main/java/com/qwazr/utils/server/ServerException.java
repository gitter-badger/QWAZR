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
package com.qwazr.utils.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.json.JsonException;

public class ServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6102827990391082335L;

	private final int statusCode;
	private final String message;

	public ServerException(Status status, String message, Exception exception) {
		super(message, exception);
		if (status == null && exception != null
				&& exception instanceof WebApplicationException) {
			WebApplicationException wae = (WebApplicationException) exception;
			this.statusCode = wae.getResponse().getStatus();
		} else
			this.statusCode = status != null ? status.getStatusCode()
					: Status.INTERNAL_SERVER_ERROR.getStatusCode();
		if (StringUtils.isEmpty(message)) {
			if (exception != null)
				message = exception.getMessage();
			if (StringUtils.isEmpty(message) && status != null)
				message = status.getReasonPhrase();
		}
		this.message = message;
	}

	public ServerException(Status status) {
		this(status, status.getReasonPhrase(), null);
	}

	public ServerException(Status status, String message) {
		this(status, message, null);
	}

	public ServerException(String message, Exception exception) {
		this(null, message, exception);
	}

	public ServerException(String message) {
		this(null, message, null);
	}

	public ServerException(Exception exception) {
		this(null, null, exception);
	}

	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public String getMessage() {
		if (message != null)
			return message;
		return super.getMessage();
	}

	private Response getTextResponse() {
		return Response.status(statusCode).type(MediaType.TEXT_PLAIN)
				.entity(message == null ? StringUtils.EMPTY : message).build();
	}

	private Response getJsonResponse() {
		return new JsonException(statusCode, message).toResponse();
	}

	public WebApplicationException getTextException() {
		return new WebApplicationException(getTextResponse());
	}

	public WebApplicationException getJsonException() {
		return new WebApplicationException(getJsonResponse());
	}

	public static ServerException getServerException(Exception e) {
		if (e instanceof ServerException)
			return (ServerException) e;
		return new ServerException(e);
	}

	public static WebApplicationException checkCompatibleException(Exception e,
			MediaType type) {
		if (!(e instanceof WebApplicationException))
			return null;
		WebApplicationException wae = (WebApplicationException) e;
		if (type == null)
			return wae;
		Response r = wae.getResponse();
		MediaType mt = r.getMediaType();
		if (r != null && mt != null && type.isCompatible(mt))
			return wae;
		return null;
	}

	public static WebApplicationException getTextException(Exception e) {
		WebApplicationException wae = checkCompatibleException(e,
				MediaType.TEXT_PLAIN_TYPE);
		if (wae != null)
			return wae;
		return getServerException(e).getTextException();
	}

	public static WebApplicationException getJsonException(Exception e) {
		WebApplicationException wae = checkCompatibleException(e,
				MediaType.APPLICATION_JSON_TYPE);
		if (wae != null)
			return wae;
		return getServerException(e).getJsonException();
	}
}
