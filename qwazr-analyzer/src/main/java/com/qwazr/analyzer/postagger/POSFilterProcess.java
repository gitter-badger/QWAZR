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
package com.qwazr.analyzer.postagger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class POSFilterProcess {

	private final POSFilter filter;
	private final List<List<POSToken>> results;

	POSFilterProcess(POSFilter filter, List<List<POSToken>> sentences) {
		this.filter = filter;
		results = new ArrayList<List<POSToken>>();
		if (sentences == null)
			return;
		for (List<POSToken> sentence : sentences)
			evaluate(sentence);
	}

	private List<POSToken> getNewResult(POSToken token) {
		List<POSToken> result = new ArrayList<POSToken>();
		result.add(token);
		return result;
	}

	private void evaluate(List<POSToken> sentence) {
		if (sentence == null)
			return;
		Iterator<POSToken> iterator = sentence.iterator();
		while (iterator.hasNext()) {
			POSToken token = iterator.next();
			if (filter.any_of.contains(token.token))
				positiveFilter(iterator, getNewResult(token));
		}
	}

	private void positiveFilter(Iterator<POSToken> iterator,
			List<POSToken> result) {
		int maxDistance = filter.max_distance == null ? 0 : filter.max_distance;
		int distance = 0;
		while (iterator.hasNext()) {
			POSToken token = iterator.next();
			if (!filter.any_of.contains(token.token)) {
				if (++distance > maxDistance)
					break;
				continue;
			}
			distance = 0;
			result.add(token);
		}
		if (filter.min_size != null && result.size() < filter.min_size)
			return;
		results.add(result);
	}

	List<List<POSToken>> getResult() {
		return results;
	}

}
