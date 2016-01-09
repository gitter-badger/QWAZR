/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.tools.test;

import com.qwazr.tools.Rrd4jTool;
import com.qwazr.utils.IOUtils;
import com.sun.tools.javac.util.Assert;
import org.junit.Test;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;

import java.io.IOException;

public class Rrd4jTest extends AbstractToolsTest {

	@Test
	public void rrd4j() throws IOException {
		Rrd4jTool rrd_memory = getToolManager().get("rrd_memory");
		Assert.checkNonNull(rrd_memory);
		IOUtils.CloseableList closeables = new IOUtils.CloseableList();
		try {
			RrdDb rrdDb = rrd_memory.getDb(closeables);
			Sample sample = rrd_memory.createSample(rrdDb, null);
			Runtime runtime = Runtime.getRuntime();
			sample.setValue("freeMemory", runtime.freeMemory());
			sample.setValue("maxMemory", runtime.maxMemory());
			sample.setValue("totalMemory", runtime.totalMemory());
		} finally {
			closeables.close();
		}
	}
}