/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    private final static Pattern removeTagPattern = Pattern.compile("<[^>]*>");
    private final static Pattern removeBrPattern1 = Pattern.compile("\\.\\p{Space}+<br\\p{Space}*/?>",
	    Pattern.CASE_INSENSITIVE);
    private final static Pattern removeEndTagBlockPattern1 = Pattern
	    .compile(
		    "\\.\\p{Space}+</(p|td|div|h1|h2|h3|h4|h5|h6|hr|li|option|pre|select|table|tbody|td|textarea|tfoot|thead|th|title|tr|ul)>",
		    Pattern.CASE_INSENSITIVE);
    private final static Pattern removeEndTagBlockPattern2 = Pattern
	    .compile(
		    "</(p|td|div|h1|h2|h3|h4|h5|h6|hr|li|option|pre|select|table|tbody|td|textarea|tfoot|thead|th|title|tr|ul)>",
		    Pattern.CASE_INSENSITIVE);
    private final static Pattern removeBrPattern2 = Pattern.compile("<br\\p{Space}*/?>", Pattern.CASE_INSENSITIVE);
    private final static Pattern removeScriptObjectStylePattern = Pattern.compile(
	    "<(script|object|style)[^>]*>[^<]*</(script|object|style)>", Pattern.CASE_INSENSITIVE);

    public static final String removeTag(String text) {
	if (StringUtils.isEmpty(text))
	    return text;
	text = StringUtils.replaceConsecutiveSpaces(text, " ");
	synchronized (removeScriptObjectStylePattern) {
	    text = removeScriptObjectStylePattern.matcher(text).replaceAll("");
	}
	synchronized (removeBrPattern1) {
	    text = removeBrPattern1.matcher(text).replaceAll("</p>");
	}
	synchronized (removeEndTagBlockPattern1) {
	    text = removeEndTagBlockPattern1.matcher(text).replaceAll("</p>");
	}
	synchronized (removeEndTagBlockPattern2) {
	    text = removeEndTagBlockPattern2.matcher(text).replaceAll(". ");
	}
	synchronized (removeBrPattern2) {
	    text = removeBrPattern2.matcher(text).replaceAll(". ");
	}
	synchronized (removeTagPattern) {
	    text = removeTagPattern.matcher(text).replaceAll("");
	}
	text = StringUtils.replaceConsecutiveSpaces(text, " ");
	return text;
    }

    public static final String removeTag(String text, String[] allowedTags) {
	if (allowedTags == null)
	    text = StringUtils.replaceConsecutiveSpaces(text, " ");
	StringBuffer sb = new StringBuffer();
	Matcher matcher;
	synchronized (removeTagPattern) {
	    matcher = removeTagPattern.matcher(text);
	}
	while (matcher.find()) {
	    boolean allowed = false;
	    String group = matcher.group();
	    if (allowedTags != null) {
		for (String tag : allowedTags) {
		    if (tag.equals(group)) {
			allowed = true;
			break;
		    }
		}
	    }
	    matcher.appendReplacement(sb, allowed ? group : "");
	}
	matcher.appendTail(sb);
	return sb.toString();
    }

    public final static String htmlWrap(String text, int wrapLength) {
	if (StringUtils.isEmpty(text))
	    return text;
	if (text.length() < wrapLength)
	    return text;
	text = StringUtils.replace(text, "&shy;", "");
	return WordUtils.wrap(text, wrapLength, "&shy;", true);
    }

    public final static String htmlWrapReduce(String text, int wrapLength, int maxSize) {
	if (StringUtils.isEmpty(text))
	    return text;
	if (text.length() < maxSize)
	    return text;
	text = StringUtils.replace(text, "&shy;", "");
	text = WordUtils.wrap(text, wrapLength, "\u00AD", true);
	String[] frags = StringUtils.split(text, '\u00AD');
	StringBuilder sb = new StringBuilder();
	int l = frags[0].length();
	for (int i = frags.length - 1; i > 0; i--) {
	    String frag = frags[i];
	    l += frag.length();
	    if (l >= maxSize)
		break;
	    sb.insert(0, frag);
	}
	sb.insert(0, '…');
	sb.insert(0, frags[0]);
	return sb.toString();
    }

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

    public static void main(String args[]) throws IOException {
	if (args != null && args.length == 2) {
	    List<String> lines = FileUtils.readLines(new File(args[0]));
	    FileWriter fw = new FileWriter(new File(args[1]));
	    PrintWriter pw = new PrintWriter(fw);
	    for (String line : lines)
		pw.println(StringEscapeUtils.unescapeHtml4(line));
	    pw.close();
	    fw.close();
	}
	String text = "file://&shy;Users/ekeller/Moteur/infotoday_enterprisesearchsourcebook08/Open_on_Windows.exe";
	System.out.println(htmlWrap(text, 20));
	System.out.println(htmlWrapReduce(text, 20, 80));
	String url = "file://Users/ekeller/Moteur/infotoday_enterprisesearchsourcebook08/Open_on_Windows.exe?test=2";
	System.out.println(urlHostPathWrapReduce(url, 80));
    }

}
