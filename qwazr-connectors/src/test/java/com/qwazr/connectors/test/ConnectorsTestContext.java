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
package com.qwazr.connectors.test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorManager;
import com.qwazr.connectors.ConnectorManagerImpl;

import java.io.File;
import java.io.IOException;

public class ConnectorsTestContext {

	static synchronized ConnectorManager getConnectorManager()
					throws IOException {
		if (ConnectorManagerImpl.getInstance() != null)
			return ConnectorManagerImpl.getInstance();
		ConnectorManagerImpl.load(new File("src/test/resources"));
		return ConnectorManagerImpl.getInstance();
	}

}
