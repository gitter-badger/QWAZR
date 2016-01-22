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
package com.qwazr.utils.server;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletContainer;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

/**
 * Generic REST server
 */
public abstract class AbstractServer {

	static volatile AbstractServer INSTANCE = null;

	final Collection<Class<? extends ServiceInterface>> services;

	protected final ExecutorService executorService;

	private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

	/**
	 * Standard help option
	 */
	public final static Option HELP_OPTION = new Option("h", "help", false, "print this message");

	/**
	 * The user can change the TCP listening port
	 */
	public final static Option WEBAPP_TCP_PORT_OPTION = new Option("wp", "webapp-port", true,
					"TCP port for web application");

	/**
	 * The user can change the TCP listening port
	 */
	public final static Option WEBSERVICE_TCP_PORT_OPTION = new Option("sp", "webservice-port", true,
					"TCP port for the web service");

	/**
	 * Set the listening host or IP address
	 */
	public final static Option LISTEN_ADDRESS_OPTION = new Option("l", "listen", true,
					"Listening hostname or IP address");

	/**
	 * Set the public address (in case of NAT)
	 */
	public final static Option PUBLIC_ADDRESS_OPTION = new Option("a", "public-address", true,
					"The public hostname or IP address for node communication");

	/**
	 * The name of the REALM connector used for the webservice authentication
	 */
	public final static Option WEBSERVICE_REALM_OPTION = new Option("wsr", "ws-realm", true,
					"The name of the REALM connector used by the web service");

	/**
	 * The type of the webservice authentication
	 */
	public final static Option WEBSERVICE_AUTH_TYPE_OPTION = new Option("wsa", "ws-auth", true,
					"The type of the authentication of the web service");

	/**
	 * Set the data directory
	 */
	public final static Option DATADIR_OPTION = new Option("d", "datadir", true, "Data directory");

	public static class ServerDefinition {
		/**
		 * The default hostname. Could be 0.0.0.0 or localhost
		 */
		public String defaultHostname = "localhost";

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
	 */
	private String webServiceRealm;

	/**
	 *
	 */
	private String webServiceAuthType;

	/**
	 * @param serverDefinition The default parameters
	 */
	protected AbstractServer(ServerDefinition serverDefinition, ExecutorService executorService) {
		this.serverDefinition = serverDefinition;
		this.executorService = executorService;
		this.services = new ArrayList<>();
		this.INSTANCE = this;
	}

	/**
	 * Override this method to put additional options. By default, this method
	 * create the options "h - help" and "p - port". Do not forget to call
	 * super.defineOptions(options);
	 *
	 * @param options The options instance
	 */
	public void defineOptions(Options options) {
		options.addOption(HELP_OPTION);
		options.addOption(WEBAPP_TCP_PORT_OPTION);
		options.addOption(WEBSERVICE_TCP_PORT_OPTION);
		options.addOption(LISTEN_ADDRESS_OPTION);
		options.addOption(PUBLIC_ADDRESS_OPTION);
		options.addOption(DATADIR_OPTION);
		options.addOption(WEBSERVICE_REALM_OPTION);
		options.addOption(WEBSERVICE_AUTH_TYPE_OPTION);
	}

	/**
	 * Call this method to start the server
	 *
	 * @param args Any command line parameters
	 * @throws IOException      if any IO error occur
	 * @throws ParseException   if the command line parameters are not valid
	 * @throws ServletException if the servlet configuration failed
	 */
	final public void start(String[] args) throws IOException, ParseException, ServletException, IllegalAccessException,
					InstantiationException {

		java.util.logging.Logger.getLogger("").setLevel(Level.WARNING);
		Options options = new Options();
		defineOptions(options);
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		// Help option
		if (cmd.hasOption(HELP_OPTION.getOpt())) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar " + serverDefinition.mainJarPath, options);
			return;
		}

		if (cmd.hasOption(WEBSERVICE_REALM_OPTION.getOpt())) {
			webServiceRealm = cmd.getOptionValue(WEBSERVICE_REALM_OPTION.getOpt());
		}

		if (cmd.hasOption(WEBSERVICE_AUTH_TYPE_OPTION.getOpt())) {
			webServiceAuthType = cmd.getOptionValue(WEBSERVICE_AUTH_TYPE_OPTION.getOpt());
		}

		File dataDir = null;
		// Data directory option
		if (cmd.hasOption(DATADIR_OPTION.getOpt())) {
			dataDir = new File(cmd.getOptionValue(DATADIR_OPTION.getOpt()));
			if (!dataDir.exists())
				throw new IOException("The data directory does not exists: " + dataDir);
		} else if (serverDefinition.defaultDataDirName != null) {
			dataDir = new File(System.getProperty("user.home"), serverDefinition.defaultDataDirName);
		}
		if (dataDir == null || !dataDir.exists())
			dataDir = new File(System.getProperty("user.dir"));
		if (!dataDir.isDirectory())
			throw new IOException("The data directory path is not a directory: " + dataDir);
		logger.info("Data directory sets to: " + dataDir);

