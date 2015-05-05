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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/analyze/markdown")
public interface MarkdownServiceInterface {

	@GET
	@Path("/")
	Map<String, MarkdownProcessorDefinition> listProcessors();

	@GET
	@Path("/{processor_name}")
	Map<String, MarkdownProcessorDefinition> getProcessor();

	@PUT
	@Path("/{processor_name}")
	MarkdownProcessorDefinition setProcessor(
			@PathParam("processor_name") String processorName,
			MarkdownProcessorDefinition processorDefinition);

	@DELETE
	@Path("/{processor_name}")
	void deleteProcessor(String processorName);

	@POST
	@Path("/{processor_name}")
	String convert(@QueryParam("path") String path, InputStream content);
}
