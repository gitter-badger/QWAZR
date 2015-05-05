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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;
import com.qwazr.utils.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MongoDbConnector extends AbstractConnector {

	private MongoClient mongoClient = null;

	public static class MongoDbCredential {
		public String username;
		public String password;
		public String database;
	}

	public List<MongoDbCredential> credentials;
	public String hostname;
	public Integer port;

	@Override
	public void load(ConnectorContext context) {
		try {
			ServerAddress serverAddress = null;
			if (!StringUtils.isEmpty(hostname)) {
				if (port == null)
					serverAddress = new ServerAddress(hostname);
				else
					serverAddress = new ServerAddress(hostname, port);
			} else
				serverAddress = new ServerAddress();
			if (credentials == null || credentials.isEmpty()) {
				mongoClient = new MongoClient(serverAddress);
			} else {
				List<MongoCredential> mongoCredentials = new ArrayList<MongoCredential>(
						credentials.size());
				for (MongoDbCredential credential : credentials)
					mongoCredentials.add(MongoCredential
							.createMongoCRCredential(credential.username,
									credential.database,
									credential.password.toCharArray()));
				mongoClient = new MongoClient(serverAddress, mongoCredentials);
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void unload(ConnectorContext context) {
		if (mongoClient != null) {
			mongoClient.close();
			mongoClient = null;
		}
	}

	/**
	 * Return a Mongo DB instance
	 * 
	 * @param databaseName
	 *            the name of the database
	 * @return a DB object
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	public DB getDatabase(String databaseName) throws IOException {
		if (StringUtils.isEmpty(databaseName))
			throw new IOException("No database name.");
		return mongoClient.getDB(databaseName);
	}

	/**
	 * Returns a DB collection instance
	 * 
	 * @param db
	 *            a Mongo DB object
	 * @param collectionName
	 *            the name of the collection
	 * @return a DBCollection object
	 * @throws IOException
	 *             if any I/O error occurs
	 */
	public DBCollection getCollection(DB db, String collectionName)
			throws IOException {
		if (StringUtils.isEmpty(collectionName))
			throw new IOException("No collection name.");
		return db.getCollection(collectionName);
	}

	/**
	 * Build a DBObject from a JSON string
	 * 
	 * @param criteria
	 *            the JSON string
	 * @return a DBObject or NULL if criteria is empty
	 */
	public DBObject getDBObject(String criteria) {
		if (StringUtils.isEmpty(criteria))
			return null;
		return (DBObject) JSON.parse(criteria);
	}

}
