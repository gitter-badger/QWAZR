/**
 * Copyright 2014 OpenSearchServer Inc.
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
package com.qwazr.store.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.util.DateUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.store.AbstractService;

@Path("/store")
public class StoreService extends AbstractService {

	@GET
	@Path("/{path : .+}")
	public Response get(@Context UriInfo uriInfo, @PathParam("path") String path)
			throws IOException {
		File file = StoreManager.INSTANCE.getFile(path);
		if (!file.exists())
			return responseText(Status.NOT_FOUND,
					"Error. Resource not found: " + path).build();
		FileItem fileItem = new FileItem(file);
		if (file.isDirectory())
			return fileItem.headerResponse(responseJson(new StoreResult(file)))
					.build();
		else if (file.isFile())
			return fileItem.headerResponse(
					responseStream(new FileInputStream(file))).build();
		return responseJson(null).build();
	}

	@GET
	@Path("/")
	public Response get(@Context UriInfo uriInfo) throws IOException {
		return get(uriInfo, StringUtils.EMPTY);
	}

	@HEAD
	@Path("/{path : .+}")
	public Response head(@Context UriInfo uriInfo,
			@PathParam("path") String path) throws IOException {
		File file = StoreManager.INSTANCE.getFile(path);
		if (!file.exists())
			return responseText(Status.NOT_FOUND,
					"Error. Resource not found: " + path).build();
		return new FileItem(file).headerResponse(Response.ok()).build();
	}

	@HEAD
	@Path("/")
	public Response head(@Context UriInfo uriInfo) throws IOException {
		return head(uriInfo, StringUtils.EMPTY);
	}

	@PUT
	@Path("/{path : .+}")
	public Response put(@Context UriInfo uriInfo,
			@PathParam("path") String path, InputStream inputStream)
			throws IOException {
		File file = StoreManager.INSTANCE.getFile(path);
		if (file.exists() && file.isDirectory())
			return responseText(Status.CONFLICT,
					"Error. A directory already exists: " + path).build();
		File tmpFile = File.createTempFile("oss-cluster-node", ".upload");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(tmpFile);
			IOUtils.copy(inputStream, fos);
			fos.close();
			if (file.exists())
				file.delete();
			tmpFile.renameTo(file);
			tmpFile = null;
			return new FileItem(file)
					.headerResponse(Response.status(Status.OK)).build();
		} finally {
			if (fos != null)
				IOUtils.closeQuietly(fos);
			if (tmpFile != null)
				tmpFile.delete();
		}
	}

	@PUT
	@Path("/")
	public Response put(@Context UriInfo uriInfo, InputStream inputStream)
			throws IOException {
		return put(uriInfo, StringUtils.EMPTY, inputStream);
	}

	@POST
	@Path("/{path : .+}")
	public Response post(@Context UriInfo uriInfo,
			@PathParam("path") String path) throws IOException {
		File file = StoreManager.INSTANCE.getFile(path);
		if (file.exists())
			return responseText(Status.CONFLICT,
					"Error. Resource already exists: " + path).build();
		file.mkdir();
		if (file.exists() && file.isDirectory())
			return new FileItem(file).headerResponse(
					Response.status(Status.CREATED)).build();
		File parentFile = file.getParentFile();
		if (parentFile == null || !parentFile.exists())
			return responseText(Status.NOT_ACCEPTABLE,
					"The parent directory does not exists: " + path).build();
		if (!parentFile.isDirectory())
			return responseText(Status.NOT_ACCEPTABLE,
					"The parent path is not a directory: " + path).build();
		throw new IOException("Unexpected internal error");
	}

	@POST
	@Path("/")
	public Response post(@Context UriInfo uriInfo) throws IOException {
		return post(uriInfo, StringUtils.EMPTY);
	}

	@DELETE
	@Path("/{path : .+}")
	public Response delete(@Context UriInfo uriInfo,
			@PathParam("path") String path) throws IOException {
		File file = StoreManager.INSTANCE.getFile(path);
		if (!file.exists())
			return responseText(Status.NOT_FOUND,
					"Error. Resource not found: " + path).build();
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null && files.length > 0)
				return responseText(Status.NOT_ACCEPTABLE,
						"The directory is not empty").build();
		}
		file.delete();
		if (file.exists())
			return responseText(Status.INTERNAL_SERVER_ERROR,
					"Unable to delete the resource: " + path).build();
		return responseText(Status.OK, "Resource deleted: " + path).build();
	}

	@DELETE
	@Path("/")
	public Response delete(@Context UriInfo uriInfo) throws IOException {
		return delete(uriInfo, StringUtils.EMPTY);
	}

	@JsonInclude(Include.NON_EMPTY)
	public static class StoreResult {

		@JsonInclude(Include.NON_NULL)
		public final List<FileItem> items;

		StoreResult(File file) {
			File[] files = file.listFiles();
			if (files == null) {
				items = null;
				return;
			}
			items = new ArrayList<FileItem>(files.length);
			for (File f : files)
				items.add(new FileItem(f));
		}
	}

	@JsonInclude(Include.NON_EMPTY)
	public static class FileItem {

		public final String name;
		public final Type type;
		public final Date lastModified;
		public final Long size;

		FileItem(File file) {
			name = file.getName();
			if (file.isDirectory())
				type = Type.DIRECTORY;
			else if (file.isFile())
				type = Type.FILE;
			else
				type = Type.UNKNOWN;
			lastModified = new Date(file.lastModified());
			size = type == Type.FILE ? file.length() : null;
		}

		public static enum Type {
			FILE, DIRECTORY, UNKNOWN
		};

		final private ResponseBuilder headerResponse(ResponseBuilder builder) {
			builder.header("X-OSS-Cluster-Store-Type", type);
			if (size != null)
				builder.header("X-OSS-Cluster-Store-Length", size);
			if (lastModified != null)
				builder.header("Last-Modified",
						DateUtil.formatDate(lastModified));
			return builder;
		}
	}

}
