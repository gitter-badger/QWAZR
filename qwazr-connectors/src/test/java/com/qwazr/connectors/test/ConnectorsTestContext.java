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
 **/
package com.qwazr.connectors.test;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorManager;

public class ConnectorsTestContext {

	private static ConnectorsTestContext testContext = null;

	static synchronized ConnectorsTestContext getTestContext()
			throws JsonParseException, JsonMappingException, IOException {
		if (testContext != null)
			return testContext;
		testContext = new ConnectorsTestContext();
		File file = new File("src/test/resources");
		ConnectorManager.load(null, file, null);
		return testContext;
	}

}
