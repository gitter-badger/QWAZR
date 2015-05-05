/**
 * Copyright 2015 OpenSearchServer Inc.
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

import org.apache.http.client.ClientProtocolException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.opensearchserver.client.JsonClient1;
import com.opensearchserver.client.ServerResource;
import com.opensearchserver.client.common.search.query.SearchFieldQuery;
import com.opensearchserver.client.common.search.query.SearchQueryBatch;
import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResults;

public class AffinityProcess2Impl implements AffinityProcessInterface {

	@SuppressWarnings("unchecked")
	@Override
	public JsonClient1 getJsonClient(ServerResource serverResource)
			throws URISyntaxException {
		return new JsonClient1(serverResource.url, serverResource.login,
				serverResource.api_key, 60000);
	}

	@Override
	public Map<String, String> doUrl(ServerResource serverResource, String url)
			throws JsonParseException, JsonMappingException,
			ClientProtocolException, IOException, URISyntaxException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public SearchFieldQuery getExactMatchQuery(String name, Affinity affinity,
			AffinityRequest request) throws URISyntaxException,
			ClientProtocolException, IOException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public SearchFieldQuery getScoringQuery(String name, Affinity affinity,
			AffinityRequest request) throws ClientProtocolException,
			IOException, URISyntaxException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public AffinityResults readCacheEntry(ServerResource serverResource,
			String requestString) throws IOException, URISyntaxException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public AffinityResults executeSearchQuery(String name,
			ServerResource serverResource, SearchFieldQuery query)
			throws IOException, URISyntaxException {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public void executeSearchQueryBatch(ServerResource serverResource,
			SearchQueryBatch queryBatch,
			List<AffinityBatchRequest> batchRequests,
			List<AffinityResults> results) throws IOException,
			URISyntaxException {
		throw new RuntimeException("Not yet implemented");
	}

}
