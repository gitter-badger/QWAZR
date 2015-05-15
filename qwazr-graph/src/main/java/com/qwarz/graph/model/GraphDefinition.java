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
package com.qwarz.graph.model;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class GraphDefinition {

	final public Map<String, PropertyTypeEnum> node_properties;

	final public Set<String> edge_types;

	public GraphDefinition() {
		this(null, null);
	}

	public GraphDefinition(Map<String, PropertyTypeEnum> node_properties,
			Set<String> edge_types) {
		this.node_properties = node_properties;
		this.edge_types = edge_types;
	}

	protected GraphDefinition(GraphDefinition graphDef) {
		this.node_properties = graphDef.node_properties;
		this.edge_types = graphDef.edge_types;
	}

	public static enum PropertyTypeEnum {
		indexed, stored, boost;
	}

	@XmlTransient
	@JsonIgnore
	public boolean isEdgeType(String edge_type) {
		return edge_types == null ? false : edge_types.contains(edge_type);
	}

	@XmlTransient
	@JsonIgnore
	public boolean isIndexedProperty(String property) {
		if (node_properties == null)
			return false;
		PropertyTypeEnum type = node_properties.get(property);
		if (type == null)
			return false;
		return type == PropertyTypeEnum.indexed;
	}

}