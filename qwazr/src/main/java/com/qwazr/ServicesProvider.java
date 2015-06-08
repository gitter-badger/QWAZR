/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qwazr.cluster.client.ClusterMultiClient;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.AbstractConnector;
import com.qwazr.crawler.web.WebCrawlerServer;
import com.qwazr.crawler.web.client.WebCrawlerMultiClient;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.extractor.ParserManager;
import com.qwazr.job.JobServer;
import com.qwazr.job.script.ScriptMultiClient;

public class ServicesProvider extends AbstractConnector {

	private ExecutorService executorService = null;

	@Override
	public void load(String contextId) {
		executorService = Executors.newFixedThreadPool(8);
	}

	@Override
	public void unload(String contextId) {
		executorService.shutdown();
	}

	public ClusterMultiClient getCluster() {
		if (ClusterManager.INSTANCE == null)
			return null;
		return ClusterManager.INSTANCE.getClusterClient();
	}

	public WebCrawlerMultiClient getNewWebCrawler() throws URISyntaxException {
		return getNewWebCrawler(null);
	}

	public WebCrawlerMultiClient getNewWebCrawler(Integer msTimeout)
			throws URISyntaxException {
		return new WebCrawlerMultiClient(ClusterManager.INSTANCE
				.getClusterClient().getActiveNodes(
						WebCrawlerServer.SERVICE_NAME_WEBCRAWLER), msTimeout);
	}

	public ScriptMultiClient getNewScriptClient() throws URISyntaxException {
		return getNewScriptClient(null);
	}

	public ScriptMultiClient getNewScriptClient(Integer msTimeout)
			throws URISyntaxException {
		return new ScriptMultiClient(executorService, ClusterManager.INSTANCE
				.getClusterClient().getActiveNodes(
						JobServer.SERVICE_NAME_SCRIPT), msTimeout);
	}

	public ExtractorServiceInterface getNewExtractorClient() {
		if (ParserManager.INSTANCE == null)
			throw new RuntimeException("Extractor service not available");
		return new ExtractorServiceImpl();
	}
}
