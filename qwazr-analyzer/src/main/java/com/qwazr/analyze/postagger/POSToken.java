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
