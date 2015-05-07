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
package com.qwazr.utils.server;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

/**
 * Generic OpenSearchServer REST server
 */
public abstract class AbstractServer {

	/**
	 * Standard help option
	 */
	public final static Option HELP_OPTION = new Option("h", "help", false,
			"print this message");

	/**
	 * The user can change the TCP listening port
	 */
	public final static Option WEBAPP_TCP_PORT_OPTION = new Option("wp",
			"webapp-port", true, "TCP port for web application");

	/**
	 * The user can change the TCP listening port
	 */
	public final static Option WEBSERVICE_TCP_PORT_OPTION = new Option("sp",
			"webservice-port", true, "TCP port for the web service");

	/**
	 * Set the listening host or IP address
	 */
	public final static Option LISTEN_ADDRESS_OPTION = new Option("l",
			"listen", true, "Listening hostname or IP address");

	/**
	 * Set the public address (in case of NAT)
	 */
	public final static Option PUBLIC_ADDRESS_OPTION = new Option("a",
			"public-address", true,
			"The public hostname or IP address for node communication");

	/**
	 * Set the data directory
	 */
	public final static Option DATADIR_OPTION = new Option("d", "datadir",
			true, "Data directory");

	public static class ServerDefinition {
		/**
		 * The default hostname. Could be 0.0.0.0 or localhost
		 */
		public String defaultHostname = "0.0.0.0";

		/**
		 * The default TCP listening port for the Web application (servlet)
		 */
		public int defaultWebApplicationTcpPort = 9090;

		/**
		 * The default TCP listening port for the Web service
		 */
		public int defaultWebServiceTcpPort = 9091;

		/**
		 * The path of the main jar (passed in the command line)
		 */
		public String mainJarPath;

		/**
		 * The default name for the data directory
		 */
		public String defaultDataDirName;

		/**
		 * The name of the optional sub directories
		 */
		public String[] subDirectoryNames;
	}

	/**
	 * The initial parameters
	 */
	private final ServerDefinition serverDefinition;

	/**
	 * The hostname or address uses for the listening socket
	 */
	private String currentListenAddress;

	/**
	 * The public hostname or address and port for external access (node
	 * communication)
	 */
	private String currentPublicAddress;

	/**
	 * The port TCP port used by the listening socket
	 */
	private int servletPort = -1;

	/**
	 * The port TCP port used by the listening socket
	 */
	private int restPort = -1;

	/**
	 * The data directory
	 */
	private File currentDataDir;

	/**
	 * 
	 * @param serverDefinition
	 *            The default parameters
	 */
	protected AbstractServer(ServerDefinition serverDefinition) {
		this.serverDefinition = serverDefinition;
	}

	/**
	 * Override this method to put additional options. By default, this method
	 * create the options "h - help" and "p - port". Do not forget to call
	 * super.defineOptions(options);
	 * 
	 * @param options
	 *            The options instance
	 */
	public void defineOptions(Options options) {
		options.addOption(HELP_OPTION);
		options.addOption(WEBAPP_TCP_PORT_OPTION);
		options.addOption(WEBSERVICE_TCP_PORT_OPTION);
		options.addOption(LISTEN_ADDRESS_OPTION);
		options.addOption(PUBLIC_ADDRESS_OPTION);
		options.addOption(DATADIR_OPTION);
	}

