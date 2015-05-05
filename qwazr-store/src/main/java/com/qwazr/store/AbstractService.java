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
package com.qwazr.store;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

public class AbstractService {

	protected final static String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
	protected final static String TEXT_PLAIN_UTF8 = "text/plain;charset=UTF-8";

	protected static final ResponseBuilder responseStream(
			InputStream inputStream) {
		return Response.ok().entity(inputStream)
				.type(MediaType.APPLICATION_OCTET_STREAM);
	}

	protected static final ResponseBuilder responseJson(Object entity) {
		return Response.ok().entity(entity).type(APPLICATION_JSON_UTF8);
	}

	protected static final ResponseBuilder responseText(Status status,
			String text) {
		return Response.status(status).entity(text).type(MediaType.TEXT_PLAIN);
	}
}
