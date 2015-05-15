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
 */
package com.qwazr.extractor.parser;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpbf.extractor.PublisherTextExtractor;
import org.apache.poi.hpsf.SummaryInformation;

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserDocument;
import com.qwazr.extractor.ParserField;

public class Publisher extends ParserAbstract {

	public static final String[] DEFAULT_MIMETYPES = { "application/x-mspublisher" };

	public static final String[] DEFAULT_EXTENSIONS = { "pub" };

	final protected static ParserField TITLE = ParserField.newString("title",
			"The title of the document");

	final protected static ParserField AUTHOR = ParserField.newString("author",
			"The name of the author");

	final protected static ParserField CREATION_DATE = ParserField.newDate(
			"creation_date", null);

	final protected static ParserField MODIFICATION_DATE = ParserField.newDate(
			"modification_date", null);

	final protected static ParserField KEYWORDS = ParserField.newString(
			"keywords", null);

	final protected static ParserField SUBJECT = ParserField.newString(
			"subject", "The subject of the document");

	final protected static ParserField COMMENTS = ParserField.newString(
			"comments", null);

	final protected static ParserField CONTENT = ParserField.newString(
			"content", "The content of the document");

	final protected static ParserField LANG_DETECTION = ParserField.newString(
			"lang_detection", "Detection of the language");

	final protected static ParserField[] FIELDS = { TITLE, AUTHOR,
			CREATION_DATE, MODIFICATION_DATE, KEYWORDS, SUBJECT, CONTENT,
			LANG_DETECTION };

	public Publisher() {
	}

	@Override
	protected ParserField[] getParameters() {
		return null;
	}

	@Override
	protected ParserField[] getFields() {
		return FIELDS;
	}

	@Override
	protected String[] getDefaultExtensions() {
		return DEFAULT_EXTENSIONS;
	}

	@Override
	protected String[] getDefaultMimeTypes() {
		return DEFAULT_MIMETYPES;
	}

	@Override
	protected void parseContent(InputStream inputStream, String extension,
			String mimeType) throws Exception {
		PublisherTextExtractor extractor = null;
		try {
			extractor = new PublisherTextExtractor(inputStream);
			SummaryInformation info = extractor.getSummaryInformation();

			if (info != null) {
				metas.add(TITLE, info.getTitle());
				metas.add(AUTHOR, info.getAuthor());
				metas.add(SUBJECT, info.getSubject());
				metas.add(CREATION_DATE, info.getCreateDateTime());
				metas.add(MODIFICATION_DATE, info.getLastSaveDateTime());
				metas.add(CONTENT, info.getKeywords());
				metas.add(COMMENTS, info.getComments());
			}
			String text = extractor.getText();
			if (StringUtils.isEmpty(text))
				return;
			ParserDocument result = getNewParserDocument();
			result.add(CONTENT, text);
			result.add(LANG_DETECTION, languageDetection(CONTENT, 10000));
		} finally {
			if (extractor != null)
				IOUtils.closeQuietly(extractor);
		}
	}
}
