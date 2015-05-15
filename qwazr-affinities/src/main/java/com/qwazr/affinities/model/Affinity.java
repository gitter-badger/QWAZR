/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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
package com.qwazr.affinities.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.opensearchserver.client.ServerResource;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Affinity {

	public static enum Type {

		/**
		 * This method checks if all the fields exactly matched.
		 */
		EXACT_MATCH,

		/**
		 * This method finds the matching documents by computing a score.
		 */
		SCORING
	}

	/**
	 * Specifies the OpenSearchServer instance and the name of resource used for
	 * the crawl.
	 */
	public ServerResource crawl;

	/**
	 * Specifies the OpenSearchServer instance and the name of the index
	 * containing the data.
	 */
	public ServerResource data;

	/**
	 * Specifies the OpenSearchServer instance and the name of the index
	 * containing the cache.
	 */
	public ServerResource cache;

	/**
	 * A map which specifies the matched fields. The key is the name of the
	 * field. The value is the weight of the field.
	 */
	public Map<String, Double> criteria;

	/**
	 * A map between crawl fields (title, content) and the criteria
	 */
	public Map<String, String> crawl_mapping;

	/**
	 * Specifies the fields returned with the recommendation.
	 */
	public List<String> returned_fields;

	/**
	 * Group the results if the fields contains the same value.
	 */
	public String group_by;

	/**
	 * Sets the matching method.
	 */
	public Type type;

	public Affinity() {
		crawl = null;
		crawl_mapping = null;
		criteria = null;
		data = null;
		returned_fields = null;
		group_by = null;
		type = null;
	}

	/**
	 * @return the specified type or the default type (SCORING).
	 */
	@XmlTransient
	@JsonIgnore
	public Type getTypeOfDefault() {
		return type == null ? Type.SCORING : type;
	}

}
