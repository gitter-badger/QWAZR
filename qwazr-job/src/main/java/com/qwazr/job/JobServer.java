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
package com.qwazr.job;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.quartz.SchedulerException;

import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.job.scheduler.SchedulerManager;
import com.qwazr.job.scheduler.SchedulerServiceImpl;
import com.qwazr.job.script.ScriptManager;
import com.qwazr.job.script.ScriptServiceImpl;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServerException;
import com.qwazr.utils.server.ServletApplication;

public class JobServer extends AbstractServer {

	public final static String SERVICE_NAME_SCHEDULER = "schedulers";
	public final static String SERVICE_NAME_SCRIPT = "scripts";

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9098;
		serverDefinition.mainJarPath = "qwazr-job.jar";
		serverDefinition.defaultDataDirPath = "qwazr";
	}

	public final static Option THREADS_OPTION = new Option("t", "maxthreads",
			true, "The maximum of threads");

	private JobServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/job")
	public static class JobApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(ClusterServiceImpl.class);
			classes.add(ScriptServiceImpl.class);
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

	public static void loadScript(AbstractServer server) throws IOException {
		ScriptManager.load(server, server.getCurrentDataDir());
	}

	public static void loadScheduler(AbstractServer server, int maxThreads)
			throws IOException {
		try {
			SchedulerManager.load(server, server.getCurrentDataDir(),
					maxThreads);
		} catch (SchedulerException | ServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void load() throws IOException {
		ClusterServer.load(this, getCurrentDataDir(), null, null);
		loadScript(this);
		loadScheduler(this, maxThreads);
	}

	public static void main(String[] args) throws IOException, ParseException,
			ServletException, SchedulerException {
		new JobServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME_SCHEDULER,
				SERVICE_NAME_SCRIPT);
	}

	@Override
	protected RestApplication getRestApplication() {
		return new JobApplication();
	}

	@Override
	protected ServletApplication getServletApplication() {
		return null;
	}

}
