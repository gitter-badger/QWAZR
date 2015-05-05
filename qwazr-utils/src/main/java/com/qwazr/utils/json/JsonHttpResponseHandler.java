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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.qwazr.utils.http.HttpResponseHandler;

public class JsonHttpResponseHandler {

	public static class JsonTreeResponse extends HttpResponseHandler<JsonNode> {

		public JsonTreeResponse(ContentType expectedContentType,
				int... expectedCodes) {
			super(expectedContentType, expectedCodes);
		}

		@Override
		public JsonNode handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			super.handleResponse(response);
			return JsonMapper.MAPPER.readTree(httpEntity.getContent());
		}
	}

	public static class JsonValueResponse<T> extends HttpResponseHandler<T> {

		private final Class<T> jsonClass;

		public JsonValueResponse(ContentType expectedContentType,
				Class<T> jsonClass, int... expectedCodes) {
			super(expectedContentType, expectedCodes);
			this.jsonClass = jsonClass;
		}

		@Override
		public T handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			super.handleResponse(response);
			return JsonMapper.MAPPER.readValue(httpEntity.getContent(),
					jsonClass);
		}
	}

	public static class JsonValueTypeRefResponse<T> extends
			HttpResponseHandler<T> {

		private final TypeReference<T> typeReference;

		public JsonValueTypeRefResponse(ContentType expectedContentType,
				TypeReference<T> typeReference, int... expectedCodes) {
			super(expectedContentType, expectedCodes);
			this.typeReference = typeReference;
		}

		@Override
		public T handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			super.handleResponse(response);
			return JsonMapper.MAPPER.readValue(httpEntity.getContent(),
					typeReference);
		}
	}
}
