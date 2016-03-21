/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.server;

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.json.JsonExceptionReponse;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

public class ServerException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -6102827990391082335L;

	private final int statusCode;
	private final String message;

	public ServerException(Status status, String message, Throwable throwable) {
		super(message, throwable);
		if (status == null && throwable != null && throwable instanceof WebApplicationException) {
			WebApplicationException wae = (WebApplicationException) throwable;
			this.statusCode = wae.getResponse().getStatus();
		} else
			this.statusCode = status != null ? status.getStatusCode() : Status.INTERNAL_SERVER_ERROR.getStatusCode();
		if (StringUtils.isEmpty(message)) {
			if (throwable != null)
				message = throwable.getMessage();
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

	public ServerException(Throwable throwable) {
		this(null, null, throwable);
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
		return new JsonExceptionReponse(statusCode, message).toResponse();
	}

	private void toJsonResponse(HttpServletResponse response) throws IOException {
		new JsonExceptionReponse(statusCode, message).toResponse(response);
	}

	public WebApplicationException getTextException() {
		return new WebApplicationException(this, getTextResponse());
	}

	public WebApplicationException getJsonException() {
		return new WebApplicationException(this, getJsonResponse());
	}

	public static ServerException getServerException(Exception e) {
		if (e instanceof ServerException)
			return (ServerException) e;
		if (e instanceof WebApplicationException) {
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof ServerException)
				return (ServerException) e;
		}
		return new ServerException(e);
	}

	private static WebApplicationException checkCompatible(Exception e, MediaType expectedType) {
		if (!(e instanceof WebApplicationException))
			return null;
		WebApplicationException wae = (WebApplicationException) e;
		Response response = wae.getResponse();
		if (response == null)
			return null;
		if (!response.hasEntity())
			return null;
		MediaType mediaType = response.getMediaType();
		if (mediaType == null)
			return null;
		if (!expectedType.isCompatible(mediaType))
			return null;
		return wae;
	}

	public static WebApplicationException getTextException(Exception e) {
		WebApplicationException wae = checkCompatible(e, MediaType.TEXT_PLAIN_TYPE);
		if (wae != null)
			return wae;
		return getServerException(e).getTextException();
	}

	public static WebApplicationException getJsonException(Exception e) {
		WebApplicationException wae = checkCompatible(e, MediaType.APPLICATION_JSON_TYPE);
		if (wae != null)
			return wae;
		return getServerException(e).getJsonException();
	}

	public static void toJsonResponse(Throwable throwable, HttpServletResponse response) throws IOException {
		new ServerException(throwable).toJsonResponse(response);
	}
}
