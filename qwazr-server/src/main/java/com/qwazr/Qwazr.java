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

import com.qwazr.ServerConfiguration.ServiceEnum;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterNodeJson;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.connectors.AbstractConnector;
import com.qwazr.connectors.ConnectorManagerImpl;
import com.qwazr.connectors.ConnectorsServiceImpl;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.database.TableManager;
import com.qwazr.extractor.ParserManager;
import com.qwazr.graph.GraphManager;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.semaphores.SemaphoresManager;
import com.qwazr.store.StoreServer;
import com.qwazr.store.data.StoreMasterDataService;
import com.qwazr.store.schema.StoreMasterSchemaService;
import com.qwazr.tools.ToolsManagerImpl;
import com.qwazr.tools.ToolsServiceImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.webapps.WebappApplication;
import com.qwazr.webapps.transaction.WebappManager;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Qwazr extends AbstractServer {

	static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

	private final ExecutorService executorService;

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9090;
		serverDefinition.defaultWebServiceTcpPort = 9091;
		serverDefinition.mainJarPath = "qwazr.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private static ServerConfiguration serverConfiguration = null;

	private static final MultivaluedMap<String, Class<?>> services = new MultivaluedHashMap<>();

	private Qwazr() {
		super(serverDefinition);
		executorService = Executors.newCachedThreadPool();
	}

	@Path("/")
	public static class WelcomeServiceImpl {

		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public WelcomeStatus welcome() {
			return new WelcomeStatus(services);
		}

	}

	@ApplicationPath("/")
	public static class QwazrApplication extends RestApplication {

		@Override
		public synchronized Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			services.forEach((s, classes1) -> classes.addAll(classes1));
			return classes;
		}
	}

	public final static Option THREADS_OPTION = new Option("t", "maxthreads", true, "The maximum of threads");

	@Override
	public void defineOptions(Options options) {
		super.defineOptions(options);
		options.addOption(THREADS_OPTION);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException {
		// Load the configuration
		serverConfiguration = new ServerConfiguration();
	}

	@Override
	public void load() throws IOException {

		File currentDataDir = getCurrentDataDir();

		ClusterManager.load(getWebServicePublicAddress(), currentDataDir);

		services.add("welcome", WelcomeServiceImpl.class);
		services.add("cluster", ClusterServiceImpl.class);

		if (ServiceEnum.extractor.isActive(serverConfiguration)) {
			services.add(ServiceEnum.extractor.name(), ParserManager.load());
		}

		if (ServiceEnum.webapps.isActive(serverConfiguration)) {
			services.add(ServiceEnum.webapps.name(), WebappManager.load(executorService, currentDataDir));
		}

		if (ServiceEnum.semaphores.isActive(serverConfiguration)) {
			services.add(ServiceEnum.semaphores.name(), SemaphoresManager.load(executorService));
		}

		if (ServiceEnum.scripts.isActive(serverConfiguration)) {
			services.add(ServiceEnum.scripts.name(), ScriptManager.load(executorService, currentDataDir));
		}

		if (ServiceEnum.webcrawler.isActive(serverConfiguration)) {
			services.add(ServiceEnum.webcrawler.name(), WebCrawlerManager.load());
		}

		if (ServiceEnum.search.isActive(serverConfiguration)) {
			services.add(ServiceEnum.search.name(), IndexManager.load(executorService, currentDataDir));
		}

		if (ServiceEnum.graph.isActive(serverConfiguration)) {
			services.add(ServiceEnum.graph.name(), GraphManager.load(executorService, currentDataDir));
		}

		if (ServiceEnum.table.isActive(serverConfiguration)) {
			services.add(ServiceEnum.table.name(), TableManager.load(executorService, currentDataDir));
		}

		if (ServiceEnum.store.isActive(serverConfiguration)) {
			StoreServer.load(currentDataDir);
			services.add(ServiceEnum.store.name(), StoreMasterDataService.class);
			services.add(ServiceEnum.store.name(), StoreMasterSchemaService.class);
		}

		ConnectorManagerImpl.load(currentDataDir);
		services.add(ServiceEnum.connectors.name(), ConnectorsServiceImpl.class);
		ToolsManagerImpl.load(currentDataDir);
		services.add(ServiceEnum.tools.name(), ToolsServiceImpl.class);

		// Scheduler is last, because it may immediatly execute a scripts
		if (ServiceEnum.schedulers.isActive(serverConfiguration)) {
			services.add(ServiceEnum.schedulers.name(),
					SchedulerManager.load(currentDataDir, serverConfiguration.getSchedulerMaxThreads()));
		}
	}

	@Override
	public Class<WebappApplication> getServletApplication() {
		if (ServiceEnum.webapps.isActive(serverConfiguration))
			return WebappApplication.class;
		return null;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) throws IOException {
		AbstractConnector connector = ConnectorManagerImpl.getInstance().get(realm);
		if (connector == null)
			throw new IOException("No realm connector with this name: " + realm);
		if (!(connector instanceof IdentityManager))
			throw new IOException("This is a not a realm connector: " + realm);
		return (IdentityManager) connector;
	}

	@Override
	public Class<QwazrApplication> getRestApplication() {
		return QwazrApplication.class;
	}

	public static void main(String[] args) {
		// Start the server
		try {
			Qwazr server = new Qwazr();
			server.start(args);
			// Register the services
			ClusterManager.getInstance().registerMe(
					new ClusterNodeJson(ClusterManager.getInstance().myAddress, services.keySet(),
							serverConfiguration.groups));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
