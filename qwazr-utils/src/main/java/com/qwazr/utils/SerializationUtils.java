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
package com.qwazr.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;

public class SerializationUtils extends
		org.apache.commons.lang3.SerializationUtils {

	public static <T> T deserialize(File file) throws FileNotFoundException {
		FileInputStream inputStream = new FileInputStream(file);
		try {
			return SerializationUtils.deserialize(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	public static void serialize(Serializable obj, File file)
			throws FileNotFoundException {
		FileOutputStream outputStream = new FileOutputStream(file);
		try {
			SerializationUtils.serialize(obj, outputStream);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

}
