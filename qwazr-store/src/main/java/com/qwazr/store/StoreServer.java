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
package com.qwazr.store;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.store.data.StoreMasterDataService;
import com.qwazr.store.data.StoreNodeDataService;
import com.qwazr.store.schema.StoreMasterSchemaService;
import com.qwazr.store.schema.StoreSchemaManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServletApplication;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executors;

public class StoreServer extends AbstractServer {

	public final static String SERVICE_NAME_STORE = "store";

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9092;
		serverDefinition.mainJarPath = "qwazr-store.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private StoreServer() {
		super(serverDefinition, Executors.newCachedThreadPool());
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
	}

	public static void load(File dataDir, Collection<Class<? extends ServiceInterface>> classes) throws IOException {
		File storeDir = new File(dataDir, SERVICE_NAME_STORE);
		if (!storeDir.exists())
			storeDir.mkdir();
		if (ClusterManager.INSTANCE.isMaster()) {
			classes.add(StoreMasterDataService.class);
			classes.add(StoreMasterSchemaService.class);
			StoreSchemaManager.load(storeDir);
		}
		if (classes != null)
			classes.add(StoreNodeDataService.class);
	}

	@Override
	public ServletApplication load(Collection<Class<? extends ServiceInterface>> services) throws IOException {
		services.add(ClusterManager.load(executorService, getWebServicePublicAddress(), null));
		load(getCurrentDataDir(), services);
		return null;
	}

	public static void main(String[] args) throws IOException, ParseException, ServletException, InstantiationException,
					IllegalAccessException {
		new StoreServer().start(args, true);
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) {
		return null;
	}
}
