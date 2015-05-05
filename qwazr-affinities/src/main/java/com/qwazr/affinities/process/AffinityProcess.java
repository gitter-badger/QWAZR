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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.client.ClientProtocolException;

import com.fasterxml.jackson.core.JsonParseException;
import com.opensearchserver.client.JsonClient1;
import com.opensearchserver.client.ServerResource;
import com.opensearchserver.client.common.index.TemplateEnum;
import com.opensearchserver.client.common.search.query.SearchFieldQuery;
import com.opensearchserver.client.common.search.query.SearchQueryAbstract;
import com.opensearchserver.client.common.search.query.SearchQueryBatch;
import com.opensearchserver.client.common.search.query.SearchQueryBatch.QueryModeEnum;
import com.opensearchserver.client.common.update.DocumentUpdate;
import com.opensearchserver.client.v1.FieldApi1;
import com.opensearchserver.client.v1.IndexApi1;
import com.opensearchserver.client.v1.UpdateApi1;
import com.opensearchserver.client.v1.field.FieldUpdate;
import com.opensearchserver.client.v1.field.SchemaField;
import com.opensearchserver.client.v1.field.SchemaField.Indexed;
import com.opensearchserver.client.v1.field.SchemaField.Stored;
import com.qwazr.affinities.AffinityManager;
import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResults;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.ServerException;

public class AffinityProcess {

	private static SearchFieldQuery getPreparedQuery(
			AffinityProcessInterface api, String name, Affinity affinity,
			AffinityRequest request) throws IOException, URISyntaxException,
			ServerException {
		switch (affinity.getTypeOfDefault()) {
		case EXACT_MATCH:
			return api.getExactMatchQuery(name, affinity, request);
		default:
		case SCORING:
			prepareScoringUrlCriteria(name, affinity, request);
			if (request.criteria == null || request.criteria.isEmpty())
				throw new ServerException(Status.BAD_REQUEST,
						"The criteria are missing");
			return api.getScoringQuery(name, affinity, request);
		}
	}

	public static AffinityResults execute(String name, Affinity affinity,
			AffinityRequest request) throws URISyntaxException, IOException,
			ServerException {
		AffinityResults results = null;
		boolean cache = request.getCacheOrDefault();
		String requestString = null;
		if (cache) {
			requestString = DigestUtils.md5Hex(JsonMapper.MAPPER
					.writeValueAsString(request));
			results = readCacheEntry(affinity, requestString);
			if (results != null)
				return results;
		}
		AffinityProcessInterface api = checkServerResource(affinity.data);
		SearchFieldQuery query = getPreparedQuery(api, name, affinity, request);
		results = api.executeSearchQuery(name, affinity.data, query);
		if (results == null)
			return null;
		if (cache)
			writeCacheEntry(affinity, requestString, results);
		return results;
	}

