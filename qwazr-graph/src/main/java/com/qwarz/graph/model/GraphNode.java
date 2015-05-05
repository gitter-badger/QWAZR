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
package com.qwarz.graph.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class GraphNode {

	public Map<String, String> properties;
	public Map<String, Set<String>> edges;

	public GraphNode() {
		properties = null;
		edges = null;
	}

	@JsonIgnore
	@XmlTransient
	public boolean addProperty(String name, String value) {
		if (name == null || name.isEmpty() || value == null || value.isEmpty())
			return false;
		if (properties == null)
			properties = new LinkedHashMap<String, String>();
		else if (properties.containsKey(name))
			return false;
		properties.put(name, value);
		return true;
	}

	private Set<String> getEdgeSet(String type) {
		if (type == null || type.isEmpty())
			return null;
		if (edges == null)
			edges = new LinkedHashMap<String, Set<String>>();
		Set<String> nodeIdSet = edges.get(type);
		if (nodeIdSet != null)
			return nodeIdSet;
		nodeIdSet = new TreeSet<String>();
		edges.put(type, nodeIdSet);
		return nodeIdSet;
	}

	@JsonIgnore
	@XmlTransient
	public boolean addEdge(String type, String value) {
		if (value == null || value.isEmpty())
			return false;
		Set<String> nodeIdSet = getEdgeSet(type);
		if (nodeIdSet == null)
			return false;
		nodeIdSet.add(value);
		return true;
	}

	@JsonIgnore
	@XmlTransient
	public boolean addEdges(String type, Collection<String> values) {
		if (values == null || values.isEmpty())
			return false;
		Set<String> nodeIdSet = getEdgeSet(type);
		if (nodeIdSet == null)
			return false;
		nodeIdSet.addAll(values);
		return true;
	}

	@JsonIgnore
	@XmlTransient
	public boolean removeEdge(String type, String value) {
		if (value == null || value.isEmpty())
			return false;
		if (edges == null)
			return false;
		if (type == null || type.isEmpty())
			return false;
		Set<String> nodeIdSet = edges.get(type);
		if (nodeIdSet == null)
			return false;
		return nodeIdSet.remove(value);
	}

	@JsonIgnore
	@XmlTransient
	public void add(GraphNode node) {
		if (node == null)
			return;
		if (node.properties != null)
			for (Map.Entry<String, String> entry : node.properties.entrySet())
				addProperty(entry.getKey(), entry.getValue());
		if (node.edges != null)
			for (Map.Entry<String, Set<String>> entry : node.edges.entrySet())
				addEdges(entry.getKey(), entry.getValue());
	}

}
