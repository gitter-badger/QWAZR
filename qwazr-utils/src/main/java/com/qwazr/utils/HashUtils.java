/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class HashUtils {

	public static final int getMurmur3Mod(final String hashString, Charset charset, final int mod) {
		HashFunction m3 = Hashing.murmur3_128();
		if (charset == null)
			charset = Charset.defaultCharset();
		return (Math.abs(m3.hashString(hashString, charset).asInt()) % mod);
	}

	public static String md5Hex(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			BufferedInputStream bis = new BufferedInputStream(fis);
			try {
				return DigestUtils.md5Hex(bis);
			} finally {
				IOUtils.closeQuietly(bis);
			}
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}
}
