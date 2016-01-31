/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

	public static final String replaceConsecutiveSpaces(String source, String replace) {
		if (isEmpty(source))
			return source;
		StringBuilder target = new StringBuilder();
		int l = source.length();
		boolean consecutiveSpace = false;
		for (int i = 0; i < l; i++) {
			char c = source.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!consecutiveSpace) {
					if (replace != null)
						target.append(replace);
					consecutiveSpace = true;
				}
			} else {
				target.append(c);
				if (consecutiveSpace)
					consecutiveSpace = false;
			}
		}
		return target.toString();
	}

	public static Pattern wildcardPattern(String s) {
		final CharSequence[] esc = { "\\", ".", "(", ")", "[", "]", "+", "?", "*" };
		final CharSequence[] replace = { "/", "\\.", "\\(", "\\)", "\\[", "\\]", "\\+", "\\?", ".*" };
		s = s.trim();
		int i = 0;
		for (CharSequence ch : esc)
			s = s.replace(ch, replace[i++]);
		return Pattern.compile(s);
	}

	/**
	 * @param text the text to encode
	 * @return a base64 encoded string
	 * @throws UnsupportedEncodingException if the encoding is not supported
	 */
	public final static String base64encode(String text) throws UnsupportedEncodingException {
		if (isEmpty(text))
			return null;
		return Base64.encodeBase64URLSafeString(text.getBytes("UTF-8"));
	}

	/**
	 * @param base64String the base64 string to decode
	 * @return a decoded string
	 */
	public final static String base64decode(String base64String) {
		if (isEmpty(base64String))
			return null;
		return new String(Base64.decodeBase64(base64String));
	}

	public final static int compareNullValues(final Object v1, final Object v2) {
		if (v1 == null) {
			if (v2 == null)
				return 0;
			return -1;
		}
		if (v2 == null)
			return 1;
		return 0;
	}

	public final static int compareNullString(final String v1, final String v2) {
		if (v1 == null) {
			if (v2 == null)
				return 0;
			return -1;
		}
		if (v2 == null)
			return 1;
		return v1.compareTo(v2);
	}

	public static int compareNullHashCode(Object o1, Object o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return -1;
		}
		if (o2 == null)
			return 1;
		return o2.hashCode() - o1.hashCode();
	}

	public final static String leftPad(int value, int size) {
		return org.apache.commons.lang3.StringUtils.leftPad(Integer.toString(value), size, '0');
	}

	public final static String leftPad(long value, int size) {
		return org.apache.commons.lang3.StringUtils.leftPad(Long.toString(value), size, '0');
	}

	public final static String[] toStringArray(Collection<? extends Object> collection, boolean sort) {
		if (collection == null)
			return null;
		String[] array = new String[collection.size()];
		int i = 0;
		for (Object o : collection)
			array[i++] = o.toString();
		if (sort)
			Arrays.sort(array);
		return array;
	}

	public final static CharSequence fastConcatCharSequence(final CharSequence... charSeqs) {
		if (charSeqs == null)
			return null;
		if (charSeqs.length == 1)
			return charSeqs[0];
		StringBuilder sb = new StringBuilder();
		for (CharSequence charSeq : charSeqs)
			if (charSeq != null)
				sb.append(charSeq);
		return sb;
	}

	public final static String fastConcat(final CharSequence... charSeqs) {
		CharSequence cs = fastConcatCharSequence(charSeqs);
		return cs == null ? null : cs.toString();
	}

	public static void appendArray(StringBuilder sb, Object[] array) {
		for (Object object : array)
			appendObject(sb, object);
	}

	public static void appendCollection(StringBuilder sb, Collection<?> collection) {
		for (Object object : collection)
			appendObject(sb, object);
	}

	public static void appendObject(StringBuilder sb, Object object) {
		if (object instanceof Collection<?>)
			appendCollection(sb, (Collection<?>) object);
		else if (object instanceof Object[])
			appendArray(sb, (Object[]) object);
		else
			sb.append(object.toString());
	}

	public final static CharSequence fastConcatCharSequence(final Object... objects) {
		if (objects == null)
			return null;
		if (objects.length == 1)
			return objects.toString();
		StringBuilder sb = new StringBuilder();
		for (Object object : objects) {
			if (object != null)
				appendObject(sb, object);
		}
		return sb;
	}

	public final static String fastConcat(final Object... objects) {
		CharSequence cs = fastConcatCharSequence(objects);
		return cs == null ? null : cs.toString();
	}

	/**
	 * Retrieve the lines found in the passed text
	 *
	 * @param text              a text
	 * @param collectEmptyLines true if the empty lines should be collected
	 * @param lineCollector     the collection filled with the found lines
	 * @return the number of lines found
	 * @throws IOException
	 */
	public final static int linesCollector(String text, boolean collectEmptyLines, Collection<String> lineCollector)
			throws IOException {
		if (text == null)
			return 0;
		int i = 0;
		StringReader sr = new StringReader(text);
		try {
			BufferedReader br = new BufferedReader(sr);
			try {
				String line;
				while ((line = br.readLine()) != null) {
					if (!collectEmptyLines && line.length() == 0)
						continue;
					lineCollector.add(line);
					i++;
				}
			} finally {
				IOUtils.closeQuietly(br);
			}
		} finally {
			IOUtils.closeQuietly(sr);
		}
		return i;
	}

	/**
	 * Escape the chars
	 *
	 * @param source        the string to escape
	 * @param escaped_chars a list of char to escape
	 * @return the escaped string
	 */
	public static String escape_chars(String source, char[] escaped_chars) {
		if (escaped_chars == null || escaped_chars.length == 0)
			return source;
		if (source == null || source.length() == 0)
			return source;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			for (char ec : escaped_chars)
				if (c == ec)
					sb.append('\\');
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Split the string by line separator
	 *
	 * @param str the string to split
	 * @return an array with one item per line
	 */
	public static String[] splitLines(String str) {
		return split(str, System.lineSeparator());
	}

}
