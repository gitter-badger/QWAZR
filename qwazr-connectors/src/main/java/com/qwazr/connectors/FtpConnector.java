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
package com.qwazr.connectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FtpConnector extends AbstractPasswordConnector {

	public final String hostname = null;
	public final String username = null;
	public final Boolean ssl = null;
	public final Integer keep_alive_timeout = null;

	private static final Logger logger = LoggerFactory.getLogger(FtpConnector.class);

	@Override
	public void load(File parentDir) {
	}

	@Override
	public void unload() {
	}

	@JsonIgnore
	public FTPSession geNewSession(IOUtils.CloseableContext context) {
		FTPSession ftpSession = new FTPSession();
		context.add(ftpSession);
		return ftpSession;
	}

	public class FTPSession implements Closeable {

		private final FTPClient ftp;

		private FTPSession() {
			ftp = ssl != null && ssl ? new FTPSClient() : new FTPClient();
		}

		public FTPClient connect() throws IOException {
			if (ftp.isConnected())
				return ftp;
			ftp.connect(hostname);
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply))
				throw new IOException("FTP server returned an error: " + reply);
			if (!ftp.login(username, password))
				throw new IOException("FTP login failed: " + ftp.getReplyCode());
			if (keep_alive_timeout != null)
				ftp.setControlKeepAliveTimeout(keep_alive_timeout);
			return ftp;
		}

		/**
		 * Download the file if any
		 *
		 * @param remote the name of the file
		 * @param file   the destination file
		 * @throws IOException
		 */
		public void retrieve(String remote, File file) throws IOException {
			InputStream is = ftp.retrieveFileStream(remote);
			if (is == null)
				throw new FileNotFoundException("FTP file not found: " + hostname + "/" + remote);
			try {
				IOUtils.copy(is, file);
			} finally {
				IOUtils.closeQuietly(is);
			}
			ftp.completePendingCommand();
		}

		public void retrieve(FTPFile remote, File file) throws IOException {
			retrieve(remote.getName(), file);
		}

		public void retrieve(FTPFile remote, String local_path) throws IOException {
			retrieve(remote.getName(), new File(local_path));
		}

		public void retrieve(String remote, String local_path) throws IOException {
			retrieve(remote, new File(local_path));
		}

		public void sync_files(String remote_path, String local_path, Boolean downloadOnlyIfNotExists)
						throws IOException {
			if (!ftp.changeWorkingDirectory(remote_path))
				throw new IOException("Remote working directory change failed: " + hostname + "/" + remote_path);
			File localDirectory = new File(local_path);
			if (!localDirectory.exists())
				throw new FileNotFoundException("The destination directory does not exist: " + local_path);
			if (!localDirectory.isDirectory())
				throw new IOException("The destination path is not a directory: " + local_path);
			FTPFile[] remoteFiles = ftp.listFiles();
			if (remoteFiles == null)
				return;
			for (FTPFile remoteFile : remoteFiles) {
				if (remoteFile == null)
					continue;
				if (!remoteFile.isFile())
					continue;
				File localFile = new File(localDirectory, remoteFile.getName());
				if (downloadOnlyIfNotExists != null && downloadOnlyIfNotExists && localFile.exists())
					continue;
				if (logger.isInfoEnabled())
					logger.info("FTP download: " + hostname + "/" + remoteFile.getName());
				retrieve(remoteFile, localFile);
			}
		}

		public void logout() throws IOException {
			ftp.logout();
		}

		@Override
		public void close() throws IOException {
			if (!ftp.isConnected())
				return;
			try {
				ftp.disconnect();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

}
