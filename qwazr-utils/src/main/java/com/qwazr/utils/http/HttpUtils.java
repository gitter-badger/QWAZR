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

import com.qwazr.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpUtils {

	/**
	 * Check if the statusLine code returned the expected status code.
	 *
	 * @param response      The response to evaluate
	 * @param expectedCodes An array of status code
	 * @return the returned code
	 * @throws ClientProtocolException if the response is empty or the status line is empty
	 */
	public static Integer checkStatusCodes(HttpResponse response, int... expectedCodes) throws ClientProtocolException {
		if (response == null)
			throw new ClientProtocolException("No response");
		StatusLine statusLine = response.getStatusLine();
		if (statusLine == null)
			throw new ClientProtocolException("Response does not contains any status");
		int statusCode = statusLine.getStatusCode();
		if (expectedCodes == null)
			return null;
		for (int code : expectedCodes)
			if (code == statusCode)
				return code;
		throw new HttpResponseEntityException(response,
						StringUtils.fastConcat("Unexpected HTTP status code: ", statusCode));
	}

	/**
	 * Check if the entity has the expected mime type. The charset is not
	 * checked.
	 *
	 * @param response            The response to check
	 * @param expectedContentType The expected content type
	 * @return the entity from the response body
	 * @throws ClientProtocolException if the response does not contains any entity
	 */
	public static HttpEntity checkIsEntity(HttpResponse response, ContentType expectedContentType)
					throws ClientProtocolException {
		if (response == null)
			throw new ClientProtocolException("No response");
		HttpEntity entity = response.getEntity();
		if (entity == null)
			throw new ClientProtocolException("Response does not contains any content entity");
		if (expectedContentType == null)
			return entity;
		ContentType contentType = ContentType.get(entity);
		if (contentType == null)
			throw new HttpResponseEntityException(response, "Unknown content type");
		if (!expectedContentType.getMimeType().equals(contentType.getMimeType()))
			throw new HttpResponseEntityException(response,
							StringUtils.fastConcat("Wrong content type: ", contentType.getMimeType()));
		return entity;
	}

	/**
	 * Return a string with the content of any TEXT/PLAIN entity
	 *
	 * @param response      The response to check
	 * @param expectedCodes The expected content type
	 * @return the entity content
	 * @throws IllegalStateException if the ob
	 * @throws IOException           if any I/O error occurs
	 */
	public static String checkTextPlainEntity(HttpResponse response, int... expectedCodes) throws IOException {
		HttpUtils.checkStatusCodes(response, expectedCodes);
		HttpEntity entity = HttpUtils.checkIsEntity(response, ContentType.TEXT_PLAIN);
		Header header = entity.getContentEncoding();
		String encoding = header == null ? null : header.getValue();
		if (encoding == null)
			return IOUtils.toString(entity.getContent());
		else
			return IOUtils.toString(entity.getContent(), encoding);
	}

	public static HttpClient createHttpClient_AcceptsUntrustedCerts()
					throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
		HttpClientBuilder b = HttpClientBuilder.create();

		// setup a Trust Strategy that allows all certificates.
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				return true;
			}
		}).build();
		b.setSSLContext(sslContext);

		// don't check Hostnames, either.
		//      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
		HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

		// here's the special part:
		//      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
		//      -- and create a Registry, to register it.
		//
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		b.setSSLSocketFactory(sslSocketFactory);

		// finally, build the HttpClient;
		//      -- done!
		HttpClient client = b.build();
		return client;
	}
}
