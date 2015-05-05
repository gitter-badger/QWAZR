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

	private final Status status;
	private final String message;
	private final Exception exception;

	public ServerException(Status status, String message, Exception exception) {
		super(message, exception);
		this.status = status != null ? status : Status.INTERNAL_SERVER_ERROR;
		this.message = message != null ? message : (exception == null ? null
				: exception.getMessage());
		this.exception = exception;
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

	public Integer getStatusCode() {
		return status == null ? null : status.getStatusCode();
	}

	@Override
	public String getMessage() {
		if (message != null)
			return message;
		if (status != null)
			return status.getReasonPhrase();
		return super.getMessage();
	}

	private Response getTextResponse() {
		return Response.status(status).type(MediaType.TEXT_PLAIN)
				.entity(message == null ? StringUtils.EMPTY : message).build();
	}

	private Response getJsonResponse() {
		return new JsonException(status, message, exception).toResponse();
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

	public static WebApplicationException getTextException(Exception e) {
		return getServerException(e).getTextException();
	}

	public static WebApplicationException getJsonException(Exception e) {
		return getServerException(e).getJsonException();

	}

}
