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
package com.qwazr.store.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.resteasy.util.DateUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.RestApplication;

@JsonInclude(Include.NON_EMPTY)
public class StoreFileResult {

	private final File file;
	private FileInputStream inputStream;

	public final Type type;
	public final Date lastModified;
	public final Long size;

	public final Map<String, StoreFileResult> childs;

	public StoreFileResult() {
		file = null;
		inputStream = null;
		type = null;
		lastModified = null;
		size = null;
		childs = null;
	}

	StoreFileResult(File file, boolean retrieveChilds) {
		inputStream = null;
		this.file = file;
		if (file.isDirectory()) {
			type = Type.DIRECTORY;
			if (retrieveChilds) {
				File[] files = file.listFiles();
				if (files != null) {
					childs = new TreeMap<String, StoreFileResult>();
					for (File f : files)
						childs.put(f.getName(), new StoreFileResult(f, false));
				} else
					childs = null;
			} else
				childs = null;
		} else if (file.isFile()) {
			type = Type.FILE;
			childs = null;
		} else {
			type = Type.UNKNOWN;
			childs = null;
		}
		lastModified = new Date(file.lastModified());
		size = type == Type.FILE ? file.length() : null;
	}

	public static enum Type {
		FILE, DIRECTORY, UNKNOWN
	};

	void free() {
		if (inputStream != null)
			IOUtils.closeQuietly(inputStream);
	}

	private final static String QWAZR_TYPE = "X-QWAZR-Store-Type";
	private final static String QWAZR_SIZE = "X-QWAZR-Store-Size";
	private final static String QWAZR_ADDR = "X-QWAZR-Store-Addr";
	private final static String LAST_MODIFIED = "Last-Modified";

	final ResponseBuilder buildHeader(ResponseBuilder builder) {
		builder.header(QWAZR_TYPE, type);
		if (size != null)
			builder.header(QWAZR_SIZE, size);
		if (lastModified != null)
			builder.header(LAST_MODIFIED, DateUtil.formatDate(lastModified));
		return builder;
	}

	final void buildEntity(ResponseBuilder builder) throws IOException {
		if (type == Type.FILE) {
			inputStream = new FileInputStream(file);
			builder.entity(inputStream)
					.type(MediaType.APPLICATION_OCTET_STREAM);
		} else if (type == Type.DIRECTORY) {
			builder.entity(JsonMapper.MAPPER.writeValueAsString(this)).type(
					RestApplication.APPLICATION_JSON_UTF8);
		}
	}

	final static void buildHeaders(HttpResponse response, URI uri,
			ResponseBuilder builder) {
		Header header = response.getFirstHeader(QWAZR_TYPE);
		if (header != null)
			builder.header(QWAZR_TYPE, header.getValue());
		header = response.getFirstHeader(QWAZR_SIZE);
		if (header != null)
			builder.header(QWAZR_SIZE, header.getValue());
		header = response.getFirstHeader(LAST_MODIFIED);
		if (header != null)
			builder.header(LAST_MODIFIED, header.getValue());
		if (uri != null)
			builder.header(QWAZR_ADDR, uri.toASCIIString());
	}

	final static Type getType(Response response) {
		String h = response.getHeaderString(QWAZR_TYPE);
		if (h == null)
			return Type.UNKNOWN;
		try {
			return Type.valueOf(h);
		} catch (IllegalArgumentException e) {
			return Type.UNKNOWN;
		}
	}

	final static URI getAddr(Response response) throws URISyntaxException {
		String u = response.getHeaderString(QWAZR_ADDR);
		if (u == null)
			throw new NullPointerException("No address");
		return new URI(u);
	}

	static class DirMerger {

		StoreFileResult mergedDirResult = null;

		final void syncMerge(StoreFileResult dirResult) {
			synchronized (this) {
				if (dirResult == null)
					return;
				if (mergedDirResult == null) {
					mergedDirResult = dirResult;
					return;
				}
				if (dirResult.childs == null)
					return;
				if (mergedDirResult.childs == null) {
					mergedDirResult = dirResult;
					return;
				}
				mergedDirResult.childs.putAll(dirResult.childs);
			}
		}
	}

}
