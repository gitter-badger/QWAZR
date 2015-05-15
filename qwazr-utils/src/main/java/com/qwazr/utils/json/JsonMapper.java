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
package com.qwazr.utils.json;

import java.util.ArrayList;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonMapper {

	public final static ObjectMapper MAPPER;

	public final static TypeReference<TreeMap<String, String>> MapStringStringTypeRef = new TypeReference<TreeMap<String, String>>() {
	};

	public final static TypeReference<TreeMap<String, ArrayList<String>>> MapStringListStringTypeRef = new TypeReference<TreeMap<String, ArrayList<String>>>() {
	};

	static {
		MAPPER = new ObjectMapper();
		MAPPER.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

}
