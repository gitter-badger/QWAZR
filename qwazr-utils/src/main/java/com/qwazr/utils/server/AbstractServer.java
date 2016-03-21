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
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.LoginConfig;
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
public abstract class AbstractServer<T extends ServerConfiguration> {

	static volatile AbstractServer INSTANCE = null;

	final Collection<Class<? extends ServiceInterface>> services;

	final Collection<Undertow> undertows;
	final Collection<DeploymentManager> deploymentManagers;

	protected final ExecutorService executorService;

	protected T serverConfiguration;

	private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

	protected AbstractServer(ExecutorService executorService, T serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
		this.executorService = executorService;
		this.services = new ArrayList<>();
		this.INSTANCE = this;
		this.undertows = new ArrayList<>();
		this.deploymentManagers = new ArrayList<>();
	}

	private synchronized void start(final Undertow undertow) {
		undertow.start();
		undertows.add(undertow);
	}

	private synchronized HttpHandler start(final DeploymentManager manager) throws ServletException {
		HttpHandler handler = manager.start();
		deploymentManagers.add(manager);
		return handler;
	}

	public synchronized void stopAll() {
		for (DeploymentManager manager : deploymentManagers)
			try {
				manager.stop();
			} catch (ServletException e) {
				if (logger.isWarnEnabled())
					logger.warn("Cannot stop the manager: " + e.getMessage(), e);
			}
		for (Undertow undertow : undertows)
			undertow.stop();
	}

	private final IdentityManager getIdentityManager(ServerConfiguration.Connector connector,
			DeploymentInfo deploymentInfo) throws IOException {
		if (connector == null)
			return null;
		if (connector.realm == null)
			return null;
		IdentityManager identityManager = getIdentityManager(connector.realm);
		if (identityManager == null)
			return null;
		deploymentInfo.setIdentityManager(identityManager).setLoginConfig(
				new LoginConfig(connector.authType == null ? "BASIC" : connector.authType, connector.realm));
		return identityManager;
	}

	private final void startServer(ServerConfiguration.Connector connector, DeploymentInfo deploymentInfo)
			throws IOException, ServletException {
		IdentityManager identityManager = getIdentityManager(connector, deploymentInfo);

		DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
		manager.deploy();

		HttpHandler httpHandler = start(manager);

		if (identityManager != null)
			httpHandler = addSecurity(httpHandler, identityManager, serverConfiguration.webAppConnector.realm);

		logger.info("Start the connector " + serverConfiguration.listenAddress + ":" + connector.port);

		Builder servletBuilder = Undertow.builder().addHttpListener(connector.port, serverConfiguration.listenAddress)
				.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 10000).setHandler(httpHandler);
		start(servletBuilder.build());
	}

	/**
	 * Call this method to start the server
	 *
	 * @throws IOException      if any IO error occur
	 * @throws ServletException if the servlet configuration failed
	 */
	final public void start(boolean shutdownHook)
			throws IOException, ServletException, IllegalAccessException, InstantiationException {

		java.util.logging.Logger.getLogger("").setLevel(Level.WARNING);

		if (!serverConfiguration.dataDirectory.exists())
			throw new IOException("The data directory does not exists: " + serverConfiguration.dataDirectory);
		if (!serverConfiguration.dataDirectory.isDirectory())
			throw new IOException("The data directory path is not a directory: " + serverConfiguration.dataDirectory);
		logger.info("Data directory sets to: " + serverConfiguration.dataDirectory);

		ServletApplication servletApplication = load(services);

		// Launch the servlet application if any
		if (servletApplication != null)
			startServer(serverConfiguration.webAppConnector, servletApplication.getDeploymentInfo());

		// Launch the jaxrs application if any
		if (!services.isEmpty())
			startServer(serverConfiguration.webServiceConnector, RestApplication.getDeploymentInfo());

		if (shutdownHook) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					stopAll();
				}
			});
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

	protected abstract ServletApplication load(Collection<Class<? extends ServiceInterface>> serviceClasses)
			throws IOException;

	/**
	 * @return the hostname and port on which the web application can be
	 * contacted
	 */
	public String getWebApplicationPublicAddress() {
		return serverConfiguration.publicAddress + ':' + serverConfiguration.webAppConnector.port;
	}

	/**
	 * @return the hostname and port on which the web service can be contacted
	 */
	public String getWebServicePublicAddress() {
		return serverConfiguration.publicAddress + ':' + serverConfiguration.webServiceConnector.port;
	}

	/**
	 * @return the data directory
	 */
	public File getCurrentDataDir() {
		return serverConfiguration.dataDirectory;
	}

	protected abstract IdentityManager getIdentityManager(String realm) throws IOException;
}
