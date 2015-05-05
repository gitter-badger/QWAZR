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
