/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.graph;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServerConfiguration;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServletApplication;
import io.undertow.security.idm.IdentityManager;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executors;

public class GraphServer extends AbstractServer<ServerConfiguration> {

	private GraphServer() {
		super(Executors.newCachedThreadPool(), new ServerConfiguration());
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) {
		return null;
	}

	@Override
	public ServletApplication load(Collection<Class<? extends ServiceInterface>> services) throws IOException {
		File dataDir = getCurrentDataDir();
		services.add(ClusterManager.load(executorService, getWebServicePublicAddress(), null));
		services.add(GraphManager.load(executorService, dataDir));
		return null;
	}

	public static void main(String[] args) throws IOException, ServletException, ReflectiveOperationException {
		new GraphServer().start(true);
	}

}