	/**
	 * Call this method to start the server
	 * 
	 * @param args
	 *            Any command line parameters
	 * @throws IOException
	 *             if any IO error occur
	 * @throws ParseException
	 *             if the command line parameters are not valid
	 * @throws ServletException
	 *             if the servlet configuration failed
	 */
	final public void start(String[] args) throws IOException, ParseException,
			ServletException {
		Logger.getLogger("").setLevel(Level.WARNING);
		Options options = new Options();
		defineOptions(options);
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(options, args);

		// Help option
		if (cmd.hasOption(HELP_OPTION.getOpt())) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar " + serverDefinition.mainJarPath,
					options);
			return;
		}

		File dataDir = null;
		// Data directory option
		if (serverDefinition.defaultDataDirName != null) {
			dataDir = new File(System.getProperty("user.home"),
					serverDefinition.defaultDataDirName);
			if (cmd.hasOption(DATADIR_OPTION.getOpt()))
				dataDir = new File(cmd.getOptionValue(DATADIR_OPTION.getOpt()));
			if (!dataDir.exists())
				throw new IOException("The data directory does not exists: "
						+ dataDir);
			if (!dataDir.isDirectory())
				throw new IOException(
						"The data directory path is not a directory: "
								+ dataDir);

			if (serverDefinition.subDirectoryNames != null) {
				for (String subDirName : serverDefinition.subDirectoryNames) {
					File subDir = new File(dataDir, subDirName);
					if (!subDir.exists())
						subDir.mkdir();
				}
			}

		}

		currentDataDir = dataDir;

		// TCP port and listening adresss options
		int webAppTcpPort = cmd.hasOption(WEBAPP_TCP_PORT_OPTION.getOpt()) ? Integer
				.parseInt(cmd.getOptionValue(WEBAPP_TCP_PORT_OPTION.getOpt()))
				: serverDefinition.defaultWebApplicationTcpPort;
		int webServiceTcpPort = cmd.hasOption(WEBSERVICE_TCP_PORT_OPTION
				.getOpt()) ? Integer.parseInt(cmd
				.getOptionValue(WEBSERVICE_TCP_PORT_OPTION.getOpt()))
				: serverDefinition.defaultWebServiceTcpPort;
		currentListenAddress = cmd.hasOption(LISTEN_ADDRESS_OPTION.getOpt()) ? cmd
				.getOptionValue(LISTEN_ADDRESS_OPTION.getOpt())
				: serverDefinition.defaultHostname;

		currentPublicAddress = cmd.hasOption(PUBLIC_ADDRESS_OPTION.getOpt()) ? cmd
				.getOptionValue(PUBLIC_ADDRESS_OPTION.getOpt()) : null;
		if (StringUtils.isEmpty(currentPublicAddress))
			currentPublicAddress = currentListenAddress;

		commandLine(cmd);

		// Launch the servlet application if any
		Builder servletBuilder = null;
		ServletApplication servletApplication = getServletApplication();
		if (servletApplication != null) {
			servletPort = webAppTcpPort;
			DeploymentInfo deploymentInfo = servletApplication
					.getDeploymentInfo();
			DeploymentManager manager = Servlets.defaultContainer()
					.addDeployment(deploymentInfo);
			manager.deploy();
			PathHandler pathHandler = new PathHandler();
			String prefixPath = servletApplication.getContextPath();
			if (StringUtils.isEmpty(prefixPath))
				prefixPath = "/";
			pathHandler.addPrefixPath(prefixPath, manager.start());
			servletBuilder = Undertow.builder()
					.addHttpListener(servletPort, currentListenAddress)
					.setHandler(pathHandler);
		}

		// Launch the jaxrs application if any
		Builder restBuilder = null;
		RestApplication restApplication = getRestApplication();
		if (restApplication != null) {
			restPort = webServiceTcpPort;
			restBuilder = Undertow.builder().addHttpListener(restPort,
					currentListenAddress);
		}

		load();

		// Start the servers
		if (servletBuilder != null)
			servletBuilder.build().start();
		if (restBuilder != null && restApplication != null)
			new UndertowJaxrsServer().deploy(restApplication)
					.deploy(restApplication).start(restBuilder);

	}

	public abstract void commandLine(CommandLine cmd) throws IOException,
			ParseException;

	public abstract void load() throws IOException;

	/**
	 * 
	 * @return the hostname and port on which the web application can be
	 *         contacted
	 */
	public String getWebApplicationPublicAddress() {
		return currentPublicAddress + ':' + servletPort;
	}

	/**
	 * 
	 * @return the hostname and port on which the web service can be contacted
	 */
	public String getWebServicePublicAddress() {
		return currentPublicAddress + ':' + restPort;
	}

	/**
	 * 
	 * @return the data directory
	 */
	public File getCurrentDataDir() {
		return currentDataDir;
	}

	protected abstract RestApplication getRestApplication();

	protected abstract ServletApplication getServletApplication();

}
