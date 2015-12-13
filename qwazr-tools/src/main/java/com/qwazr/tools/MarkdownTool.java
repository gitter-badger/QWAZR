/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.io.FileUtils;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkdownTool extends AbstractTool {

	public List<ExtensionEnum> extensions;

	/**
	 * @see org.pegdown.Extensions
	 */
	public enum ExtensionEnum {

		none(Extensions.NONE),
		smarts(Extensions.SMARTS),
		quotes(Extensions.QUOTES),
		smartypants(Extensions.SMARTYPANTS),
		abbrevations(Extensions.ABBREVIATIONS),
		hardwraps(Extensions.HARDWRAPS),
		autolinks(Extensions.AUTOLINKS),
		tables(Extensions.TABLES),
		definitions(Extensions.DEFINITIONS),
		fenced_code_blocks(Extensions.FENCED_CODE_BLOCKS),
		wikilinks(Extensions.WIKILINKS),
		strikethrough(Extensions.STRIKETHROUGH),
		anchorlinks(Extensions.ANCHORLINKS),
		all(Extensions.ALL);

		private final int value;

		ExtensionEnum(int value) {
			this.value = value;
		}
	}

	@JsonIgnore
	private PegDownProcessor pegDownProcessor = null;

	@Override
	public void load(File dataDir) {
		int extensionsValue = 0;
		if (extensions != null)
			for (ExtensionEnum extensionEnum : extensions)
				extensionsValue |= extensionEnum.value;
		pegDownProcessor = new PegDownProcessor(extensionsValue);
	}

	@Override
	public void unload() {
	}

	public String toHtml(String input) {
		return pegDownProcessor.markdownToHtml(input);
	}

	public String toHtml(File file) throws IOException {
		return pegDownProcessor.markdownToHtml(FileUtils.readFileToString(file, "UTF-8"));
	}

	public String toHtml(File file, String encoding) throws IOException {
		return pegDownProcessor.markdownToHtml(FileUtils.readFileToString(file, encoding));
	}
}
