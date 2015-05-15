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
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AffinityResults {

	public String name;

	public final List<AffinityResult> results;

	public AffinityResults() {
		this.name = null;
		this.results = new ArrayList<AffinityResult>(0);
	}

	/**
	 * @param name
	 *            the name to set
	 * @return the current instance
	 */
	public AffinityResults setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param result
	 *            the result to add
	 * @return the current instance
	 */
	@XmlTransient
	@JsonIgnore
	public AffinityResults addResult(AffinityResult result) {
		results.add(result);
		return this;
	}

	@XmlTransient
	@JsonIgnore
	public boolean isEmpty() {
		return results == null || results.isEmpty();
	}
}
