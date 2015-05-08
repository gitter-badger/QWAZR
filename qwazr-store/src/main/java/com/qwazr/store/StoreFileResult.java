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
 */
package com.qwazr.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.jboss.resteasy.util.DateUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class StoreFileResult {

	public final String name;
	public final Type type;
	public final Date lastModified;
	public final Long size;

	public final List<StoreFileResult> childs;

	StoreFileResult(File file, boolean retrieveChilds) {
		name = file.getName();
		if (file.isDirectory()) {
			type = Type.DIRECTORY;
			if (retrieveChilds) {
				File[] files = file.listFiles();
				if (files != null) {
					childs = new ArrayList<StoreFileResult>(files.length);
					for (File f : files)
						childs.add(new StoreFileResult(f, false));
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

	final ResponseBuilder headerResponse(ResponseBuilder builder) {
		builder.header("X-OSS-Cluster-Store-Type", type);
		if (size != null)
			builder.header("X-OSS-Cluster-Store-Length", size);
		if (lastModified != null)
			builder.header("Last-Modified", DateUtil.formatDate(lastModified));
		return builder;
	}
}
