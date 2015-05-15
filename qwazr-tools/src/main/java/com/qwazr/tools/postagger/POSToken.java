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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class POSToken {

	public final String token;
	public final Integer start_pos;
	public final Integer end_pos;
	public final String[] lemmas;
	public final String[] tags;

	public POSToken() {
		token = null;
		start_pos = null;
		end_pos = null;
		lemmas = null;
		tags = null;
	}

	private POSToken(AnalyzedTokenReadings readings) {
		token = readings.getToken();
		start_pos = readings.getStartPos();
		end_pos = readings.getEndPos();
		List<String> tags = new ArrayList<String>();
		Set<String> lemmas = new TreeSet<String>();
		for (AnalyzedToken analysedToken : readings.getReadings()) {
			String lemma = analysedToken.getLemma();
			if (lemma != null)
				lemmas.add(lemma);
			String tag = analysedToken.getPOSTag();
			if (tag != null)
				tags.add(tag);
		}
		this.lemmas = lemmas.toArray(new String[lemmas.size()]);
		this.tags = tags.toArray(new String[tags.size()]);
	}

	@JsonIgnore
	static List<POSToken> newList(List<AnalyzedTokenReadings> readingsList) {
		List<POSToken> posTokenList = new ArrayList<POSToken>(
				readingsList.size());
		for (AnalyzedTokenReadings readings : readingsList)
			posTokenList.add(new POSToken(readings));
		return posTokenList;
	}
}
