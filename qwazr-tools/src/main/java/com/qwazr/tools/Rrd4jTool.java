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
package com.qwazr.tools;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Rrd4jTool extends AbstractTool {

	private static final Logger logger = LoggerFactory.getLogger(Rrd4jTool.class);

	public final String path = null;
	public final Long startTime = null;
	public final Long step = null;
	public final String backendFactory = null;

	private RrdDef rrdDef = null;
	private RrdDb rrdDb = null;

	@Override
	public void load(File parentDir) throws IOException {
		Objects.requireNonNull(path, "The path property is required");
		final RrdDef rrdDef;
		if (step != null) {
			if (startTime != null) {
				rrdDef = new RrdDef(path, startTime, step);
			} else
				rrdDef = new RrdDef(path, step);
		} else
			rrdDef = new RrdDef(path);
		this.rrdDef = rrdDef;
		this.rrdDb = new RrdDb(rrdDef);
	}

	@Override
	public void unload() {
		if (rrdDb != null && !rrdDb.isClosed()) {
			try {
				rrdDb.close();
			} catch (IOException e) {
				if (logger.isWarnEnabled())
					logger.warn(e.getMessage(), e);
			}
		}
	}

	public RrdDb getDb() {
		return rrdDb;
	}

	public RrdDef getDef() {
		return rrdDef;
	}

}