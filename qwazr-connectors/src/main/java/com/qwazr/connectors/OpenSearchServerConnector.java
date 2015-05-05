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
package com.qwazr.connectors;

import java.net.URISyntaxException;

import org.apache.hadoop.fs.FileSystem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opensearchserver.client.JsonClient1;
import com.opensearchserver.client.v1.AnalyzerApi1;
import com.opensearchserver.client.v1.DictionaryApi1;
import com.opensearchserver.client.v1.DocumentApi1;
import com.opensearchserver.client.v1.FieldApi1;
import com.opensearchserver.client.v1.IndexApi1;
import com.opensearchserver.client.v1.ReplicationApi1;
import com.opensearchserver.client.v1.SearchApi1;
import com.opensearchserver.client.v1.UpdateApi1;
import com.opensearchserver.client.v1.WebCrawlerApi1;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchServerConnector extends AbstractConnector {

	public final String url = null;

	public final String login = null;

	public final String api_key = null;

	public final Integer time_out = null;

	@JsonIgnore
	private FileSystem fileSystem;

	@JsonIgnore
	private JsonClient1 jsonClient;

	@Override
	public void load(ConnectorContext context) {
		try {
			jsonClient = new JsonClient1(url, login, api_key,
					time_out == null ? 60000 : time_out);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void unload(ConnectorContext context) {
	}

	public AnalyzerApi1 getNewAnalyzerApi1() {
		return new AnalyzerApi1(jsonClient);
	}

	public DictionaryApi1 getNewDictionaryApi1() {
		return new DictionaryApi1(jsonClient);
	}

	public DocumentApi1 getNewDocumentApi1() {
		return new DocumentApi1(jsonClient);
	}

	public FieldApi1 getNewFieldApi1() {
		return new FieldApi1(jsonClient);
	}

	public IndexApi1 getNewIndexApi1() {
		return new IndexApi1(jsonClient);
	}

	public ReplicationApi1 getNewReplicationApi1() {
		return new ReplicationApi1(jsonClient);
	}

	public SearchApi1 getNewSearchApi1() {
		return new SearchApi1(jsonClient);
	}

	public UpdateApi1 getNewUpdateApi1() {
		return new UpdateApi1(jsonClient);
	}

	public WebCrawlerApi1 getNewWebCrawlerApi1() {
		return new WebCrawlerApi1(jsonClient);
	}
}
