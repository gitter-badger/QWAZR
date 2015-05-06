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
package com.qwazr.utils.json;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qwazr.utils.ExceptionUtils;

@JsonInclude(Include.NON_EMPTY)
public class JsonException {

	public final String error;
	public final String reason_phrase;
	public final Integer status_code;
	public final String message;
	public final String exception;
	public final List<String> stackTraces;

	public JsonException(Status status, String message) {
		this.error = status == null ? null : status.name();
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		this.message = message;
		this.exception = null;
		this.stackTraces = null;
	}

	public JsonException(Status status, Exception e) {
		this.error = status == null ? null : status.name();
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		Throwable cause = e == null ? null : ExceptionUtils.getRootCause(e);
		this.message = cause == null ? null : cause.getMessage();
		this.exception = cause == null ? null : cause.getClass().getName();
		this.stackTraces = cause == null ? null : ExceptionUtils
				.getStackTraces(cause);
	}

	public JsonException(Status status, String error, Exception e) {
		this.error = error;
		this.reason_phrase = status == null ? null : status.getReasonPhrase();
		this.status_code = status == null ? null : status.getStatusCode();
		Throwable cause = e == null ? null : ExceptionUtils.getRootCause(e);
		this.message = cause == null ? null : cause.getMessage();
		this.exception = cause == null ? null : cause.getClass().getName();
		this.stackTraces = cause == null ? null : ExceptionUtils
				.getStackTraces(cause);
	}

	public JsonException(int status, String message) {
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
			return Response.status(status_code)
					.type(MediaType.APPLICATION_JSON).entity(jsonMessage)
					.build();
		} catch (JsonProcessingException e) {
			return Response.status(status_code).type(MediaType.TEXT_PLAIN)
					.entity(message).build();
		}
	}

}