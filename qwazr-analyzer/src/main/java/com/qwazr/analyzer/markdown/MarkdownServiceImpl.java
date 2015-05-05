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
package com.qwazr.analyzer.markdown;

import java.io.InputStream;
import java.util.Map;

public class MarkdownServiceImpl implements MarkdownServiceInterface {

	@Override
	public Map<String, MarkdownProcessorDefinition> listProcessors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, MarkdownProcessorDefinition> getProcessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MarkdownProcessorDefinition setProcessor(String processorName,
			MarkdownProcessorDefinition processorDefinition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteProcessor(String processorName) {
		// TODO Auto-generated method stub

	}

	@Override
	public String convert(String path, InputStream content) {
		// TODO Auto-generated method stub
		return null;
	}

}
