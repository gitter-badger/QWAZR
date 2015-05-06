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
import com.qwazr.tools.ToolsManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.WebappServer;
import com.qwazr.webapps.WebappServer.WebappApplication;

public class Qwazr extends AbstractServer {

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9090;
		serverDefinition.defaultWebServiceTcpPort = 9091;
		serverDefinition.mainJarPath = "qwazr.jar";
		serverDefinition.defaultDataDirPath = "qwazr";
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
			return classes;
		}
	}

	private File subDir(File dataDir, String name) throws IOException {
		File dir = new File(dataDir, name);
		if (!dir.exists())
			dir.mkdir();
		if (!dir.isDirectory())
			throw new IOException(
					"The configuration directory does not exist or cannot be created: "
							+ dir.getName());
		return dir;
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
				&& serverConfigurationFile.isFile())
			serverConfiguration = ServerConfiguration
					.getNewInstance(serverConfigurationFile);

		serverConfiguration = ServerConfiguration.getDefaultConfiguration();
	}

	@Override
	public void load() throws IOException {

		File currentDataDir = getCurrentDataDir();

		ClusterServer.load(this, subDir(currentDataDir, "cluster"), null, null);

		ConnectorManager.load(this, currentDataDir, null);
		ToolsManager.load(this, currentDataDir, null);

		if (ServiceEnum.extractor.isActive(serverConfiguration)) {
			ExtractorServer.load(this, subDir(currentDataDir, "extractor"),
					null);
			services.add(ServiceEnum.extractor.name());
		}

		if (ServiceEnum.webapps.isActive(serverConfiguration)) {
			WebappServer.load(WEBAPPS_CONTEXT_PATH, null, 1,
					subDir(currentDataDir, "webapps"));
			services.add(ServiceEnum.extractor.name());
		}

		if (ServiceEnum.scripts.isActive(serverConfiguration)) {
			JobServer.loadScript(this);
			services.add(ServiceEnum.scripts.name());
		}

		if (ServiceEnum.schedulers.isActive(serverConfiguration)) {
			JobServer.loadScheduler(this,
					serverConfiguration.getSchedulerMaxThreads());
			services.add(ServiceEnum.schedulers.name());
		}

		if (ServiceEnum.webcrawler.isActive(serverConfiguration)) {
			WebCrawlerServer.load(this);
			services.add(ServiceEnum.webcrawler.name());
		}

		if (ServiceEnum.search.isActive(serverConfiguration)) {
			SearchServer.loadIndexManager(this);
			services.add(ServiceEnum.search.name());
		}

		if (ServiceEnum.graph.isActive(serverConfiguration)) {
			GraphServer.load(subDir(currentDataDir, "graph"));
			services.add(ServiceEnum.graph.name());
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
