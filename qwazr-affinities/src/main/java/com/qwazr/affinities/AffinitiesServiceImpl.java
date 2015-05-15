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
package com.qwazr.affinities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;

import com.qwazr.affinities.model.Affinity;
import com.qwazr.affinities.model.AffinityBatchRequest;
import com.qwazr.affinities.model.AffinityRequest;
import com.qwazr.affinities.model.AffinityResults;
import com.qwazr.affinities.process.AffinityProcess;
import com.qwazr.utils.server.ServerException;

@Path("/")
public class AffinitiesServiceImpl implements AffinitiesServiceInterface {

	@Override
	public Set<String> list(@Context UriInfo uriInfo) {
		return AffinityManager.INSTANCE.nameSet();
	}

	@Override
	public Affinity get(UriInfo uriInfo, String name) throws ServerException {
		Affinity affinity = AffinityManager.INSTANCE.get(name);
		if (affinity == null)
			throw new ServerException(Status.NOT_FOUND, "Unknown: " + name);
		return affinity;
	}

	@Override
	public Affinity delete(UriInfo uriInfo, String name)
			throws ServerException, IOException {
		Affinity affinity = AffinityManager.INSTANCE.delete(name);
		if (affinity == null)
			throw new ServerException(Status.NOT_FOUND, "Unknown: " + name);
		return affinity;
	}

	@Override
	public Affinity create(UriInfo uriInfo, @PathParam("name") String name,
			Affinity affinity) throws ServerException {
		try {
			AffinityManager.INSTANCE.set(name, affinity);
			if (affinity.cache != null)
				AffinityProcess.createCacheIndex(affinity);
			return affinity;
		} catch (Exception e) {
			throw new ServerException(e);
		}
	}

	@Override
	public AffinityResults recommendRequest(UriInfo uriInfo, String name,
			AffinityRequest request) throws ServerException {
		if (request == null)
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"The request is missing.");
		Affinity affinity = AffinityManager.INSTANCE.get(name);
		if (affinity == null)
			throw new ServerException(Status.NOT_FOUND, "Unknown: " + name);
		try {
			return AffinityProcess.execute(name, affinity, request);
		} catch (Exception e) {
			throw new ServerException(e);
		}
	}

	@Override
	public List<AffinityResults> recommendRequests(UriInfo uriInfo,
			List<AffinityBatchRequest> requests) throws ServerException {
		if (requests == null || requests.isEmpty())
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"The requests are missing.");
		try {
			return AffinityProcess.execute(requests);
		} catch (Exception e) {
			throw new ServerException(e);
		}
	}

	@Override
	public List<List<AffinityResults>> recommendBatchRequests(UriInfo uriInfo,
			List<List<AffinityBatchRequest>> batchRequests)
			throws ServerException {
		if (batchRequests == null || batchRequests.isEmpty())
			throw new ServerException(Status.NOT_ACCEPTABLE,
					"The requests are missing.");
		List<List<AffinityResults>> results = new ArrayList<List<AffinityResults>>(
				batchRequests.size());
		for (List<AffinityBatchRequest> requests : batchRequests)
			results.add(recommendRequests(uriInfo, requests));
		return results;
	}

	@Override
	public String refererTemplateRecommend(UriInfo uriInfo, String name,
			String headerReferer, String queryReferer) throws ServerException {
		Affinity affinity = AffinityManager.INSTANCE.get(name);
		if (affinity == null)
			throw new ServerException(Status.NOT_FOUND, "Unknown: " + name);
		String referer = queryReferer;
		AffinityResults results = null;
		if (StringUtils.isEmpty(referer))
			referer = headerReferer;
		try {
			if (!StringUtils.isEmpty(referer)) {
				AffinityRequest request = new AffinityRequest().setUrl(referer)
						.setCache(affinity.cache != null);
				results = AffinityProcess.execute(name, affinity, request);
			}
		} catch (Exception e) {
			System.err.print(e);
		}
		return "<html><p>" + referer + " - " + results + "</p></html>";
	}
}
