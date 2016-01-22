/**
 * s * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import io.undertow.server.session.SessionListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import javax.ws.rs.ApplicationPath;
import java.util.List;

/**
 * Generic ServletApplication
 */
@ApplicationPath("/")
public abstract class ServletApplication {

	private final String appPath;

	public ServletApplication() {
		appPath = this.getClass().getAnnotation(ApplicationPath.class).value();
	}

	protected abstract List<ServletInfo> getServletInfos();

	final String getApplicationPath() {
		return appPath;
	}

	final DeploymentInfo getDeploymentInfo() {
		DeploymentInfo deploymentInfo = Servlets.deployment().setClassLoader(getClass().getClassLoader())
						.setContextPath(appPath).setDefaultEncoding(java.nio.charset.Charset.defaultCharset().name())
						.setDeploymentName(getClass().getName() + appPath);
		List<ServletInfo> servletInfos = getServletInfos();
		if (servletInfos != null)
			deploymentInfo.addServlets(servletInfos);
		if (this instanceof SessionListener)
			deploymentInfo.addSessionListener((SessionListener) this);
		return deploymentInfo;
	}
}
