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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HdfsConnector extends AbstractConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(HdfsConnector.class);

	public final String config_path = null;

	public final List<String> config_files = null;

	@JsonIgnore
	private FileSystem fileSystem;

	@JsonIgnore
	private Configuration configuration;

	@Override
	public void load(ConnectorContext context) {
		configuration = new Configuration();

		try {
			if (config_files != null) {
				for (String configFile : config_files) {
					File file = new File(config_path, configFile);
					if (!file.exists())
						throw new IOException("Configuration file not found: "
								+ file.getAbsolutePath());
					configuration
							.addResource(new Path(config_path, configFile));
				}
			}
			configuration.set("fs.hdfs.impl",
					org.apache.hadoop.hdfs.DistributedFileSystem.class
							.getName());
			configuration.set("fs.file.impl",
					org.apache.hadoop.fs.LocalFileSystem.class.getName());
			logger.info("*** HDFS configuration ***: "
					+ configuration.toString());
			fileSystem = FileSystem.get(configuration);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void unload(ConnectorContext context) {
		if (fileSystem != null) {
			try {
				logger.info("Closing HDFS");
				fileSystem.close();
				fileSystem = null;
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void checkFileSystem() throws IOException {
		if (fileSystem == null)
			throw new IOException("No filesystem available");
	}

	public void write(String pathString, String content) throws IOException {
		checkFileSystem();
		if (content == null || content.length() == 0)
			throw new IOException("No content");
		logger.info("Writing text: " + pathString);
		Path path = new Path(pathString);
		FSDataOutputStream out = fileSystem.create(path, true);
		try {
			out.writeUTF(content);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	public void write(String pathString, InputStream in) throws IOException {
		checkFileSystem();
		if (in == null)
			throw new IOException("No input stream");
		logger.info("Writing stream: " + pathString);
		Path path = new Path(pathString);
		FSDataOutputStream out = fileSystem.create(path, true);
		try {
			IOUtils.copy(in, out);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	public String readUTF(String pathString) throws IOException {
		checkFileSystem();
		logger.info("readUTF: " + pathString);
		Path path = new Path(pathString);
		FSDataInputStream in = fileSystem.open(path);
		try {
			return in.readUTF();
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public File readAsFile(String pathString, File localFile)
			throws IOException {
		checkFileSystem();
		logger.info("readAsFile: " + pathString);
		Path path = new Path(pathString);
		FSDataInputStream in = fileSystem.open(path);
		try {
			IOUtils.copy(in, localFile, true);
			return localFile;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public File readAsTempFile(String pathString, String fileSuffix)
			throws IOException {
		File localFile = File.createTempFile("qwazr-hdfs-connector",
				fileSuffix == null ? StringUtils.EMPTY : fileSuffix);
		return readAsFile(pathString, localFile);
	}

	public boolean exists(String pathString) throws IOException {
		checkFileSystem();
		logger.info("Check path exist: " + pathString);
		return fileSystem.exists(new Path(pathString));
	}

	public boolean mkdir(String pathString) throws IOException {
		checkFileSystem();
		logger.info("Create dir: " + pathString);
		return fileSystem.mkdirs(new Path(pathString));
	}

}
