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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.qwazr.utils.json.JacksonConfig;
import io.undertow.io.Sender;
import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ExceptionHandler;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.Headers;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic RestApplication
 */
class RestApplication extends Application {

	@Override
	final public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(JacksonConfig.class);
		classes.add(JacksonJsonProvider.class);
		if (AbstractServer.INSTANCE != null && AbstractServer.INSTANCE.services != null)
			classes.addAll(AbstractServer.INSTANCE.services);
		return classes;
	}

	final static DeploymentInfo getDeploymentInfo() {
		DeploymentInfo deploymentInfo = Servlets.deployment().setClassLoader(RestApplication.class.getClassLoader())
				.setContextPath("/").setDeploymentName("REST");
		List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
		servletInfos.add(new ServletInfo("REST", ServletContainer.class)
				.addInitParam("javax.ws.rs.Application", RestApplication.class.getName()).setAsyncSupported(true)
				.addMapping("/*"));
		deploymentInfo.addServlets(servletInfos);
		return deploymentInfo;
	}

	public class JsonErrorHandler implements HttpHandler {

		private final HttpHandler next;

		public JsonErrorHandler(final HttpHandler next) {
			this.next = next;
		}

		@Override
		public void handleRequest(final HttpServerExchange exchange) throws Exception {
			if (!exchange.isResponseChannelAvailable()) {
				return false;
			}
			exchange.addDefaultResponseListener(new DefaultResponseListener() {
				@Override
				public boolean handleDefaultResponse(final HttpServerExchange exchange) {

					Set<Integer> codes = responseCodes;
					if (exchange.getResponseCode() == 500) {
						final String errorPage = "<html><head><title>Error</title></head><body>Internal Error</body></html>";
						exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + errorPage.length());
						exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
						Sender sender = exchange.getResponseSender();
						sender.send(errorPage);
						return true;
					}
					return false;
				}
			});
			next.handleRequest(exchange);
		}
	}

	public static class JSONExceptionHandler implements ExceptionHandler {

		@Override
		public boolean handleThrowable(HttpServerExchange exchange, ServletRequest request, ServletResponse response,
				Throwable throwable) {
			try {
				ServerException.toJsonResponse(throwable, (HttpServletResponse) response);
				return true;
			} catch (IOException e) {
				return false;
			}
		}
	}
}
