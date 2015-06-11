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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.Languages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.tools.AbstractTool;
import com.qwazr.utils.server.ServerException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class POSTaggerTool extends AbstractTool {

	public String lang;

	@Override
	public void load(File dataDir) {
	}

	@Override
	public void unload() {
	}

	private Language getLanguage(String languageName) {
		Language language = Languages.getLanguageForShortName(languageName);
		if (language == null)
			language = Languages.getLanguageForName(languageName);
		if (language == null)
			throw new RuntimeException("Language not found: " + languageName);
		return language;
	}

	public List<List<POSToken>> analyze(String languageName, String text)
			throws ServerException, IOException {
		Language language = getLanguage(languageName);
		List<String> sentences = language.getSentenceTokenizer().tokenize(text);
		if (sentences == null)
			return null;
		List<List<POSToken>> taggedSentences = new ArrayList<List<POSToken>>(
				sentences.size());
		for (String sentence : sentences) {
			List<String> words = language.getWordTokenizer().tokenize(sentence);
			List<AnalyzedTokenReadings> readings = language.getTagger().tag(
					words);
			taggedSentences.add(POSToken.newList(readings));
		}
		return taggedSentences;
	}

}
