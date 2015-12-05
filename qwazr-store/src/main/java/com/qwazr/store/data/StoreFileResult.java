/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.store.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.utils.json.JsonMapper;
import com.qwazr.utils.server.RestApplication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.DateUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@JsonInclude(Include.NON_EMPTY)
public class StoreFileResult {

    private final File file;
    private FileInputStream inputStream;

    public final Type type;
    public final Date last_modified;
    public final Long size;

    public final Map<String, StoreFileResult> directories;
    public final Map<String, Map<String, StoreFileResult>> files;

    public StoreFileResult() {
	this((Type) null, false);
    }

    StoreFileResult(Type type, boolean instanceMaps) {
	file = null;
	inputStream = null;
	this.type = type;
	last_modified = null;
	size = null;
	if (instanceMaps) {
	    directories = new TreeMap<String, StoreFileResult>();
	    files = new TreeMap<String, Map<String, StoreFileResult>>();
	} else {
	    directories = null;
	    files = null;
	}
    }

    StoreFileResult(File file, boolean retrieveChilds) {
	inputStream = null;
	this.file = file;
	if (file.isDirectory()) {
	    type = Type.DIRECTORY;
	    if (retrieveChilds) {
		// We need at least an empty structure for the merging process
		directories = new TreeMap<String, StoreFileResult>();
		files = new TreeMap<String, Map<String, StoreFileResult>>();
		File[] fileList = file.listFiles((FileFilter) HiddenFileFilter.VISIBLE);
		if (fileList != null) {
		    for (File f : fileList) {
			if (f.isDirectory())
			    directories.put(f.getName(), new StoreFileResult(f, false));
			else if (f.isFile()) {
			    Map<String, StoreFileResult> nodeMap = new TreeMap<String, StoreFileResult>();
			    nodeMap.put(ClusterManager.INSTANCE.myAddress, new StoreFileResult(f, false));
			    files.put(f.getName(), nodeMap);
			}
		    }
		}
	    } else {
		directories = null;
		files = null;
	    }
	} else if (file.isFile()) {
	    type = Type.FILE;
	    directories = null;
	    files = null;
	} else {
	    type = Type.UNKNOWN;
	    directories = null;
	    files = null;
	}
	last_modified = new Date(file.lastModified());
	size = type == Type.FILE ? file.length() : null;
    }

    public static enum Type {
	FILE, DIRECTORY, UNKNOWN
    }

    ;

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
	if (last_modified != null)
	    builder.header(LAST_MODIFIED, DateUtils.formatDate(last_modified));
	return builder;
    }

    final void buildEntity(ResponseBuilder builder) throws IOException {
	if (type == Type.FILE) {
	    inputStream = new FileInputStream(file);
	    builder.entity(inputStream).type(MediaType.APPLICATION_OCTET_STREAM);
	} else if (type == Type.DIRECTORY) {
	    builder.entity(JsonMapper.MAPPER.writeValueAsString(this)).type(RestApplication.APPLICATION_JSON_UTF8);
	}
    }

    final static void buildHeaders(HttpResponse response, URI uri, ResponseBuilder builder) {
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

    @JsonIgnore
    final void merge(StoreFileResult dirResult) {
	if (dirResult == null)
	    return;
	if (dirResult.directories != null)
	    directories.putAll(dirResult.directories);
	if (dirResult.files != null) {
	    for (Map.Entry<String, Map<String, StoreFileResult>> entry : dirResult.files.entrySet()) {
		String name = entry.getKey();
		Map<String, StoreFileResult> nodeMap = entry.getValue();
		Map<String, StoreFileResult> mergeNodeMap = files.get(name);
		if (mergeNodeMap == null)
		    files.put(name, nodeMap);
		else
		    mergeNodeMap.putAll(nodeMap);
	    }
	}
    }

    /**
     * Check if the file are identical. Same type, same last_modified date, and
     * same size.
     *
     * @param o
     *            the object to compare
     * @return true if the file are the same
     */
    public final boolean repairCheckFileEquals(StoreFileResult o) {
	if (o.type == null || type == null)
	    throw new IllegalArgumentException("The type is node defined");
	if (o.type != type)
	    return false;
	if (size == null || o.size == null)
	    throw new IllegalArgumentException("The size is node defined");
	if (!size.equals(o.size))
	    return false;
	if (last_modified == null || o.last_modified == null)
	    throw new IllegalArgumentException("The last modified date is node defined");
	return last_modified.equals(o.last_modified);
    }
}
