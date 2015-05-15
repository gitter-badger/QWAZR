/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.tools.postagger;

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
