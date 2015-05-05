/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.analyzer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.qwazr.analyzer.markdown.MarkdownProcessManager;
import com.qwazr.analyzer.markdown.MarkdownServiceImpl;
import com.qwazr.analyzer.postagger.LanguageManager;
import com.qwazr.analyzer.postagger.POSTaggerServiceImpl;
import com.qwazr.cluster.ClusterServer;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.RestApplication;
import com.qwazr.utils.server.ServletApplication;

public class AnalyzeServer extends AbstractServer {

	public final static String SERVICE_NAME = "analyze";

	private final static ServerDefinition serverDefinition = new ServerDefinition();
	static {
		serverDefinition.defaultWebApplicationTcpPort = 9091;
		serverDefinition.mainJarPath = "qwazr-analyze.jar";
		serverDefinition.defaultDataDirPath = "qwazr";
	}

	private AnalyzeServer() {
		super(serverDefinition);
	}

	@ApplicationPath("/")
	public static class AnalyzeApplication extends RestApplication {

		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>> classes = super.getClasses();
			classes.add(POSTaggerServiceImpl.class);
			classes.add(MarkdownServiceImpl.class);
			return classes;
		}
	}

	public static void load(AbstractServer server, File data_directory,
			Set<Class<?>> restClasses) throws IOException {
		File analyzeDirectory = new File(data_directory, "analyze");
		if (!analyzeDirectory.exists())
			analyzeDirectory.mkdir();
		LanguageManager.load(server, analyzeDirectory);
		MarkdownProcessManager.load(server, analyzeDirectory);
		if (restClasses != null)
			restClasses.add(AnalyzeApplication.class);
	}

	@Override
	public void commandLine(CommandLine cmd) throws IOException {
	}

	@Override
	public void load() throws IOException {
		ClusterServer.load(this, getCurrentDataDir(), null, null);
		load(this, getCurrentDataDir(), null);
	}

	@Override
	public RestApplication getRestApplication() {
		return new AnalyzeApplication();
	}

	@Override
	protected ServletApplication getServletApplication() {
		return null;
	}

	public static void main(String[] args) throws IOException, ParseException,
			ServletException {
		new AnalyzeServer().start(args);
		ClusterManager.INSTANCE.registerMe(SERVICE_NAME);
	}

}
