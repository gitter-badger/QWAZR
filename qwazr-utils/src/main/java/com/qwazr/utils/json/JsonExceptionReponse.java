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
package com.qwazr.utils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qwazr.utils.ExceptionUtils;
import com.qwazr.utils.server.ServiceInterface;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;

@JsonInclude(Include.NON_EMPTY)
public class JsonExceptionReponse {

	public final String error;
	public final String reason_phrase;
	public final Integer status_code;
	public final String message;
	public final String exception;
	public final List<String> stackTraces;

	public JsonExceptionReponse(Status status, String message) {
		this.error = status == null ? null : status.name();
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		this.message = message;
		this.exception = null;
		this.stackTraces = null;
	}

	public JsonExceptionReponse(Status status, Throwable e) {
		this.error = status == null ? null : status.name();
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		Throwable cause = e == null ? null : ExceptionUtils.getRootCause(e);
		this.message = cause == null ? null : cause.getMessage();
		this.exception = cause == null ? null : cause.getClass().getName();
		this.stackTraces = cause == null ? null : ExceptionUtils.getStackTraces(cause);
	}

	public JsonExceptionReponse(Status status, String error, Throwable e) {
		this.error = error;
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		Throwable cause = e == null ? null : ExceptionUtils.getRootCause(e);
		this.message = cause == null ? null : cause.getMessage();
		this.exception = cause == null ? null : cause.getClass().getName();
		this.stackTraces = cause == null ? null : ExceptionUtils.getStackTraces(cause);
	}

	public JsonExceptionReponse(int status, String message) {
		this.error = null;
		this.reason_phrase = null;
		this.status_code = status;
		this.message = message;
		this.exception = null;
		this.stackTraces = null;
	}

	public Response toResponse() {
		try {
			String jsonMessage = JsonMapper.MAPPER.writeValueAsString(this);
			return Response.status(status_code).type(ServiceInterface.APPLICATION_JSON_UTF8).entity(jsonMessage)
							.build();
		} catch (JsonProcessingException e) {
			return Response.status(status_code).type(MediaType.TEXT_PLAIN).entity(message).build();
		}
	}

	public void toResponse(final HttpServletResponse response) throws IOException {
		try {
			final String jsonMessage = JsonMapper.MAPPER.writeValueAsString(this);
			response.setContentType(ServiceInterface.APPLICATION_JSON_UTF8);
			response.setCharacterEncoding("UTF-8");
			response.sendError(status_code, jsonMessage);
		} catch (JsonProcessingException e) {
			response.sendError(status_code, MediaType.TEXT_PLAIN);
		}
	}

	public boolean toResponse(final HttpServerExchange exchange) {
		final String jsonMessage;
		try {
			jsonMessage = JsonMapper.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return false;
		}
		exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + jsonMessage.length());
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ServiceInterface.APPLICATION_JSON_UTF8);
		Sender sender = exchange.getResponseSender();
		sender.send(jsonMessage);
		return true;
	}

}
