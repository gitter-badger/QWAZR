/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.graph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import javax.xml.bind.annotation.XmlTransient;
import java.util.*;

@JsonInclude(Include.NON_EMPTY)
public class GraphNode {

    public Map<String, Object> properties;
    public Map<String, Set<Object>> edges;

    public GraphNode() {
	properties = null;
	edges = null;
    }

    @JsonIgnore
    @XmlTransient
    public boolean addProperty(String name, Object value) {
	if (name == null || name.isEmpty() || value == null)
	    return false;
	if (properties == null)
	    properties = new LinkedHashMap<String, Object>();
	else if (properties.containsKey(name))
	    return false;
	properties.put(name, value);
	return true;
    }

    private Set<Object> getEdgeSet(String type) {
	if (type == null || type.isEmpty())
	    return null;
	if (edges == null)
	    edges = new LinkedHashMap<String, Set<Object>>();
	Set<Object> nodeIdSet = edges.get(type);
	if (nodeIdSet != null)
	    return nodeIdSet;
	nodeIdSet = new TreeSet<Object>();
	edges.put(type, nodeIdSet);
	return nodeIdSet;
    }

    @JsonIgnore
    @XmlTransient
    public boolean addEdge(String type, Object value) {
	if (value == null)
	    return false;
	if (value instanceof Collection)
	    return addEdges(type, (Collection<Object>) value);
	if (value.getClass().isArray())
	    return addEdges(type, (Object[]) value);
	Set<Object> nodeIdSet = getEdgeSet(type);
	if (nodeIdSet == null)
	    return false;
	nodeIdSet.add(value);
	return true;
    }

    @JsonIgnore
    @XmlTransient
    public boolean addEdges(String type, Collection<Object> values) {
	if (values == null || values.isEmpty())
	    return false;
	Set<Object> nodeIdSet = getEdgeSet(type);
	if (nodeIdSet == null)
	    return false;
	nodeIdSet.addAll(values);
	return true;
    }

    @JsonIgnore
    @XmlTransient
    public boolean addEdges(String type, Object[] values) {
	if (values == null || values.length == 0)
	    return false;
	Set<Object> nodeIdSet = getEdgeSet(type);
	if (nodeIdSet == null)
	    return false;
	for (Object value : values)
	    nodeIdSet.add(value);
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
	Set<Object> nodeIdSet = edges.get(type);
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
	    for (Map.Entry<String, Object> entry : node.properties.entrySet())
		addProperty(entry.getKey(), entry.getValue());
	if (node.edges != null)
	    for (Map.Entry<String, Set<Object>> entry : node.edges.entrySet())
		addEdges(entry.getKey(), entry.getValue());
    }

}
