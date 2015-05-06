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
package com.qwazr.store;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.store.store.StoreManager;
import com.qwazr.store.store.StoreService;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;

public class StoreServer extends AbstractServer {

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9092;
		serverDefinition.mainJarPath = "qwazr-store.jar";
		serverDefinition.defaultDataDirPath = "qwazr/store";
	}

	private StoreServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class StoreApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(StoreService.class);
			classes.add(ClusterServiceImpl.class);
			return classes;
		}
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

	public static void load(AbstractServer server, File storeDir)
			throws IOException {
		StoreManager.load(storeDir);
	}

	@Override
	public void load() throws IOException {
		File dataDir = getCurrentDataDir();
		ClusterManager.load(getWebServicePublicAddress(), dataDir, null);
		load(this, dataDir);
	}

	public static void main(String[] args) throws IOException, ParseException,
			ServletException {
		new StoreServer().start(args);
	}

	@Override
	protected RestApplication getRestApplication() {
		return new StoreApplication();
	}

	@Override
	protected ServletApplication getServletApplication() {
		return null;
	}
}
