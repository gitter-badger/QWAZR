/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qwazr.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;

import java.io.File;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CSVPrinter extends AbstractTool {

	public enum Format {

		DEFAULT(CSVFormat.DEFAULT),

		EXCEL(CSVFormat.EXCEL),

		MYSQL(CSVFormat.MYSQL),

		RFC4180(CSVFormat.RFC4180),

		TDF(CSVFormat.TDF);

		private final CSVFormat csvFormat;

		private Format(CSVFormat csvFormat) {
			this.csvFormat = csvFormat;
		}
	}

	public final Format format = Format.DEFAULT;

	@Override
	public void load(File parentDir) {
	}

	@Override
	public void unload() {
	}

	public org.apache.commons.csv.CSVPrinter getNewPrinter(Appendable appendable, IOUtils.CloseableContext closeable)
					throws IOException {
		return getNewPrinter(format.csvFormat, appendable, closeable);
	}

	public org.apache.commons.csv.CSVPrinter getNewPrinter(CSVFormat format, Appendable appendable,
					IOUtils.CloseableContext closeable) throws IOException {
		org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(appendable, format);
		if (closeable != null)
			closeable.add(printer);
		return printer;
	}

}