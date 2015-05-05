/**
 * Copyright 2015 OpenSearchServer Inc.
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class AffinityRequest {

	public String url;

	public Map<String, String> criteria;

	public List<String> returned_fields;

	public String group_by;

	public Boolean cache;

	public String template_path;

	public AffinityRequest() {
		this.url = null;
		this.criteria = null;
		this.returned_fields = null;
		this.group_by = null;
		this.cache = null;
		this.template_path = null;
	}

	/**
	 * @param url
	 *            the url to set
	 * @return the current instance
	 */
	public AffinityRequest setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @param criteria
	 *            the criteria to set
	 * @return the current instance
	 */
	public AffinityRequest setCriteria(Map<String, String> criteria) {
		this.criteria = criteria;
		return this;
	}

	/**
	 * @param returned_fields
	 *            the returned_fields to set
	 * @return the current instance
	 */
	public AffinityRequest setReturned_fields(List<String> returned_fields) {
		this.returned_fields = returned_fields;
		return this;
	}

	/**
	 * @param group_by
	 *            the group_by to set
	 * @return the current instance
	 */
	public AffinityRequest setGroup_by(String group_by) {
		this.group_by = group_by;
		return this;
	}

	/**
	 * @param cache
	 *            the cache to set
	 * @return the current instance
	 */
	public AffinityRequest setCache(Boolean cache) {
		this.cache = cache;
		return this;
	}

	@XmlTransient
	@JsonIgnore
	public boolean getCacheOrDefault() {
		return cache == null ? false : cache;
	}

	/**
	 * @param template_path
	 *            the template_path to set
	 * @return the current instance
	 */
	public AffinityRequest setTemplate_path(String template_path) {
		this.template_path = template_path;
		return this;
	}

}
