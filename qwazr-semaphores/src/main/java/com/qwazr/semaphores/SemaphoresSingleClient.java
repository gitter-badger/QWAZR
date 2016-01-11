/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.semaphores;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qwazr.utils.json.client.JsonClientAbstract;
import org.apache.http.client.fluent.Request;

import java.net.URISyntaxException;
import java.util.Set;

public class SemaphoresSingleClient extends JsonClientAbstract implements SemaphoresServiceInterface {

	private final static String SCRIPT_PREFIX_SEMAPHORES = "/semaphores/";

	SemaphoresSingleClient(String url, Integer msTimeOut) throws URISyntaxException {
		super(url, msTimeOut);
	}

	public final static TypeReference<Set<String>> SetStringTypeRef = new TypeReference<Set<String>>() {
	};

	@Override
	public Set<String> getSemaphores() {
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_SEMAPHORES);
		Request request = Request.Get(uriBuilder.build());
		return commonServiceRequest(request, null, msTimeOut, SetStringTypeRef, 200);
	}

	@Override
	public Set<String> getSemaphoreOwners(String semaphore_id) {
		UBuilder uriBuilder = new UBuilder(SCRIPT_PREFIX_SEMAPHORES, semaphore_id);
		Request request = Request.Get(uriBuilder.build());
		return commonServiceRequest(request, null, msTimeOut, SetStringTypeRef, 200);
	}
}
