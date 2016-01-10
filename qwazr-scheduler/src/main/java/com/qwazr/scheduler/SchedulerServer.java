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
package com.qwazr.scheduler;

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.connectors.ConnectorManagerImpl;
import com.qwazr.tools.ToolsManagerImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.server.ServletApplication;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.quartz.SchedulerException;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class SchedulerServer extends AbstractServer {

	public final static String SERVICE_NAME_SCHEDULER = "schedulers";

	private final static ServerDefinition serverDefinition = new ServerDefinition();

	static {
		serverDefinition.defaultWebApplicationTcpPort = 9098;
		serverDefinition.mainJarPath = "qwazr-scheduler.jar";
		serverDefinition.defaultDataDirName = "qwazr";
	}

	public final static Option THREADS_OPTION = new Option("t", "maxthreads", true, "The maximum of threads");

	private SchedulerServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class SchedulerApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(ClusterServiceImpl.class);
			classes.add(SchedulerServiceImpl.class);
			return classes;
		}
	}

	private int maxThreads = 1000;

	@Override
	public void defineOptions(Options options) {
		super.defineOptions(options);
		options.addOption(THREADS_OPTION);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException, ParseException {
		maxThreads = Integer.parseInt(THREADS_OPTION.getValue("1000"));
	}

	public static void loadScheduler(File dataDirectory, int maxThreads) throws IOException {
		try {
			SchedulerManager.load(dataDirectory, maxThreads);
		} catch (SchedulerException | ServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void load() throws IOException {
		File currentDataDir = getCurrentDataDir();
		ClusterServer.load(getWebServicePublicAddress(), currentDataDir);
		ConnectorManagerImpl.load(currentDataDir);
		ToolsManagerImpl.load(currentDataDir);
		loadScheduler(currentDataDir, maxThreads);
	}

	public static void main(String[] args)
			throws IOException, ParseException, ServletException, SchedulerException, InstantiationException,
			IllegalAccessException {
		new SchedulerServer().start(args);
	}

	@Override
	protected Class<SchedulerApplication> getRestApplication() {
		return SchedulerApplication.class;
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
