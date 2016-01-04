/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.store.schema;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.fluent.Request;

import com.qwazr.utils.json.client.JsonClientAbstract;

public class StoreSchemaSingleClient extends JsonClientAbstract implements
		StoreSchemaServiceInterface {

	private final static String PREFIX_PATH = "/store_schema/";
	private final static String REPAIR_PATH = "/repair";

	StoreSchemaSingleClient(String url, int msTimeOut)
			throws URISyntaxException {
		super(url, msTimeOut);
	}

	@Override
	public Set<String> getSchemas(Boolean local, Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH).setParameters(local,
				msTimeout);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut, SetStringTypeRef,
				200);
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName)
				.setParameters(local, msTimeout);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				StoreSchemaDefinition.class, 200);
	}

	@Override
	public StoreSchemaDefinition createSchema(String schemaName,
			StoreSchemaDefinition schemaDef, Boolean local, Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName)
				.setParameters(local, msTimeout);
		Request request = Request.Post(uBuilder.build());
		return commonServiceRequest(request, schemaDef, msTimeOut,
				StoreSchemaDefinition.class, 200);
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName, Boolean local,
			Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName)
				.setParameters(local, msTimeout);
		Request request = Request.Delete(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				StoreSchemaDefinition.class, 200);
	}

	@Override
	public Map<String, StoreSchemaRepairStatus> getRepairStatus(
			String schemaName, Boolean local, Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName, REPAIR_PATH)
				.setParameters(local, msTimeout);
		Request request = Request.Get(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				MapStringRepairTypeRef, 200);
	}

	@Override
	public StoreSchemaRepairStatus startRepair(String schemaName,
			Boolean local, Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName, REPAIR_PATH)
				.setParameters(local, msTimeout);
		Request request = Request.Post(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				StoreSchemaRepairStatus.class, 200);
	}

	@Override
	public Map<String, StoreSchemaRepairStatus> stopRepair(String schemaName,
			Boolean local, Integer msTimeout) {
		UBuilder uBuilder = new UBuilder(PREFIX_PATH, schemaName, REPAIR_PATH)
				.setParameters(local, msTimeout);
		Request request = Request.Delete(uBuilder.build());
		return commonServiceRequest(request, null, msTimeOut,
				MapStringRepairTypeRef, 200);
	}

}
