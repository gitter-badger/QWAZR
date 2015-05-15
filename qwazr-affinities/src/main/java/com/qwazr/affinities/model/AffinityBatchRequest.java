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

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AffinityBatchRequest extends AffinityRequest {

	public static enum BatchAction {

		/**
		 * The batch is stopped if the request found documents. IF not document
		 * was found, the batch execute the next request.
		 */
		STOP_IF_FOUND,

		/**
		 * Go to the next request even if the request found documents.
		 */
		CONTINUE
	}

	public String name;

	public BatchAction action;

	public AffinityBatchRequest() {
		this.name = null;
		this.action = null;
	}

	/**
	 * @param name
	 *            the name to set
	 * @return the current instance
	 */
	public AffinityBatchRequest setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return the specified action or the default action (STOP_IF_FOUND).
	 */
	@XmlTransient
	@JsonIgnore
	public BatchAction getActionOrDefault() {
		return action == null ? BatchAction.STOP_IF_FOUND : action;
	}
}
