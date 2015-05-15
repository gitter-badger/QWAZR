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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class LinkUtils {

	public final static String urlHostPathWrapReduce(String url, int maxSize) {
		URL u;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			return url;
		}
		String path = StringUtils.fastConcat(u.getHost(), '/', u.getPath());
		String[] frags = StringUtils.split(path, '/');
		if (frags.length < 2)
			return path;
		int startPos = 1;
		int endPos = frags.length - 2;
		StringBuilder sbStart = new StringBuilder(frags[0]);
		StringBuilder sbEnd = new StringBuilder(frags[frags.length - 1]);
		int length = sbStart.length() + sbEnd.length();
		for (;;) {
			boolean bHandled = false;
			if (startPos != -1 && startPos < endPos) {
				if (frags[startPos].length() + length < maxSize) {
					sbStart.append('/');
					sbStart.append(frags[startPos++]);
					bHandled = true;
				}
			}
			if (endPos != -1 && endPos > startPos) {
				if (frags[endPos].length() + length < maxSize) {
					sbEnd.insert(0, '/');
					sbEnd.insert(0, frags[endPos--]);
					bHandled = true;
				}
			}
			if (!bHandled)
				break;
		}
		return StringUtils.fastConcat(sbStart, "/…/", sbEnd);
	}

	public final static String concatPath(String path1, String path2) {
		if (path2 == null)
			return path1;
		if (path1 == null)
			return path2;
		StringBuilder sb = new StringBuilder(path1);
		if (!path1.endsWith("/") && !path2.startsWith("/"))
			sb.append('/');
		sb.append(path2);
		return sb.toString();
	}

	public final static String lastPart(String path) {
		if (path == null)
			return null;
		String[] parts = StringUtils.split(path, '/');
		if (parts == null)
			return path;
		if (parts.length == 0)
			return path;
		return parts[parts.length - 1];
	}

	public final static String UTF8_URL_Encode(String s)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
	}

	public final static String UTF8_URL_QuietDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}

	public final static URI newEncodedURI(String u)
			throws MalformedURLException, URISyntaxException {
		URL tmpUrl = new URL(u);
		return new URI(tmpUrl.getProtocol(), tmpUrl.getUserInfo(),
				tmpUrl.getHost(), tmpUrl.getPort(), tmpUrl.getPath(),
				tmpUrl.getQuery(), tmpUrl.getRef());
	}

	public final static URL newEncodedURL(String u)
			throws MalformedURLException, URISyntaxException {
		return newEncodedURI(u).toURL();
	}

	public static void main(String[] args) throws MalformedURLException,
			UnsupportedEncodingException {
		System.out.println(lastPart("/my+folder/"));
		System.out.println(lastPart("my folder/"));
		System.out.println(lastPart("my folder/my+sub-folder/"));
		System.out.println(lastPart("/my+file.png"));
		System.out.println(lastPart("my+file.png"));
		System.out.println(lastPart("my+folder/my+sub-folder/my+file.png"));
		System.out.println(UTF8_URL_Encode("outlook:INBOX/~TEST TEST"));
	}

	public final static Map<String, String> getUniqueQueryParameters(
			final URI uri, final String charset) {
		final Map<String, String> map = new TreeMap<String, String>();
		final List<NameValuePair> parameters = URLEncodedUtils.parse(uri,
				"UTF-8");
		for (NameValuePair parameter : parameters)
			map.put(parameter.getName(), parameter.getValue());
		return map;
	}

}