	/**
	 * Make one search request for each item in the batch.
	 * 
	 * @param requests
	 *            the affinity request definition
	 * @return a list of results
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	private static List<AffinityResults> executeBatchWithDifferentAffinityData(
			List<AffinityBatchRequest> requests) throws URISyntaxException,
			IOException, ServerException {
		List<AffinityResults> results = new ArrayList<AffinityResults>();
		for (AffinityBatchRequest batchRequest : requests) {
			Affinity affinity = AffinityManager.INSTANCE.get(batchRequest.name);
			if (affinity == null)
				throw new ServerException(Status.NOT_FOUND, "Unknown: "
						+ batchRequest.name);
			AffinityResults result = execute(batchRequest.name, affinity,
					batchRequest);
			results.add(result);
			switch (batchRequest.getActionOrDefault()) {
			case STOP_IF_FOUND:
				if (!result.isEmpty())
					return results;
				break;
			case CONTINUE:
				break;
			}
		}
		return results;
	}

	/**
	 * Use the SearchBatchQuery from OpenSearchServer. Only one request is made.
	 * 
	 * @param affinity
	 *            the affinity instance
	 * @param batchRequests
	 *            a list of affinity requests
	 * @return A list of affinity results
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	private static List<AffinityResults> executeBatchWithSameAffinityData(
			Affinity affinity, List<AffinityBatchRequest> batchRequests)
			throws URISyntaxException, IOException, ServerException {
		AffinityProcessInterface api = checkServerResource(affinity.data);
		SearchQueryBatch batchQuery = new SearchQueryBatch();
		batchQuery.setMode(QueryModeEnum.manual);
		for (AffinityBatchRequest batchRequest : batchRequests) {
			SearchFieldQuery query = getPreparedQuery(api, batchRequest.name,
					affinity, batchRequest);
			switch (batchRequest.getActionOrDefault()) {
			case CONTINUE:
				query.batchAction = SearchQueryAbstract.BatchAction.CONTINUE;
				break;
			default:
			case STOP_IF_FOUND:
				query.batchAction = SearchQueryAbstract.BatchAction.STOP_IF_FOUND;
				break;
			}
			batchQuery.addQuery(query);
		}
		List<AffinityResults> results = new ArrayList<AffinityResults>();
		api.executeSearchQueryBatch(affinity.data, batchQuery, batchRequests,
				results);
		return results;
	}

	public static List<AffinityResults> execute(
			List<AffinityBatchRequest> batchRequests) throws ServerException,
			URISyntaxException, IOException {

		boolean sameServerResource = true;
		Affinity previousAffinity = null;

		// Check if the name exists, and if they all use the same data resource
		for (AffinityBatchRequest batchRequest : batchRequests) {
			Affinity affinity = AffinityManager.INSTANCE.get(batchRequest.name);
			if (affinity == null)
				throw new ServerException(Status.NOT_FOUND, "Unknown: "
						+ batchRequest.name);
			if (previousAffinity != null)
				if (!ServerResource.sameResource(previousAffinity.data,
						affinity.data))
					sameServerResource = false;
			previousAffinity = affinity;
		}
		if (sameServerResource)
			return executeBatchWithSameAffinityData(previousAffinity,
					batchRequests);
		else
			return executeBatchWithDifferentAffinityData(batchRequests);
	}

	/**
	 * @param server
	 *            a server definition
	 * @return the right AffinityProcess implementation
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	private static AffinityProcessInterface checkServerResource(
			ServerResource server) throws IOException, ServerException {
		if (server == null)
			throw new ClientProtocolException(
					"The server to used has not been configured.");
		if (server.url != null)
			return new AffinityProcess1Impl();
		else
			return new AffinityProcess2Impl();
	}

	static void prepareScoringUrlCriteria(String name, Affinity affinity,
			AffinityRequest request) throws JsonParseException,
			ServerException, IOException, URISyntaxException {

		if (request.url == null || request.url.isEmpty())
			return;
		Map<String, String> crawlResult = null;

		// We do the crawl
		if (affinity.crawl_mapping == null || affinity.crawl_mapping.isEmpty())
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"The mapping between crawl and criteria is missing.");
		crawlResult = checkServerResource(affinity.crawl).doUrl(affinity.crawl,
				request.url);

		if (crawlResult == null)
			return;

		// If we don't have previous critera, we create the map
		if (request.criteria == null)
			request.criteria = new LinkedHashMap<String, String>();

		// Copy crawl result to criteria
		for (Map.Entry<String, String> entry : affinity.crawl_mapping
				.entrySet()) {
			String keyCriteria = entry.getKey().intern();
			String keyCrawl = entry.getValue().intern();
			String value = crawlResult.get(keyCrawl);
			if (value != null && !value.isEmpty()
					&& affinity.criteria.containsKey(keyCriteria))
				request.criteria.put(keyCriteria, value);
		}

	}

	static List<String> getReturnedFieldsFromAffinityOrRequest(
			Affinity affinity, AffinityRequest request) {
		ArrayList<String> returnedFields;

		if (request.returned_fields != null) {
			returnedFields = new ArrayList<>(request.returned_fields);
		} else if (affinity.returned_fields != null) {
			returnedFields = new ArrayList<>(affinity.returned_fields);
		} else {
			// TODO Do we really want no field to be returned if none defined in
			// both request and affinity?
			returnedFields = new ArrayList<>(0);
		}

		return returnedFields;
	}

	static String anyGroupBy(Affinity affinity, AffinityRequest request) {
		String group_by = request.group_by;
		if (!StringUtils.isEmpty(group_by))
			return group_by;
		return StringUtils.isEmpty(affinity.group_by) ? null
				: affinity.group_by;
	}

	final static String CACHE_FIELD_KEY = "key";
	final static String CACHE_FIELD_DATE = "date";
	final static String CACHE_FIELD_VALUE = "value";

	final static FastDateFormat CACHE_DATEFORMAT = FastDateFormat
			.getInstance("yyyyMMddHHmmss");

	public static void createCacheIndex(Affinity affinity) throws IOException,
			URISyntaxException, ServerException {
		JsonClient1 client = checkServerResource(affinity.cache).getJsonClient(
				affinity.cache);
		IndexApi1 indexApi = new IndexApi1(client);
		if (indexApi.indexExists(affinity.cache.name))
			return;
		indexApi.createIndex(affinity.cache.name, TemplateEnum.EMPTY_INDEX);
		FieldApi1 fieldApi = new FieldApi1(client);
		List<SchemaField> fields = new ArrayList<SchemaField>();
		fields.add(new SchemaField().setName(CACHE_FIELD_KEY).setIndexed(
				Indexed.YES));
		fields.add(new SchemaField().setName(CACHE_FIELD_DATE).setIndexed(
				Indexed.YES));
		fields.add(new SchemaField().setName(CACHE_FIELD_VALUE)
				.setIndexed(Indexed.NO).setStored(Stored.YES));
		fieldApi.setFields(affinity.cache.name, fields);
		fieldApi.setDefaultUniqueField(affinity.cache.name, CACHE_FIELD_KEY,
				CACHE_FIELD_KEY);
	}

	public static void writeCacheEntry(Affinity affinity, String requestString,
			AffinityResults results) throws URISyntaxException, IOException,
			ServerException {
		JsonClient1 client = checkServerResource(affinity.cache).getJsonClient(
				affinity.cache);
		UpdateApi1 updateApi = new UpdateApi1(client);
		List<DocumentUpdate> documents = new ArrayList<DocumentUpdate>(1);
		documents
				.add(new DocumentUpdate()
						.addField(
								new FieldUpdate(CACHE_FIELD_KEY, requestString,
										null))
						.addField(
								new FieldUpdate(CACHE_FIELD_DATE,
										CACHE_DATEFORMAT.format(System
												.currentTimeMillis()), null))
						.addField(
								new FieldUpdate(CACHE_FIELD_VALUE,
										JsonMapper.MAPPER
												.writeValueAsString(results),
										null)));
		updateApi.updateDocuments(affinity.cache.name, documents);
	}

	public static AffinityResults readCacheEntry(Affinity affinity,
			String requestString) throws URISyntaxException, IOException,
			ServerException {
		return checkServerResource(affinity.cache).readCacheEntry(
				affinity.cache, requestString);
	}
}
