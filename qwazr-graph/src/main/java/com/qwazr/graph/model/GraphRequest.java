/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.graph.model;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_EMPTY)
public class GraphRequest {

	public Integer start;
	public Integer rows;

	public JsonNode filters;

	public Set<String> exclude_nodes;

	public Map<String, Double> edge_type_weight;
	public Map<String, Set<String>> edges;

	public Set<String> node_property_boost;

	public GraphRequest() {
		start = null;
		rows = null;
		exclude_nodes = null;
		edge_type_weight = null;
		edges = null;
	}

	@JsonInclude
	@XmlTransient
	public int getStartOrDefault() {
		return start == null ? 0 : start;
	}

	@JsonInclude
	@XmlTransient
	public int getRowsOrDefault() {
		return rows == null ? 10 : rows;
	}

	@JsonInclude
	@XmlTransient
	public Double getEdgeWeight(String edge_type) {
		if (edge_type_weight == null)
			return 1.0;
		Double weight = edge_type_weight.get(edge_type);
		return weight == null ? 1.0 : weight;
	}

}
