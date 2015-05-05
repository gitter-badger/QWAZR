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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PegDownConnector extends AbstractConnector {

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

	private PegDownProcessor pegdownProcessor = null;
	private File viewDir = null;

	@Override
	public void load(ConnectorContext context) {
		int extensionsValue = 0;
		if (extensions != null)
			for (ExtensionEnum extensionEnum : extensions)
				extensionsValue |= extensionEnum.value;
		pegdownProcessor = new PegDownProcessor(extensionsValue);
		viewDir = new File(context.getContextDirectory(), "view");
	}

	@Override
	public void unload(ConnectorContext context) {
	}

	public String toHtml(String path) throws IOException {
		FileInputStream fileInputStream = null;
		File file = new File(viewDir, path);
		try {
			if (!file.exists())
				throw new FileNotFoundException("File not found: " + path);
			fileInputStream = new FileInputStream(file);
			String content = IOUtils.toString(fileInputStream, "UTF-8");
			synchronized (pegdownProcessor) {
				return pegdownProcessor.markdownToHtml(content);
			}
		} finally {
			if (fileInputStream != null)
				IOUtils.closeQuietly(fileInputStream);
		}
	}
}
