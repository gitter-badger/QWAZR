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
import com.qwazr.crawler.web.client.WebCrawlerMultiClient;
import com.qwazr.crawler.web.client.WebCrawlerSingleClient;
import com.qwazr.crawler.web.manager.WebCrawlerManager;
import com.qwazr.crawler.web.service.WebCrawlerServiceInterface;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.extractor.ParserManager;
import com.qwazr.scripts.ScriptManager;
import com.qwazr.scripts.ScriptMultiClient;
import com.qwazr.search.index.*;

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
		if (ClusterManager.getInstance() == null)
			return null;
		return ClusterManager.getInstance().getClusterClient();
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
		if (ClusterManager.getInstance().isCluster())
			return new WebCrawlerMultiClient(ClusterManager.getInstance().getClusterClient()
							.getActiveNodesByService(WebCrawlerManager.SERVICE_NAME_WEBCRAWLER, null), msTimeout);
		else
			return new WebCrawlerSingleClient(ClusterManager.getInstance().myAddress, msTimeout);
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
		return new ScriptMultiClient(executorService, ClusterManager.getInstance().getClusterClient()
						.getActiveNodesByService(ScriptManager.SERVICE_NAME_SCRIPT, null), msTimeout);
	}

	@JsonIgnore
	public ExtractorServiceInterface getNewExtractorClient() {
		ParserManager.getInstance();
		return new ExtractorServiceImpl();
	}

	@JsonIgnore
	public IndexServiceInterface getNewIndexClient(Boolean local, Integer msTimeout) throws URISyntaxException {
		if (local != null && local)
			return new IndexServiceImpl();
		String[] nodes = ClusterManager.getInstance().getClusterClient()
						.getActiveNodesByService(IndexManager.SERVICE_NAME_SEARCH, null);
		if (nodes == null)
			throw new RuntimeException("Index service not available");
		if (nodes.length == 1)
			return new IndexSingleClient(nodes[0], msTimeout);
		return new IndexMultiClient(executorService, ClusterManager.getInstance().getClusterClient()
						.getActiveNodesByService(IndexManager.SERVICE_NAME_SEARCH, null), msTimeout);
	}
}
