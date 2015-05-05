/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.connectors.test;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.qwazr.connectors.ConnectorContextAbstract;
import com.qwazr.connectors.ConnectorsConfigurationFile;
import com.qwazr.utils.json.JsonMapper;

public class ConnectorsTestContext extends ConnectorContextAbstract {

	private static ConnectorsTestContext testContext = null;

	static synchronized ConnectorsTestContext getTestContext()
			throws JsonParseException, JsonMappingException, IOException {
		if (testContext != null)
			return testContext;
		testContext = new ConnectorsTestContext();
		File file = new File("src/test/resources/connectors.json");
		ConnectorsConfigurationFile.load(testContext, JsonMapper.MAPPER
				.readValue(file, ConnectorsConfigurationFile.class));
		return testContext;
	}

	@Override
	public String getContextId() {
		return "Test";
	}

	@Override
	public File getContextDirectory() {
		return null;
	}

}
