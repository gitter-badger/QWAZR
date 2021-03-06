/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.search.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.json.JsonMapper;

import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IndexSettingsDefinition {

	final public String similarity_class;

	public IndexSettingsDefinition() {
		similarity_class = null;
	}

	public IndexSettingsDefinition(String similarity_class) {
		this.similarity_class = similarity_class;
	}

	final static IndexSettingsDefinition EMPTY = new IndexSettingsDefinition();

	public final static IndexSettingsDefinition newSettings(String jsonString) throws IOException {
		if (StringUtils.isEmpty(jsonString))
			return null;
		return JsonMapper.MAPPER.readValue(jsonString, IndexSettingsDefinition.class);
	}

}
