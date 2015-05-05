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
import java.util.TreeMap;

import org.apache.http.client.ClientProtocolException;

import com.opensearchserver.client.JsonClient1;
import com.opensearchserver.client.ServerResource;
import com.opensearchserver.client.common.CommonListResult;
import com.opensearchserver.client.common.search.query.CollapsingParameters;
import com.opensearchserver.client.common.search.query.CollapsingParameters.ModeEnum;
import com.opensearchserver.client.common.search.query.CollapsingParameters.TypeEnum;
import com.opensearchserver.client.common.search.query.SearchField;
import com.opensearchserver.client.common.search.query.SearchField.SearchFieldMode;
import com.opensearchserver.client.common.search.query.SearchFieldQuery;
import com.opensearchserver.client.common.search.query.SearchQueryAbstract.OperatorEnum;
import com.opensearchserver.client.common.search.query.SearchQueryBatch;
import com.opensearchserver.client.common.search.query.filter.TermFilter;
import com.opensearchserver.client.v1.SearchApi1;
import com.opensearchserver.client.v1.WebCrawlerApi1;
import com.opensearchserver.client.v1.search.FieldValueList1;
import com.opensearchserver.client.v1.search.SearchResult1;
import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResult;
import com.qwazr.affinities.model.AffinityResults;
import com.qwazr.utils.json.JsonMapper;

/**
 * Implementation for OpenSearchServer v1.5.x
 */
public class AffinityProcess1Impl implements AffinityProcessInterface {

	@SuppressWarnings("unchecked")
	@Override
	public JsonClient1 getJsonClient(ServerResource serverResource)
			throws URISyntaxException {
		return new JsonClient1(serverResource.url, serverResource.login,
				serverResource.api_key, 60000);
	}

	private SearchApi1 getSearchApi(ServerResource serverResource)
			throws URISyntaxException {
		return new SearchApi1(getJsonClient(serverResource));
	}

	private WebCrawlerApi1 getWebCrawlApi(ServerResource serverResource)
			throws URISyntaxException {
		return new WebCrawlerApi1(getJsonClient(serverResource));
	}

	@Override
	public Map<String, String> doUrl(ServerResource serverResource, String url)
			throws URISyntaxException, IOException {
		WebCrawlerApi1 webCrawlerApi = getWebCrawlApi(serverResource);
		CommonListResult<List<FieldValueList1>> webCrawlResult = webCrawlerApi
				.crawlWithData(serverResource.name, url, 60000);
		if (webCrawlResult == null || webCrawlResult.items == null)
			return null;

		Map<String, String> result = new TreeMap<String, String>();

		for (List<FieldValueList1> items : webCrawlResult.items) {
			if (items == null)
				continue;
			for (FieldValueList1 fieldValueList : items) {
				String field = fieldValueList.fieldName.intern();
				StringBuilder sb = new StringBuilder();
				for (String value : fieldValueList.values) {
					sb.append(value);
					sb.append(' ');
				}
				result.put(field, sb.toString());
			}
		}
		return result;
	}

	@Override
	public SearchFieldQuery getExactMatchQuery(String name, Affinity affinity,
			AffinityRequest request) throws URISyntaxException,
			ClientProtocolException, IOException {

		SearchFieldQuery query = (SearchFieldQuery) new SearchFieldQuery()
				.setEmptyReturnsAll(true).setOperator(OperatorEnum.AND);

		for (Map.Entry<String, String> entry : request.criteria.entrySet())
			query.addFilter(new TermFilter(entry.getKey(), entry.getValue()));

		query.setReturnedFields(AffinityProcess
				.getReturnedFieldsFromAffinityOrRequest(affinity, request));

		String group_by = AffinityProcess.anyGroupBy(affinity, request);
		if (group_by != null)
			query.setCollapsing(new CollapsingParameters().setField(group_by)
					.setMax(0).setMode(ModeEnum.ADJACENT)
					.setType(TypeEnum.OPTIMIZED));

		return query;
	}

	@Override
	public SearchFieldQuery getScoringQuery(String name, Affinity affinity,
			AffinityRequest request) throws ClientProtocolException,
			IOException, URISyntaxException {

		SearchFieldQuery query = (SearchFieldQuery) new SearchFieldQuery()
				.setEmptyReturnsAll(false).setOperator(OperatorEnum.OR);

		StringBuilder sbQuery = new StringBuilder();
		for (String criterion : request.criteria.values()) {
			sbQuery.append(criterion);
			sbQuery.append(' ');
		}
		query.setQuery(sbQuery.toString());

		for (Map.Entry<String, Double> entry : affinity.criteria.entrySet()) {
			query.addSearchField(new SearchField().setField(entry.getKey())
					.setMode(SearchFieldMode.TERM_AND_PHRASE)
					.setBoost(entry.getValue())
					.setPhraseBoost(entry.getValue()).setPhraseSlop(10));
		}
		query.setQueryStringMap(request.criteria);

		query.setReturnedFields(AffinityProcess
				.getReturnedFieldsFromAffinityOrRequest(affinity, request));

		String group_by = AffinityProcess.anyGroupBy(affinity, request);
		if (group_by != null)
			query.setCollapsing(new CollapsingParameters().setField(group_by)
					.setMax(0).setMode(ModeEnum.ADJACENT)
					.setType(TypeEnum.OPTIMIZED));

		return query;
	}

	@Override
	public AffinityResults readCacheEntry(ServerResource serverResource,
			String requestString) throws IOException, URISyntaxException {

		SearchApi1 searchApi = getSearchApi(serverResource);
		SearchFieldQuery query = (SearchFieldQuery) new SearchFieldQuery()
				.setEmptyReturnsAll(true).setOperator(OperatorEnum.AND);

		query.addFilter(new TermFilter(AffinityProcess.CACHE_FIELD_KEY,
				requestString));

		query.addReturnedField(AffinityProcess.CACHE_FIELD_VALUE);
		SearchResult1 result = searchApi.executeSearchField(
				serverResource.name, query);
		if (result == null || result.documents == null
				|| result.documents.isEmpty())
			return null;
		List<FieldValueList1> fields = result.documents.get(0).fields;
		if (fields == null || fields.isEmpty())
			return null;
		for (FieldValueList1 fieldValue : fields) {
			if (AffinityProcess.CACHE_FIELD_VALUE.equals(fieldValue.fieldName)) {
				if (fieldValue.values != null && !fieldValue.values.isEmpty()) {
					return JsonMapper.MAPPER.readValue(
							fieldValue.values.get(0), AffinityResults.class);
				}
			}
		}
		return null;
	}

	@Override
	public AffinityResults executeSearchQuery(String name,
			ServerResource serverResource, SearchFieldQuery query)
			throws IOException, URISyntaxException {
		SearchApi1 searchApi = getSearchApi(serverResource);
		return AffinityResult.newList(name,
				searchApi.executeSearchField(serverResource.name, query));
	}

	@Override
	public void executeSearchQueryBatch(ServerResource serverResource,
			SearchQueryBatch queryBatch,
			List<AffinityBatchRequest> batchRequests,
			List<AffinityResults> results) throws IOException,
			URISyntaxException {
		SearchApi1 searchApi = getSearchApi(serverResource);
		List<SearchResult1> searchResults = searchApi.searchBatch(
				serverResource.name, queryBatch);
		int i = 0;
		for (SearchResult1 searchResult : searchResults)
			results.add(AffinityResult.newList(batchRequests.get(i++).name,
					searchResult));
	}
}
