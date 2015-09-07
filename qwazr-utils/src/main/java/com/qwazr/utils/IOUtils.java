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
 */
package com.qwazr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IOUtils extends org.apache.commons.io.IOUtils {

	private final static Logger logger = LoggerFactory
			.getLogger(IOUtils.class);

	public static final void close(final Closeable... closeables) {
		if (closeables == null)
			return;
		for (Closeable closeable : closeables)
			closeQuietly(closeable);
	}

	public static final void close(List<AutoCloseable> autoCloseables) {
		if (autoCloseables == null)
			return;
		for (AutoCloseable autoCloseable : autoCloseables)
			closeQuietly(autoCloseable);
	}

	public static final void close(ImageInputStream... closeables) {
		if (closeables == null)
			return;
		for (ImageInputStream closeable : closeables)
			closeQuietly(closeable);
	}

	public static final void closeQuietly(ImageInputStream closeable) {
		if (closeable == null)
			return;
		try {
			closeable.close();
		} catch (IOException e) {
			if (logger.isWarnEnabled())
				logger.warn("Close failure on " + closeable, e);
		}
	}

	public static final void closeQuietly(AutoCloseable autoCloseable) {
		if (autoCloseable == null)
			return;
		try {
			autoCloseable.close();
		} catch (Exception e) {
			if (logger.isWarnEnabled())
				logger.warn("Close failure on " + autoCloseable, e);
		}
	}

	public static final void close(
			final Collection<? extends Closeable> closeables) {
		if (closeables == null)
			return;
		Closeable[] array = closeables.toArray(new Closeable[closeables.size()]);
		int i = array.length;
		while (i > 0) {
			Closeable closeable = array[--i];
			try {
				closeQuietly(closeable);
			} catch (Exception e) {
				if (logger.isWarnEnabled())
					logger.warn("Close failure on " + closeable, e);
			}
		}
	}

	public static final int copy(InputStream inputStream, File tempFile,
								 boolean bCloseInputStream) throws IOException {
		FileOutputStream fos = new FileOutputStream(tempFile);
		try {
			return copy(inputStream, fos);
		} finally {
			close(fos);
			if (bCloseInputStream)
				close(inputStream);
		}
	}

	public static final StringBuilder copy(InputStream inputStream,
										   StringBuilder sb, String charsetName, boolean bCloseInputStream)
			throws IOException {
		if (inputStream == null)
			return sb;
		if (sb == null)
			sb = new StringBuilder();
		Charset charset = Charset.forName(charsetName);
		byte[] buffer = new byte[16384];
		int length;
		while ((length = inputStream.read(buffer)) != -1)
			sb.append(new String(buffer, 0, length, charset));
		if (bCloseInputStream)
			inputStream.close();
		return sb;
	}

	public static final void appendLines(File file, String... lines)
			throws IOException {
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(file, true);
			pw = new PrintWriter(fw);
			for (String line : lines)
				pw.println(line);
		} finally {
			close(fw, pw);
		}
	}

	public static final File storeAsTempFile(InputStream inputStream)
			throws IOException {
		File tmpFile = File.createTempFile("qwazr-store", ".upload");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(tmpFile);
			IOUtils.copy(inputStream, fos);
			return tmpFile;
		} finally {
			if (fos != null)
				IOUtils.closeQuietly(fos);
		}
	}

	public interface CloseableContext {

		void add(Closeable closeable);

		void add(AutoCloseable autoCloseable);
	}

	public static class CloseableList implements CloseableContext, Closeable {

		private final List<Closeable> closeables;
		private final List<AutoCloseable> autoCloseables;

		public CloseableList() {
			closeables = new ArrayList<Closeable>();
			autoCloseables = new ArrayList<AutoCloseable>();
		}

		@Override
		public void add(Closeable closeable) {
			closeables.add(closeable);
		}

		@Override
		public void add(AutoCloseable autoCloseable) {
			autoCloseables.add(autoCloseable);
		}

		@Override
		public void close() {
			IOUtils.close(closeables);
			closeables.clear();
			IOUtils.close(autoCloseables);
			autoCloseables.clear();
		}

	}

	/**
	 * Extract the content of a file to a string
	 *
	 * @param file the file
	 * @return the content of the file as a string
	 * @throws IOException
	 */
	public static String readFileAsString(File file) throws IOException {
		FileReader reader = new FileReader(file);
		try {
			return toString(reader);
		} finally {
			closeQuietly(reader);
		}
	}

}
