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

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.service.ClusterNodeJson;
import com.qwazr.cluster.service.ClusterServiceImpl;
import com.qwazr.compiler.CompilerManager;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.database.TableManager;
import com.qwazr.extractor.ParserManager;
import com.qwazr.graph.GraphManager;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.library.LibraryManager;
import com.qwazr.library.LibraryServiceImpl;
import com.qwazr.scheduler.SchedulerManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.search.index.IndexManager;
import com.qwazr.semaphores.SemaphoresManager;
import com.qwazr.utils.file.TrackedDirectory;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;
import com.qwazr.utils.server.ServletApplication;
import com.qwazr.webapps.transaction.WebappManager;
import io.undertow.security.idm.IdentityManager;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;

public class Qwazr extends AbstractServer<QwazrConfiguration> {

	static final Logger logger = LoggerFactory.getLogger(Qwazr.class);

	private final Collection<Class<? extends ServiceInterface>> services = new ArrayList<>();

	private Qwazr(QwazrConfiguration configuration) {
		super(Executors.newCachedThreadPool(), configuration);
	}

	@Path("/")
	@ServiceName("welcome")
	public class WelcomeServiceImpl implements ServiceInterface {

		@GET
		@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
		public WelcomeStatus welcome() {
			return new WelcomeStatus(services);
		}

	}

	public final static Option THREADS_OPTION = new Option("t", "maxthreads", true, "The maximum of threads");

	@Override
	public ServletApplication load(Collection<Class<? extends ServiceInterface>> classes) throws IOException {

		final ServletApplication servletApplication;

		File currentDataDir = getCurrentDataDir();
		File currentTempDir = new File(currentDataDir, "tmp");
		File currentEtcDir = new File(currentDataDir, "etc");
		TrackedDirectory etcTracker = new TrackedDirectory(currentEtcDir, serverConfiguration.etcFileFilter);

		ClusterManager.load(executorService, getWebServicePublicAddress(), serverConfiguration.groups);

		ClassLoaderManager.load(currentDataDir, Thread.currentThread());

		if (QwazrConfiguration.ServiceEnum.compiler.isActive(serverConfiguration))
			services.add(CompilerManager.load(executorService, currentDataDir));

		services.add(WelcomeServiceImpl.class);
		services.add(ClusterServiceImpl.class);

		if (QwazrConfiguration.ServiceEnum.extractor.isActive(serverConfiguration))
			services.add(ParserManager.load());

		if (QwazrConfiguration.ServiceEnum.webapps.isActive(serverConfiguration)) {
			services.add(WebappManager.load(currentDataDir, etcTracker, currentTempDir));
			servletApplication = WebappManager.getInstance().getServletApplication();
		} else
			servletApplication = null;

		if (QwazrConfiguration.ServiceEnum.semaphores.isActive(serverConfiguration))
			services.add(SemaphoresManager.load(executorService));

		if (QwazrConfiguration.ServiceEnum.scripts.isActive(serverConfiguration))
			services.add(ScriptManager.load(executorService, currentDataDir));

		if (QwazrConfiguration.ServiceEnum.webcrawler.isActive(serverConfiguration))
			services.add(WebCrawlerManager.load(executorService));

		if (QwazrConfiguration.ServiceEnum.search.isActive(serverConfiguration))
			services.add(IndexManager.load(executorService, currentDataDir));

		if (QwazrConfiguration.ServiceEnum.graph.isActive(serverConfiguration))
			services.add(GraphManager.load(executorService, currentDataDir));

		if (QwazrConfiguration.ServiceEnum.table.isActive(serverConfiguration))
			services.add(TableManager.load(executorService, currentDataDir));

		LibraryManager.load(currentDataDir, etcTracker);
		services.add(LibraryServiceImpl.class);

		// Scheduler is last, because it may immediatly execute a scripts
		if (QwazrConfiguration.ServiceEnum.schedulers.isActive(serverConfiguration))
			services.add(SchedulerManager.load(etcTracker, serverConfiguration.scheduler_max_threads));

		etcTracker.check();

		classes.addAll(services);

		return servletApplication;
	}

	@Override
	protected IdentityManager getIdentityManager(String realm) throws IOException {
		AbstractLibrary library = LibraryManager.getInstance().get(realm);
		if (library == null)
			throw new IOException("No realm connector with this name: " + realm);
		if (!(library instanceof IdentityManager))
			throw new IOException("This is a not a realm connector: " + realm);
		return (IdentityManager) library;
	}

	private void startAll()
			throws ServletException, IllegalAccessException, ParseException, IOException, InstantiationException {
		super.start(true);
		// Register the services
		ClusterManager.INSTANCE.registerMe(
				new ClusterNodeJson(ClusterManager.INSTANCE.myAddress, services, serverConfiguration.groups));

	}

	private static Qwazr qwazr = null;

	/**
	 * Start the server
	 *
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws ServletException
	 * @throws IllegalAccessException
	 * @throws ParseException
	 */
	public static synchronized void start(QwazrConfiguration configuration)
			throws IOException, InstantiationException, ServletException, IllegalAccessException, ParseException {
		if (qwazr != null)
			throw new IllegalAccessException("QWAZR is already started");
		qwazr = new Qwazr(configuration);
		qwazr.startAll();
	}

	/**
	 * Stop the server
	 */
	public static synchronized void stop() {
		if (qwazr == null)
			return;
		qwazr.stopAll();
	}

	public static void main(String[] args) {
		try {
			start(new QwazrConfiguration());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}

	}
}
