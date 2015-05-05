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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class CharsetUtils {

	public final static String charsetDetector(InputStream inputStream)
			throws IOException {
		CharsetDetector detector = new CharsetDetector();
		detector.setText(inputStream);
		CharsetMatch match = detector.detect();
		if (match == null)
			return null;
		return match.getName();
	}

	public final static String charsetDetector(byte[] bytes) {
		CharsetDetector detector = new CharsetDetector();
		detector.setText(bytes);
		CharsetMatch match = detector.detect();
		if (match == null)
			return null;
		return match.getName();
	}

	public final static Charset CharsetUTF8 = Charset.forName("UTF-8");

	public final static CharsetEncoder newUTF8Encoder() {
		synchronized (CharsetUTF8) {
			return CharsetUTF8.newEncoder();
		}
	}

	final public byte[] encode(String test) {
		return org.apache.commons.codec.binary.StringUtils.getBytesUtf8(test);
	}

}
