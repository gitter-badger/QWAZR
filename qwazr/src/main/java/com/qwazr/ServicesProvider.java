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
package com.qwazr;

import java.net.URISyntaxException;

import com.qwazr.cluster.client.ClusterMultiClient;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.connectors.AbstractConnector;
import com.qwazr.connectors.ConnectorContext;
import com.qwazr.crawler.web.WebCrawlerServer;
import com.qwazr.crawler.web.client.WebCrawlerMultiClient;
import com.qwazr.extractor.ExtractorServiceImpl;
import com.qwazr.extractor.ExtractorServiceInterface;
import com.qwazr.extractor.ParserManager;
import com.qwazr.job.JobServer;
import com.qwazr.job.script.ScriptMultiClient;

public class ServicesProvider extends AbstractConnector {

	@Override
	public void load(ConnectorContext context) {
	}

	@Override
	public void unload(ConnectorContext context) {
	}

	public ClusterMultiClient getCluster() {
		if (ClusterManager.INSTANCE == null)
			return null;
		return ClusterManager.INSTANCE.getClusterClient();
	}

	public WebCrawlerMultiClient getNewWebCrawler() throws URISyntaxException {
		return new WebCrawlerMultiClient(ClusterManager.INSTANCE
				.getClusterClient().getActiveNodes(
						WebCrawlerServer.SERVICE_NAME), 60000);
	}

	public ScriptMultiClient getNewScriptClient() throws URISyntaxException {
		return new ScriptMultiClient(ClusterManager.INSTANCE.getClusterClient()
				.getActiveNodes(JobServer.SERVICE_NAME_SCRIPT), 60000);
	}

	public ExtractorServiceInterface getNewExtractorClient() {
		if (ParserManager.INSTANCE == null)
			throw new RuntimeException("Extractor service not available");
		return new ExtractorServiceImpl();
	}
}
