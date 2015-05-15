/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.affinities.process;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.opensearchserver.client.ServerResource;
import com.opensearchserver.client.common.search.query.SearchFieldQuery;
import com.opensearchserver.client.common.search.query.SearchQueryBatch;
import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResults;
import com.qwazr.utils.json.client.JsonClientAbstract;

public interface AffinityProcessInterface {

	/**
	 * @param serverResource
	 *            The server and resource to use
	 * @return a client instance
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 */
	<T extends JsonClientAbstract> T getJsonClient(ServerResource serverResource)
			throws URISyntaxException;

	/**
	 * Crawl the content of the given URL
	 * 
	 * @param serverResource
	 *            The server and resource to use
	 * @param url
	 *            The crawled URL
	 * @return The crawled content
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	Map<String, String> doUrl(ServerResource serverResource, String url)
			throws IOException, URISyntaxException;

	/**
	 * Build an Exact Match query
	 * 
	 * @param name
	 *            the submitted content
	 * @param affinity
	 *            the affinity instance
	 * @param request
	 *            the affinity request
	 * @return a list of matching documents
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	SearchFieldQuery getExactMatchQuery(String name, Affinity affinity,
			AffinityRequest request) throws IOException, URISyntaxException;

	/**
	 * Execute a query
	 * 
	 * @param name
	 *            the submitted content
	 * @param serverResource
	 *            The server and resource to query
	 * @param query
	 *            the search query
	 * @return a list of matching document
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	AffinityResults executeSearchQuery(String name,
			ServerResource serverResource, SearchFieldQuery query)
			throws IOException, URISyntaxException;

	/**
	 * Execute a batch query
	 * 
	 * @param serverResource
	 *            The server and resource to query
	 * @param queryBatch
	 *            a batch of queries
	 * @param batchRequests
	 *            a batch of affinity requests
	 * @param results
	 *            a collection filled with the results
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	void executeSearchQueryBatch(ServerResource serverResource,
			SearchQueryBatch queryBatch,
			List<AffinityBatchRequest> batchRequests,
			List<AffinityResults> results) throws IOException,
			URISyntaxException;

	/**
	 * Execute a scoring query
	 * 
	 * @param name
	 *            the submitted content
	 * @param affinity
	 *            the affinity instance
	 * @param request
	 *            the affinity request
	 * @return the best matching documents
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	SearchFieldQuery getScoringQuery(String name, Affinity affinity,
			AffinityRequest request) throws IOException, URISyntaxException;

	/**
	 * Check if an entry is available in the cache
	 * 
	 * @param serverResource
	 *            The server and resource to query
	 * @param requestString
	 *            the request ID
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @return the result
	 */
	AffinityResults readCacheEntry(ServerResource serverResource,
			String requestString) throws IOException, URISyntaxException;

}
