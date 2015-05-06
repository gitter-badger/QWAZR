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
package com.qwazr.analyzer.markdown;

import java.util.List;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkdownProcessorDefinition {

	public List<ExtensionEnum> extensions;

	/**
	 * 
	 * @see org.pegdown.Extensions
	 *
	 */
	public enum ExtensionEnum {

		none(Extensions.NONE), smarts(Extensions.SMARTS), quotes(
				Extensions.QUOTES), smartypants(Extensions.SMARTYPANTS), abbrevations(
				Extensions.ABBREVIATIONS), hardwraps(Extensions.HARDWRAPS), autolinks(
				Extensions.AUTOLINKS), tables(Extensions.TABLES), definitions(
				Extensions.DEFINITIONS), fenced_code_blocks(
				Extensions.FENCED_CODE_BLOCKS), wikilinks(Extensions.WIKILINKS), strikethrough(
				Extensions.STRIKETHROUGH), anchorlinks(Extensions.ANCHORLINKS), all(
				Extensions.ALL);

		private final int value;

		private ExtensionEnum(int value) {
			this.value = value;
		}
	}

	@JsonIgnore
	PegDownProcessor getNewPegdownProcessor() {
		int extensionsValue = 0;
		if (extensions != null)
			for (ExtensionEnum extensionEnum : extensions)
				extensionsValue |= extensionEnum.value;
		return new PegDownProcessor(extensionsValue);
	}

}
