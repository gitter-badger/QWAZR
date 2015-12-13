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
package com.qwazr.utils.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public abstract class HttpResponseHandler<T> implements ResponseHandler<T> {

	private final ContentType expectedContentType;
	private final int[] expectedCodes;
	protected HttpEntity httpEntity;
	protected StatusLine statusLine;

	public HttpResponseHandler(ContentType expectedContentType, int... expectedCodes) {
		this.expectedContentType = expectedContentType;
		this.expectedCodes = expectedCodes;
	}

	@Override
	public T handleResponse(HttpResponse response) throws IOException {
		httpEntity = response.getEntity();
		statusLine = response.getStatusLine();
		if (expectedCodes != null && expectedCodes.length > 0)
			HttpUtils.checkStatusCodes(response, expectedCodes);
		if (expectedContentType != null)
			HttpUtils.checkIsEntity(response, expectedContentType);
		return null;
	}

	public Integer getStatusCode() {
		if (statusLine == null)
			return null;
		return statusLine.getStatusCode();
	}

	public ContentType getContentType() {
		if (httpEntity == null)
			return null;
		return ContentType.get(httpEntity);
	}

}
