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
 **/
package com.qwazr;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwarz.graph.GraphServer;
import com.qwarz.graph.GraphServiceImpl;
import com.qwazr.ServerConfiguration.ServiceEnum;
import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.crawler.web.WebCrawlerServer;
import com.qwazr.crawler.web.service.WebCrawlerServiceImpl;
import com.qwazr.extractor.ExtractorServer;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.job.JobServer;
import com.qwazr.job.scheduler.SchedulerServiceImpl;
import com.qwazr.job.script.ScriptServiceImpl;
import com.qwazr.search.SearchServer;
import com.qwazr.search.index.IndexServiceImpl;
import com.qwazr.store.StoreDataService;
import com.qwazr.store.StoreNameService;
import com.qwazr.store.StoreServer;
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.WebappServer;
import com.qwazr.webapps.WebappServer.WebappApplication;

public class Qwazr extends AbstractServer {

	private static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9090;
		serverDefinition.defaultWebServiceTcpPort = 9091;
		serverDefinition.mainJarPath = "qwazr.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	private final static String WEBAPPS_CONTEXT_PATH = "/";

	private final static String SERVER_YAML_NAME = "server.yaml";
	private static ServerConfiguration serverConfiguration = null;

	private final HashSet<String> services = new HashSet<String>();

	private Qwazr() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class QwazrApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(ClusterServiceImpl.class);
			if (ServiceEnum.extractor.isActive(serverConfiguration))
				classes.add(ExtractorServiceImpl.class);
			if (ServiceEnum.scripts.isActive(serverConfiguration))
				classes.add(ScriptServiceImpl.class);
			if (ServiceEnum.schedulers.isActive(serverConfiguration))
				classes.add(SchedulerServiceImpl.class);
			if (ServiceEnum.webcrawler.isActive(serverConfiguration))
				classes.add(WebCrawlerServiceImpl.class);
			if (ServiceEnum.search.isActive(serverConfiguration))
				classes.add(IndexServiceImpl.class);
			if (ServiceEnum.graph.isActive(serverConfiguration))
				classes.add(GraphServiceImpl.class);
			if (ServiceEnum.store.isActive(serverConfiguration)) {
				classes.add(StoreDataService.class);
				if (ClusterManager.INSTANCE.isMaster())
					classes.add(StoreNameService.class);
			}
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
		// Load the configuration file
		File serverConfigurationFile = new File(getCurrentDataDir(),
				SERVER_YAML_NAME);
		if (serverConfigurationFile.exists()
				&& serverConfigurationFile.isFile()) {
			logger.info("Load server configuration file: "
					+ serverConfigurationFile.getAbsolutePath());
			serverConfiguration = ServerConfiguration
					.getNewInstance(serverConfigurationFile);
		} else {
			logger.info("Load default server configuration");
			serverConfiguration = ServerConfiguration.getDefaultConfiguration();
		}
	}

	@Override
	public void load() throws IOException {

		File currentDataDir = getCurrentDataDir();

		ClusterServer.load(getWebServicePublicAddress(), currentDataDir, null);

		ConnectorManager.load(currentDataDir, null);
		ToolsManager.load(currentDataDir, null);

		if (ServiceEnum.extractor.isActive(serverConfiguration)) {
			ExtractorServer.loadParserManager();
			services.add(ServiceEnum.extractor.name());
		}

		if (ServiceEnum.webapps.isActive(serverConfiguration)) {
			WebappServer.load(WEBAPPS_CONTEXT_PATH, null, 1, currentDataDir);
			services.add(ServiceEnum.webapps.name());
		}

		if (ServiceEnum.scripts.isActive(serverConfiguration)) {
			JobServer.loadScript(currentDataDir);
			services.add(ServiceEnum.scripts.name());
		}

		if (ServiceEnum.schedulers.isActive(serverConfiguration)) {
			JobServer.loadScheduler(currentDataDir,
					serverConfiguration.getSchedulerMaxThreads());
			services.add(ServiceEnum.schedulers.name());
		}

		if (ServiceEnum.webcrawler.isActive(serverConfiguration)) {
			WebCrawlerServer.load(this);
			services.add(ServiceEnum.webcrawler.name());
		}

		if (ServiceEnum.search.isActive(serverConfiguration)) {
			SearchServer.loadIndexManager(currentDataDir);
			services.add(ServiceEnum.search.name());
		}

		if (ServiceEnum.graph.isActive(serverConfiguration)) {
			GraphServer.load(currentDataDir);
			services.add(ServiceEnum.graph.name());
		}

		if (ServiceEnum.store.isActive(serverConfiguration)) {
			StoreServer.load(currentDataDir);
			services.add(ServiceEnum.store.name());
		}

	}

	@Override
	public ServletApplication getServletApplication() {
		if (ServiceEnum.webapps.isActive(serverConfiguration))
			return new WebappApplication(WEBAPPS_CONTEXT_PATH);
		return null;
	}

	@Override
	public RestApplication getRestApplication() {
		return new QwazrApplication();
	}

	public static void main(String[] args) throws IOException,
			ServletException, ParseException {
		// Start the server
		Qwazr server = new Qwazr();
		server.start(args);
		// Register the services
		ClusterManager.INSTANCE.registerMe(server.services);
	}
}
