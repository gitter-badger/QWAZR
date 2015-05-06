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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response.Status;

import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.AbstractServer;
import com.qwazr.utils.server.ServerException;

public class LanguageManager extends DirectoryJsonManager<POSFilter> {

	private static final Logger logger = LoggerFactory
			.getLogger(LanguageManager.class);

	public static volatile LanguageManager INSTANCE = null;

	public static void load(AbstractServer server, File rootDirectory)
			throws IOException {
		if (INSTANCE != null)
			throw new IOException("Already loaded");
		File filtersDirectory = new File(rootDirectory, "posfilters");
		if (!filtersDirectory.exists())
			filtersDirectory.mkdir();
		INSTANCE = new LanguageManager(server, filtersDirectory);

	}

	private LanguageManager(AbstractServer server, File filtersDirectory)
			throws IOException {
		super(filtersDirectory, POSFilter.class);
	}

	Map<String, String> getAvailableLanguages() {
		Map<String, String> languageMap = new TreeMap<String, String>();
		for (Language language : Languages.get())
			languageMap.put(language.getShortName(), language.getName());
		return languageMap;
	}

	List<List<POSToken>> analyze(String languageName, String text)
			throws ServerException, IOException {
		Language language = Languages.getLanguageForShortName(languageName);
		if (language == null)
			language = Languages.getLanguageForName(languageName);
		if (language == null)
			throw new ServerException(Status.NOT_FOUND, "Language not found: "
					+ languageName);
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
