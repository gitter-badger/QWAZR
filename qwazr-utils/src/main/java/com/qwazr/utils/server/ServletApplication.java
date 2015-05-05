/**
s * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.utils.server;

import io.undertow.server.session.SessionListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.List;

/**
 * Generic OpenSearchServer ServletApplication
 */
public abstract class ServletApplication {

	protected abstract List<ServletInfo> getServletInfos();

	protected abstract SessionListener getSessionListener();

	protected abstract String getContextPath();

	DeploymentInfo getDeploymentInfo() {
		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(getClass().getClassLoader())
				.setContextPath(getContextPath())
				.setDeploymentName(getClass().getName() + getContextPath());
		List<ServletInfo> servletInfos = getServletInfos();
		if (servletInfos != null)
			deploymentInfo.addServlets(servletInfos);
		SessionListener sessionListener = getSessionListener();
		if (sessionListener != null)
			deploymentInfo.addSessionListener(sessionListener);
		return deploymentInfo;
	}
}
