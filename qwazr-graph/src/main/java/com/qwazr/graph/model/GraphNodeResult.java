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

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qwazr.graph.GraphInstance.NodeScore;

@JsonInclude(Include.NON_EMPTY)
public class GraphNodeResult extends GraphNode {

	public double score;
	public String node_id;

	public GraphNodeResult() {
		score = 0;
		node_id = null;
	}

	@XmlTransient
	@JsonIgnore
	public GraphNodeResult set(NodeScore nodeScore) {
		score = nodeScore.score;
		node_id = nodeScore.node_id;
		return this;
	}

}
