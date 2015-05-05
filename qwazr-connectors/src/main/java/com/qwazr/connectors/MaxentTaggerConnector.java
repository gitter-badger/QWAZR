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
package com.qwazr.connectors;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MaxentTaggerConnector extends AbstractConnector {

	public final String model_path = null;

	private MaxentTagger tagger = null;

	@Override
	public void load(ConnectorContext context) {
		File modelFile = new File(context.getContextDirectory(), model_path);
		tagger = new MaxentTagger(modelFile.getAbsolutePath());
	}

	@Override
	public void unload(ConnectorContext context) {
	}

	@JsonIgnore
	public List<List<TaggedWord>> tag(String text) {
		if (StringUtils.isEmpty(text))
			return null;
		StringReader sr = new StringReader(text);
		try {
			List<List<HasWord>> sentences = MaxentTagger.tokenizeText(sr);
			List<List<TaggedWord>> taggedSentences = new ArrayList<List<TaggedWord>>();
			for (List<HasWord> sentence : sentences)
				taggedSentences.add(tagger.tagSentence(sentence));
			return taggedSentences;
		} finally {
			sr.close();
		}
	}
}
