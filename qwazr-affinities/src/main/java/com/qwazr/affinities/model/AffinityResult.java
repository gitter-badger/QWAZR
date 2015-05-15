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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.opensearchserver.client.v1.search.DocumentResult1;
import com.opensearchserver.client.v1.search.FieldValueList1;
import com.opensearchserver.client.v1.search.SearchResult1;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AffinityResult {

	public Float score;

	public Map<String, List<String>> fields;

	public List<Map<String, List<String>>> grouped;

	public AffinityResult() {
		score = null;
		fields = null;
	}

	private AffinityResult(DocumentResult1 document) {
		this.score = document.score;
		if (document.fields == null) {
			fields = null;
			return;
		}
		fields = getFields(document);
		if (document.collapseCount != null && document.collapseCount > 0) {
			grouped = new ArrayList<Map<String, List<String>>>(
					document.collapseCount);
			for (DocumentResult1 doc : document.collapsedDocs)
				grouped.add(getFields(doc));
		}
	}

	private Map<String, List<String>> getFields(DocumentResult1 document) {
		Map<String, List<String>> fields = new LinkedHashMap<String, List<String>>();
		for (FieldValueList1 fieldValue : document.fields)
			if (fieldValue.fieldName != null && fieldValue.values != null)
				fields.put(fieldValue.fieldName, fieldValue.values);
		return fields;
	}

	/**
	 * @param score
	 *            the score to set
	 * @return the current instance
	 */
	public AffinityResult setScore(Float score) {
		this.score = score;
		return this;
	}

	/**
	 * @param fields
	 *            the fields to set
	 * @return the current instance
	 */
	public AffinityResult setFields(Map<String, List<String>> fields) {
		this.fields = fields;
		return this;
	}

	@XmlTransient
	@JsonIgnore
	public static AffinityResults newList(String name,
			SearchResult1 searchResult) {
		AffinityResults results = new AffinityResults().setName(name);
		if (searchResult.documents == null)
			return results;
		for (DocumentResult1 document : searchResult.documents)
			results.addResult(new AffinityResult(document));
		return results;
	}

}
