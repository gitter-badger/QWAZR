/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.mavenplugin;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.http.HttpUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Mojo(name = "stop")
public class QwazrStopMojo extends AbstractMojo {

	@Parameter
	private String public_addr;

	@Parameter
	private Integer webservice_port;

	@Parameter
	private Boolean fault_tolerant;

	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();
		log.info("Stopping QWAZR");
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = HttpUtils.createHttpClient_AcceptsUntrustedCerts();
		try {
			URI uri = new URI("http", null, StringUtils.isEmpty(public_addr) ? "localhost" : public_addr,
							webservice_port == null ? 9091 : webservice_port, "/shutdown", null, null);
			log.info("Post HTTP Delete on: " + uri);
			response = httpClient.execute(new HttpDelete(uri));
			log.info("HTTP Status Code: " + response.getStatusLine().getStatusCode());
		} catch (IOException | URISyntaxException e) {
			if (fault_tolerant == null || fault_tolerant)
				log.warn(e.getMessage(), e);
			else
				throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			IOUtils.close(httpClient, response);
		}
	}
}
