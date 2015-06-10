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
package com.qwazr.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeMarkerTool extends AbstractTool implements TemplateLoader {

	public final String output_encoding;
	public final String default_encoding;
	public final String default_content_type;

	@JsonIgnore
	protected Configuration cfg = null;

	private final static String DEFAULT_CHARSET = "UTF-8";
	private final static String DEFAULT_CONTENT_TYPE = "text/html";

	public FreeMarkerTool() {
		output_encoding = null;
		default_encoding = null;
		default_content_type = null;
	}

	@Override
	public void load(String contextId) {
		cfg = new Configuration(Configuration.VERSION_2_3_22);
		cfg.setTemplateLoader(this);
		cfg.setOutputEncoding(output_encoding == null ? DEFAULT_CHARSET
				: output_encoding);
		cfg.setDefaultEncoding(default_encoding == null ? DEFAULT_CHARSET
				: default_encoding);
		cfg.setLocalizedLookup(false);
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	@Override
	public void unload(String contextId) {
		cfg.clearTemplateCache();
	}

	@Override
	public Object findTemplateSource(String path) throws IOException {
		File file = new File(path);
		return file.exists() && file.isFile() ? file : null;
	}

	@Override
	public long getLastModified(Object templateSource) {
		return ((File) templateSource).lastModified();
	}

	@Override
	public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		return new FileReader((File) templateSource);
	}

	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {
	}

	public void template(String templatePath, Map<?, ?> dataModel,
			HttpServletResponse response) throws TemplateException, IOException {
		if (response.getContentType() == null)
			response.setContentType(default_content_type == null ? DEFAULT_CONTENT_TYPE
					: default_content_type);
		response.setCharacterEncoding(DEFAULT_CHARSET);
		Template template = cfg.getTemplate(templatePath);
		template.process(dataModel, response.getWriter());
	}

	public String template(String templatePath, Map<?, ?> dataModel)
			throws TemplateException, IOException {
		Template template = cfg.getTemplate(templatePath);
		StringWriter stringWriter = new StringWriter();
		try {
			template.process(dataModel, stringWriter);
			return stringWriter.toString();
		} finally {
			IOUtils.closeQuietly(stringWriter);
		}
	}

}
