/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils.server;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.qwazr.utils.json.JacksonConfig;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.plugins.providers.jackson.Jackson2JsonpInterceptor;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic RestApplication
 */
public class RestApplication extends Application {

	public final static String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(JacksonConfig.class);
		classes.add(JacksonJsonProvider.class);
		classes.add(Jackson2JsonpInterceptor.class);
		return classes;
	}

	DeploymentInfo getDeploymentInfo() {
		ApplicationPath appPath = getClass().getAnnotation(ApplicationPath.class);
		DeploymentInfo deploymentInfo = Servlets.deployment().setClassLoader(this.getClass().getClassLoader())
						.setContextPath(appPath.value()).setDeploymentName("REST");
		List<ServletInfo> servletInfos = new ArrayList<ServletInfo>();
		servletInfos.add(new ServletInfo("REST", HttpServletDispatcher.class)
						.addInitParam("javax.ws.rs.Application", getClass().getName()).setAsyncSupported(true)
						.addMapping("/*"));
		deploymentInfo.addServlets(servletInfos);
		return deploymentInfo;
	}
}
