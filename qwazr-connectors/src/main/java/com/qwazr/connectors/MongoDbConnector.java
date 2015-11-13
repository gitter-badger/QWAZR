/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.connectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.qwazr.utils.StringUtils;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MongoDbConnector extends AbstractConnector {

	private MongoClient mongoClient = null;

	public static class MongoDbCredential {
		final public String username = null;
		final public String password = null;
		final public String database = null;
	}

	public static class MongoServerAddress {
		final public String hostname = null;
		final public Integer port = null;
	}

	public List<MongoDbCredential> credentials;
	public List<MongoServerAddress> servers;
	public Integer port;

	@Override
	public void load(File data_directory) {
		List<ServerAddress> serverAddresses = new ArrayList();
		for (MongoServerAddress server : servers) {
			ServerAddress serverAddress = server.port == null ?
							new ServerAddress(server.hostname) :
							new ServerAddress(server.hostname, server.port);
			serverAddresses.add(serverAddress);
		}
		if (credentials == null || credentials.isEmpty()) {
			mongoClient = new MongoClient(serverAddresses);
		} else {
			List<MongoCredential> mongoCredentials = new ArrayList<MongoCredential>(credentials.size());
			for (MongoDbCredential credential : credentials)
				mongoCredentials.add(MongoCredential.createMongoCRCredential(credential.username, credential.database,
								credential.password.toCharArray()));
			mongoClient = new MongoClient(serverAddresses, mongoCredentials);
		}

	}

	@Override
	public void unload() {
		if (mongoClient != null) {
			mongoClient.close();
			mongoClient = null;
		}
	}

	/**
	 * Return a Mongo DB instance
	 *
	 * @param databaseName the name of the database
	 * @return a MongoDatabase object
	 * @throws IOException if any I/O error occurs
	 */
	public MongoDatabase getDatabase(String databaseName) throws IOException {
		if (StringUtils.isEmpty(databaseName))
			throw new IOException("No database name.");
		return mongoClient.getDatabase(databaseName);
	}

	/**
	 * Returns a DB collection instance
	 *
	 * @param db             a MongoDatabase object
	 * @param collectionName the name of the collection
	 * @return a MongoCollection object
	 * @throws IOException if any I/O error occurs
	 */
	public MongoCollection<Document> getCollection(MongoDatabase db, String collectionName) throws IOException {
		if (StringUtils.isEmpty(collectionName))
			throw new IOException("No collection name.");
		return db.getCollection(collectionName);
	}

	/**
	 * Build a BSON Document from a JSON string
	 *
	 * @param json the JSON string
	 * @return a Document or NULL if json is empty
	 */
	public Document getNewDocument(String json) {
		if (StringUtils.isEmpty(json))
			return null;
		return Document.parse(json);
	}

	public Document newDocument(String json) {
		return getNewDocument(json);
	}

	/**
	 * Build a BSON Document from a MAP
	 *
	 * @param map a map
	 * @return a Document or NULL if the MAP is null
	 */
	public Document getNewDocument(Map<String, Object> map) {
		if (map == null)
			return null;
		return new Document(map);
	}

	/**
	 * Create a new UpdateOptions object
	 *
	 * @param upsert true if a new document should be inserted if there are no
	 *               matches to the query filter
	 * @return a new UpdateOptions object
	 */
	public UpdateOptions getNewUpdateOptions(boolean upsert) {
		UpdateOptions updateOptions = new UpdateOptions();
		updateOptions.upsert(upsert);
		return updateOptions;
	}

	public UpdateOptions newUpdateOptions(boolean upsert) {
		return getNewUpdateOptions(upsert);
	}

}
