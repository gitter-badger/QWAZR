/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

	public static final String replaceConsecutiveSpaces(String source,
			String replace) {
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
		final CharSequence[] esc = { "\\", ".", "(", ")", "[", "]", "+", "?",
				"*" };
		final CharSequence[] replace = { "/", "\\.", "\\(", "\\)", "\\[",
				"\\]", "\\+", "\\?", ".*" };
		s = s.trim();
		int i = 0;
		for (CharSequence ch : esc)
			s = s.replace(ch, replace[i++]);
		return Pattern.compile(s);
	}

	/**
	 * 
	 * @param text
	 *            the text to encode
	 * @return a base64 encoded string
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported
	 */
	public final static String base64encode(String text)
			throws UnsupportedEncodingException {
		if (isEmpty(text))
			return null;
		return Base64.encodeBase64URLSafeString(text.getBytes("UTF-8"));
	}

	/**
	 * 
	 * @param base64String
	 *            the base64 string to decode
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
		return org.apache.commons.lang3.StringUtils.leftPad(
				Integer.toString(value), size, '0');
	}

	public final static String leftPad(long value, int size) {
		return org.apache.commons.lang3.StringUtils.leftPad(
				Long.toString(value), size, '0');
	}

	public final static String[] toStringArray(
			Collection<? extends Object> collection, boolean sort) {
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

	public final static CharSequence fastConcatCharSequence(
			final CharSequence... charSeqs) {
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

	public static void appendCollection(StringBuilder sb,
			Collection<?> collection) {
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

	public final static CharSequence fastConcatCharSequence(
			final Object... objects) {
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

	public final static String LINE_SEPARATOR = System
			.getProperty("line.separator");

	public final static String[] splitLines(String str) {
		return split(str, LINE_SEPARATOR);
	}

}
