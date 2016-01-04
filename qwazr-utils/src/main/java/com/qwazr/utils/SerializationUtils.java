/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class SerializationUtils extends
		org.apache.commons.lang3.SerializationUtils {

	/**
	 * Read an object from a file using a buffered stream and GZIP compression
	 *
	 * @param file the destination file
	 * @param <T>  the type of the object
	 * @return the deserialized object
	 * @throws IOException
	 */
	public static <T> T deserialize(File file) throws IOException {
		FileInputStream is = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(is);
		GZIPInputStream zis = new GZIPInputStream(bis);
		try {
			return SerializationUtils.deserialize(zis);
		} finally {
			IOUtils.close(zis, bis, is);
		}
	}

	/**
	 * Write an object to a file using a buffered stream and GZIP compression.
	 *
	 * @param obj  the object to write
	 * @param file the destination file
	 * @throws IOException
	 */
	public static void serialize(Serializable obj, File file)
			throws IOException {
		FileOutputStream os = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(os);
		GZIPOutputStream zos = new GZIPOutputStream(bos);
		try {
			SerializationUtils.serialize(obj, zos);
		} finally {
			IOUtils.close(zos, bos, os);
		}
	}

}
