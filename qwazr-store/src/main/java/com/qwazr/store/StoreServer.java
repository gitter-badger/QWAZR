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
package com.qwazr.store;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.store.data.StoreDataManager;
import com.qwazr.store.data.StoreMasterDataService;
import com.qwazr.store.data.StoreNodeDataService;
import com.qwazr.store.schema.StoreMasterSchemaService;
import com.qwazr.store.schema.StoreSchemaManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class StoreServer extends AbstractServer {

	public final static String SERVICE_NAME_STORE = "store";

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9092;
		serverDefinition.mainJarPath = "qwazr-store.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private StoreServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class StoreApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(StoreNodeDataService.class);
			if (ClusterManager.INSTANCE.isMaster()) {
				classes.add(StoreMasterDataService.class);
				classes.add(StoreMasterSchemaService.class);
			}
			classes.add(ClusterServiceImpl.class);
			return classes;
		}
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

	public static void load(File dataDir) throws IOException {
		File storeDir = new File(dataDir, SERVICE_NAME_STORE);
		if (!storeDir.exists())
			storeDir.mkdir();
		StoreDataManager.load(storeDir);
		if (ClusterManager.INSTANCE.isMaster())
			StoreSchemaManager.load(storeDir);
	}

	@Override
	public void load() throws IOException {
		File dataDir = getCurrentDataDir();
		ClusterManager.load(getWebServicePublicAddress(), dataDir);
		load(dataDir);
	}

	public static void main(String[] args) throws IOException, ParseException, ServletException, InstantiationException,
					IllegalAccessException {
		new StoreServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME_STORE);
	}

	@Override
	protected Class<StoreApplication> getRestApplication() {
		return StoreApplication.class;
	}

	@Override
	protected Class<ServletApplication> getServletApplication() {
		return null;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) {
		return null;
	}
}
