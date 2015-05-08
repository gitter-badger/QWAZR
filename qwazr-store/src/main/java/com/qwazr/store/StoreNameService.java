/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.store;

import java.io.InputStream;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/store")
public class StoreNameService implements StoreNameServiceInterface {

	@Override
	public Response getFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response headFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response putFile(String schemaName, String path,
			InputStream inputStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response createDirectory(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response deleteFile(String schemaName, String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition getSchema(String schemaName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition postSchema(String schemaName,
			StoreSchemaDefinition schemaDef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoreSchemaDefinition deleteSchema(String schemaName) {
		// TODO Auto-generated method stub
		return null;
	}

}
