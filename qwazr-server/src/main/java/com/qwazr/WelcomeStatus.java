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
 **/

package com.qwazr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qwazr.cluster.manager.ClusterManager;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WelcomeStatus {

	public final ImplementationStatus implementation;
	public final List<String> endpoints;

	WelcomeStatus(MultivaluedMap<String, Class<?>> services) {
		this.implementation = new ImplementationStatus();
		endpoints = new ArrayList<String>();
		services.forEach((s, classes) -> classes.forEach(aClass -> addService(endpoints, aClass)));
	}

	private void addService(List<String> endpoints, Class<?> clazz) {
		Path path = clazz.getAnnotation(Path.class);
		if (path == null) {
			Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces == null)
				return;
			for (Class<?> interfac : interfaces)
				if ((path = interfac.getAnnotation(Path.class)) != null)
					break;
		}
		if (path == null) {
			if (Qwazr.logger.isWarnEnabled())
				Qwazr.logger.warn("No PATH annotation for " + clazz.getName());
			return;
		}
		endpoints.add(ClusterManager.INSTANCE.myAddress + path.value());
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public class ImplementationStatus {

		public final String title;
		public final String vendor;
		public final String version;

		ImplementationStatus() {
			Package pkg = getClass().getPackage();
			title = pkg.getImplementationTitle();
			vendor = pkg.getImplementationVendor();
			version = pkg.getImplementationVersion();
		}
	}

}
