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
package com.qwazr.analyze.postagger;

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
