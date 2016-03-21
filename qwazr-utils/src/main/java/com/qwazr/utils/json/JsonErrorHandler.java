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

import com.qwazr.utils.server.ServerException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class JsonErrorHandler implements HttpHandler {

	private final HttpHandler next;

	public JsonErrorHandler(final HttpHandler next) {
		this.next = next;
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		try {
			next.handleRequest(exchange);
		} catch (Exception exception) {
			if (!exchange.isResponseChannelAvailable())
				throw exception;
			if (!(ServerException.toJsonResponse(exception, exchange)))
				throw exception;
		}
	}

}
