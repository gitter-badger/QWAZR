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
package com.qwazr.utils.json.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

public interface JsonClientInterface {

	/**
	 * Execute an HTTP request returning the expected object.
	 *
	 * @param request         the HTTP request to execute
	 * @param bodyObject      an optional object for the body.
	 * @param msTimeOut       an optional timeout in milliseconds
	 * @param jsonResultClass The class of the returned object
	 * @param expectedCodes   The expected HTTP status code(s)
	 * @param <T>             The type of the returned object
	 * @return An instance of the expected class
	 * @throws IOException if any IO error occur
	 */
	public <T> T execute(Request request, Object bodyObject, Integer msTimeOut, Class<T> jsonResultClass,
					int... expectedCodes) throws IOException;

	/**
	 * Execute an HTTP request returning an objet of the expected type
	 * reference.
	 *
	 * @param request       the HTTP request to execute
	 * @param msTimeOut     an optional timeout in milliseconds
	 * @param bodyObject    an optional object for the body
	 * @param typeRef       the TypeRef of the returned object
	 * @param expectedCodes The expected HTTP status code(s)
	 * @param <T>           The type of the returned object
	 * @return An instance of the expected type
	 * @throws IOException in case of IO error
	 */
	public <T> T execute(Request request, Object bodyObject, Integer msTimeOut, TypeReference<T> typeRef,
					int... expectedCodes) throws IOException;

	/**
	 * @param request       the HTTP request to execute
	 * @param bodyObject    an optional object for the body
	 * @param msTimeOut     an optional timeout in milliseconds
	 * @param expectedCodes The expected HTTP status code(s)
	 * @return a new JsonNode object
	 * @throws IOException in case of IO error
	 */
	public JsonNode execute(Request request, Object bodyObject, Integer msTimeOut, int... expectedCodes)
					throws IOException;

	/**
	 * Execute an HTTP request. The bodyObject is sent as payload if it is not
	 * null. If it is a String object, it is send as PLAIN/TEXT. If it is
	 * another object, it is serialized in JSON format.
	 *
	 * @param request    A preconfigured HTTP request
	 * @param msTimeOut  The time out in milliseconds. If null, the default value is
	 *                   used
	 * @param bodyObject The body of the request (payload)
	 * @return the HTTP response
	 * @throws IOException in case of any IO error
	 */
	public HttpResponse execute(Request request, Object bodyObject, Integer msTimeOut) throws IOException;

}
