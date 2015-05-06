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
