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
 **/
package com.qwazr;

import com.qwazr.ServerConfiguration.ServiceEnum;
import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.connectors.AbstractConnector;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.crawler.web.WebCrawlerServer;
import com.qwazr.crawler.web.service.WebCrawlerServiceImpl;
import com.qwazr.database.TableServer;
import com.qwazr.database.TableServiceImpl;
import com.qwazr.extractor.ExtractorServer;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.graph.GraphServer;
import com.qwazr.graph.GraphServiceImpl;
import com.qwazr.job.JobServer;
import com.qwazr.job.scheduler.SchedulerServiceImpl;
import com.qwazr.job.script.ScriptServiceImpl;
import com.qwazr.search.SearchServer;
import com.qwazr.search.index.IndexServiceImpl;
import com.qwazr.store.StoreServer;
import com.qwazr.store.data.StoreMasterDataService;
import com.qwazr.store.schema.StoreMasterSchemaService;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.webapps.WebappManagerServiceImpl;
import com.qwazr.webapps.WebappServer;
import com.qwazr.webapps.WebappServer.WebappApplication;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
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

public class Qwazr extends AbstractServer {

	static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

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
	}

	@Path("/")
	public static class WelcomeServiceImpl {

		@GET
		@Path("/")
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

	@Override
	public void defineOptions(Options options) {
		super.defineOptions(options);
		options.addOption(JobServer.THREADS_OPTION);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException {
		// Load the configuration
		serverConfiguration = new ServerConfiguration();
	}

	@Override
	public void load() throws IOException {

		File currentDataDir = getCurrentDataDir();

		ClusterServer.load(getWebServicePublicAddress(), currentDataDir);

		services.add("welcome", WelcomeServiceImpl.class);
		services.add("cluster", ClusterServiceImpl.class);

		if (ServiceEnum.extractor.isActive(serverConfiguration)) {
			ExtractorServer.loadParserManager();
			services.add(ServiceEnum.extractor.name(), ExtractorServiceImpl.class);
		}

		if (ServiceEnum.webapps.isActive(serverConfiguration)) {
			WebappServer.load(currentDataDir);
			services.add(ServiceEnum.webapps.name(), WebappManagerServiceImpl.class);
		}

		if (ServiceEnum.scripts.isActive(serverConfiguration)) {
			JobServer.loadScript(currentDataDir);
			services.add(ServiceEnum.scripts.name(), ScriptServiceImpl.class);
		}

		if (ServiceEnum.schedulers.isActive(serverConfiguration)) {
			JobServer.loadScheduler(currentDataDir, serverConfiguration.getSchedulerMaxThreads());
			services.add(ServiceEnum.schedulers.name(), SchedulerServiceImpl.class);
		}

		if (ServiceEnum.webcrawler.isActive(serverConfiguration)) {
			WebCrawlerServer.load(this);
			services.add(ServiceEnum.webcrawler.name(), WebCrawlerServiceImpl.class);
		}

		if (ServiceEnum.search.isActive(serverConfiguration)) {
			SearchServer.loadIndexManager(currentDataDir);
			services.add(ServiceEnum.search.name(), IndexServiceImpl.class);
		}

		if (ServiceEnum.graph.isActive(serverConfiguration)) {
			GraphServer.load(currentDataDir);
			services.add(ServiceEnum.graph.name(), GraphServiceImpl.class);
		}

		if (ServiceEnum.table.isActive(serverConfiguration)) {
			TableServer.load(currentDataDir);
			services.add(ServiceEnum.table.name(), TableServiceImpl.class);
		}

		if (ServiceEnum.store.isActive(serverConfiguration)) {
			StoreServer.load(currentDataDir);
			services.add(ServiceEnum.store.name(), StoreMasterDataService.class);
			services.add(ServiceEnum.store.name(), StoreMasterSchemaService.class);
		}

		ConnectorManager.load(currentDataDir);
		ToolsManager.load(currentDataDir);

	}

	@Override
	public Class<WebappApplication> getServletApplication() {
		if (ServiceEnum.webapps.isActive(serverConfiguration))
			return WebappApplication.class;
		return null;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) throws IOException {
		AbstractConnector connector = ConnectorManager.INSTANCE.get(realm);
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
			ClusterManager.INSTANCE.registerMe(server.services.keySet());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
