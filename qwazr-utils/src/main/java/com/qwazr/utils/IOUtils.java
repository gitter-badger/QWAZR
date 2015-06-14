/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IOUtils extends org.apache.commons.io.IOUtils {

	public static final void close(final Closeable... closeables) {
		if (closeables == null)
			return;
		for (Closeable closeable : closeables)
			closeQuietly(closeable);
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
			// We said Quietly
		}
	}

	public static final void close(
			final Collection<? extends Closeable> closeables) {
		if (closeables == null)
			return;
		for (Closeable closeable : closeables)
			closeQuietly(closeable);
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
	}

	public static class CloseableList implements CloseableContext, Closeable {

		private final List<Closeable> closeables;

		public CloseableList() {
			closeables = new ArrayList<Closeable>();
		}

		@Override
		public void add(Closeable closeable) {
			closeables.add(closeable);
		}

		@Override
		public void close() {
			IOUtils.close(closeables);
			closeables.clear();
		}
	}

}
