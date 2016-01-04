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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.cluster.client.ClusterMultiClient;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.AbstractConnector;
import com.qwazr.crawler.web.WebCrawlerServer;
import com.qwazr.crawler.web.client.WebCrawlerMultiClient;
import com.qwazr.crawler.web.client.WebCrawlerSingleClient;
import com.qwazr.crawler.web.service.WebCrawlerServiceInterface;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.extractor.ParserManager;
import com.qwazr.job.JobServer;
import com.qwazr.job.script.ScriptMultiClient;
import com.qwazr.search.SearchServer;
import com.qwazr.search.index.IndexMultiClient;
import com.qwazr.search.index.IndexServiceImpl;
import com.qwazr.search.index.IndexServiceInterface;
import com.qwazr.search.index.IndexSingleClient;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicesProvider extends AbstractConnector {

	private ExecutorService executorService = null;

	@Override
	public void load(File data_directory) {
		executorService = Executors.newFixedThreadPool(8);
	}

	@Override
	public void unload() {
		executorService.shutdown();
	}

	public ClusterMultiClient getCluster() {
		if (ClusterManager.INSTANCE == null)
			return null;
		return ClusterManager.INSTANCE.getClusterClient();
	}

	/**
	 * Create a new WebCrawler client instance.
	 * This API queries the cluster to get the current active node for the WebCrawler service.
	 *
	 * @return a new WebCrawlerServiceInterface instance
	 * @throws URISyntaxException
	 */
	@JsonIgnore
	public WebCrawlerServiceInterface getNewWebCrawlerClient() throws URISyntaxException {
		return getNewWebCrawlerClient(null);
	}

	/**
	 * Create a new WebCrawler client instance.
	 * This API queries the cluster to get the current active node for the WebCrawler service.
	 *
	 * @param msTimeout the default timeout used by the client
	 * @return a new WebCrawlerMultiClient instance
	 * @throws URISyntaxException
	 */
	@JsonIgnore
	public WebCrawlerServiceInterface getNewWebCrawlerClient(Integer msTimeout) throws URISyntaxException {
		if (ClusterManager.INSTANCE.isCluster())
			return new WebCrawlerMultiClient(ClusterManager.INSTANCE.getClusterClient()
							.getActiveNodes(WebCrawlerServer.SERVICE_NAME_WEBCRAWLER), msTimeout);
		else
			return new WebCrawlerSingleClient(ClusterManager.INSTANCE.myAddress, msTimeout);
	}

	/**
	 * Create a new Script client instance.
	 * This API queries the cluster to get the current active node for the Script service.
	 *
	 * @return
	 * @throws URISyntaxException
	 */
	@JsonIgnore
	public ScriptMultiClient getNewScriptClient() throws URISyntaxException {
		return getNewScriptClient(null);
	}

	@JsonIgnore
	public ScriptMultiClient getNewScriptClient(Integer msTimeout) throws URISyntaxException {
		return new ScriptMultiClient(executorService,
						ClusterManager.INSTANCE.getClusterClient().getActiveNodes(JobServer.SERVICE_NAME_SCRIPT),
						msTimeout);
	}

	@JsonIgnore
	public ExtractorServiceInterface getNewExtractorClient() {
		if (ParserManager.INSTANCE == null)
			throw new RuntimeException("Extractor service not available");
		return new ExtractorServiceImpl();
	}

	@JsonIgnore
	public IndexServiceInterface getNewIndexClient(Boolean local, Integer msTimeout) throws URISyntaxException {
		if (local != null && local)
			return new IndexServiceImpl();
		String[] nodes = ClusterManager.INSTANCE.getClusterClient().getActiveNodes(SearchServer.SERVICE_NAME_SEARCH);
		if (nodes == null)
			throw new RuntimeException("Index service not available");
		if (nodes.length == 1)
			return new IndexSingleClient(nodes[0], msTimeout);
		return new IndexMultiClient(executorService,
						ClusterManager.INSTANCE.getClusterClient().getActiveNodes(SearchServer.SERVICE_NAME_SEARCH),
						msTimeout);
	}
}
