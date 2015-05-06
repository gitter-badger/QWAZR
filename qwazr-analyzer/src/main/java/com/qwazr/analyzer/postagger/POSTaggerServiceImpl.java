/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.analyzer.postagger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.server.ServerException;

public class POSTaggerServiceImpl implements POSTaggerServiceInterface {

	@Override
	public Map<String, String> getAvailableLanguages() {
		return LanguageManager.INSTANCE.getAvailableLanguages();
	}

	@Override
	public List<List<POSToken>> analyze(String languageName, InputStream content) {
		try {
			return LanguageManager.INSTANCE.analyze(languageName,
					IOUtils.toString(content, "UTF-8"));
		} catch (IOException | ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Set<String> getFilters() {
		return LanguageManager.INSTANCE.nameSet();
	}

	private POSFilter getFilterOrNotFound(String filterName)
			throws ServerException {
		POSFilter filter = LanguageManager.INSTANCE.get(filterName);
		if (filter == null)
			throw new ServerException(Status.NOT_FOUND, "Filter not found: "
					+ filterName);
		return filter;
	}

	@Override
	public POSFilter getFilter(String filterName) {
		try {
			return getFilterOrNotFound(filterName);
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public POSFilter createUpdateFilter(String filterName, POSFilter filter) {
		try {
			LanguageManager.INSTANCE.set(filterName, filter);
			return filter;
		} catch (IOException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public POSFilter deleteFilter(String filterName) {
		try {
			POSFilter filter = LanguageManager.INSTANCE.delete(filterName);
			if (filter == null)
				throw new ServerException("Filter not found: " + filterName);
			return filter;
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public List<List<POSToken>> applyFilter(String filterName,
			List<List<POSToken>> sentences) {
		try {
			if (sentences == null)
				throw new ServerException("No sentences");
			return new POSFilterProcess(getFilterOrNotFound(filterName),
					sentences).getResult();
		} catch (ServerException e) {
			throw ServerException.getJsonException(e);
		}
	}
}