		currentDataDir = dataDir;

		// TCP port and listening adresss options
		servletPort = cmd.hasOption(WEBAPP_TCP_PORT_OPTION.getOpt()) ?
						Integer.parseInt(cmd.getOptionValue(WEBAPP_TCP_PORT_OPTION.getOpt())) :
						serverDefinition.defaultWebApplicationTcpPort;
		restPort = cmd.hasOption(WEBSERVICE_TCP_PORT_OPTION.getOpt()) ?
						Integer.parseInt(cmd.getOptionValue(WEBSERVICE_TCP_PORT_OPTION.getOpt())) :
						serverDefinition.defaultWebServiceTcpPort;
		currentListenAddress = cmd.hasOption(LISTEN_ADDRESS_OPTION.getOpt()) ?
						cmd.getOptionValue(LISTEN_ADDRESS_OPTION.getOpt()) :
						serverDefinition.defaultHostname;

		currentPublicAddress = cmd.hasOption(PUBLIC_ADDRESS_OPTION.getOpt()) ?
						cmd.getOptionValue(PUBLIC_ADDRESS_OPTION.getOpt()) :
						null;
		if (StringUtils.isEmpty(currentPublicAddress))
			currentPublicAddress = currentListenAddress;

		commandLine(cmd);

		ServletApplication servletApplication = load(services);

		// Launch the servlet application if any
		if (servletApplication != null) {
			DeploymentInfo deploymentInfo = servletApplication.getDeploymentInfo();
			DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
			manager.deploy();
			PathHandler pathHandler = new PathHandler();
			pathHandler.addPrefixPath(servletApplication.getApplicationPath(), manager.start());
			logger.info("Start the WEB server " + currentListenAddress + ":" + servletPort);
			Builder servletBuilder = Undertow.builder().addHttpListener(servletPort, currentListenAddress)
							.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 10000).setHandler(pathHandler);
			servletBuilder.build().start();
		}

		// Launch the jaxrs application if any
		if (!services.isEmpty()) {
			DeploymentInfo deploymentInfo = RestApplication.getDeploymentInfo();
			IdentityManager identityManager = null;
			if (webServiceRealm != null) {
				identityManager = getIdentityManager(webServiceRealm);
				deploymentInfo.setIdentityManager(identityManager)
								.setLoginConfig(new LoginConfig("BASIC", webServiceRealm));
				deploymentInfo.addInitParameter("resteasy.role.based.security", "true");
			}
			ServletContainer container = Servlets.defaultContainer();
			DeploymentManager manager = container.addDeployment(deploymentInfo);
			manager.deploy();
			HttpHandler httpHandler = manager.start();
			if (identityManager != null)
				httpHandler = addSecurity(httpHandler, identityManager, webServiceRealm);
			PathHandler pathHandler = new PathHandler();
			pathHandler.addPrefixPath("/", httpHandler);
			logger.info("Start the REST server " + currentListenAddress + ":" + restPort);
			Builder restBuilder = Undertow.builder().addHttpListener(restPort, currentListenAddress)
							.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 10000).setHandler(httpHandler);
			restBuilder.build().start();
		}
	}

	private static HttpHandler addSecurity(HttpHandler handler, final IdentityManager identityManager, String realm) {
		handler = new AuthenticationCallHandler(handler);
		handler = new AuthenticationConstraintHandler(handler);
		final List<AuthenticationMechanism> mechanisms = Collections.<AuthenticationMechanism>singletonList(
						new BasicAuthenticationMechanism(realm));
		handler = new AuthenticationMechanismsHandler(handler, mechanisms);
		handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
		return handler;
	}

	public abstract void commandLine(CommandLine cmd) throws IOException, ParseException;

	protected abstract ServletApplication load(Collection<Class<? extends ServiceInterface>> serviceClasses)
					throws IOException;

	/**
	 * @return the hostname and port on which the web application can be
	 * contacted
	 */
	public String getWebApplicationPublicAddress() {
		return currentPublicAddress + ':' + servletPort;
	}

	/**
	 * @return the hostname and port on which the web service can be contacted
	 */
	public String getWebServicePublicAddress() {
		return currentPublicAddress + ':' + restPort;
	}

	/**
	 * @return the data directory
	 */
	public File getCurrentDataDir() {
		return currentDataDir;
	}

	protected abstract IdentityManager getIdentityManager(String realm) throws IOException;
}
