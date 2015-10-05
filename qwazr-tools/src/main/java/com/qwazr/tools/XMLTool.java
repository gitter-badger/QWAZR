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
 **/
package com.qwazr.tools;

import com.jamesmurty.utils.XMLBuilder2;
import com.qwazr.utils.IOUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;

public class XMLTool extends AbstractTool {

	private SAXParserFactory saxParserFactory;

	@Override
	public void load(File parentDir) {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);
	}

	@Override
	public void unload() {
	}

	/**
	 * @param root the name of the root element
	 * @return an new XML builder instance
	 * <p>
	 * {@link XMLBuilder2}
	 */
	public XMLBuilder2 create(String root) {
		return XMLBuilder2.create(root);
	}

	/**
	 * Save the XML to the file described by the given path
	 *
	 * @param builder
	 * @param path
	 * @throws IOException
	 */
	public void saveTo(XMLBuilder2 builder, String path) throws IOException {
		FileWriter writer = new FileWriter(path);
		try {
			builder.toWriter(true, writer, null);
		} finally {
			writer.close();
		}
	}

	/**
	 * Parse and XML stream
	 *
	 * @param jsObject
	 * @param is
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void parseStream(ScriptObjectMirror jsObject, InputStream is)
					throws ParserConfigurationException, SAXException, IOException {
		DefaultHandler defaultHandler = (DefaultHandler) ScriptUtils.convert(jsObject, DefaultHandler.class);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		saxParser.parse(is, defaultHandler);
	}

	/**
	 * Parse an XML file
	 *
	 * @param jsObject
	 * @param path
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void parseFile(ScriptObjectMirror jsObject, String path)
					throws IOException, SAXException, ParserConfigurationException {
		InputStream in = new BufferedInputStream(new FileInputStream(path));
		try {
			parseStream(jsObject, in);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	final public static class JSDefaultHandler implements EntityResolver, DTDHandler, ContentHandler, ErrorHandler {

		private final ScriptObjectMirror jsObject;

		private JSDefaultHandler(ScriptObjectMirror jsObject) {
			this.jsObject = jsObject;

		}

		@Override
		public void setDocumentLocator(Locator locator) {
			if (jsObject.hasMember("setDocumentLocator"))
				jsObject.callMember("setDocumentLocator", locator);
		}

		@Override
		public void startDocument() throws SAXException {
			if (jsObject.hasMember("startDocument"))
				jsObject.callMember("startDocument");
		}

		@Override
		public void endDocument() throws SAXException {
			if (jsObject.hasMember("endDocument"))
				jsObject.callMember("endDocument");
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			if (jsObject.hasMember("startPrefixMapping"))
				jsObject.callMember("startPrefixMapping", prefix, uri);
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			if (jsObject.hasMember("endPrefixMapping"))
				jsObject.callMember("endPrefixMapping", prefix);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {

		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {

		}

		@Override
		public void skippedEntity(String name) throws SAXException {

		}

		@Override
		public void notationDecl(String name, String publicId, String systemId) throws SAXException {

		}

		@Override
		public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
						throws SAXException {

		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			return null;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {

		}

		@Override
		public void error(SAXParseException exception) throws SAXException {

		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {

		}
	}

}