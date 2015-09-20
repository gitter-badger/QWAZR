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
 */
package com.qwazr.store.schema;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.store.StoreServer;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.json.DirectoryJsonManager;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.lang3.RandomUtils;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreSchemaManager extends DirectoryJsonManager<StoreSchemaDefinition> {

    public static volatile StoreSchemaManager INSTANCE = null;

    public static void load(File storeDirectory) throws IOException {
	if (INSTANCE != null)
	    throw new IOException("Already loaded");
	try {
	    INSTANCE = new StoreSchemaManager(storeDirectory);
	} catch (ServerException e) {
	    throw new RuntimeException(e);
	}
    }

    private final LockUtils.ReadWriteLock rwlSchemas = new LockUtils.ReadWriteLock();
    private final Map<String, StoreSchemaInstance> schemaInstanceMap;
    private final ExecutorService executor;

    private StoreSchemaManager(File storeDirectory) throws IOException, ServerException {
	super(storeDirectory, StoreSchemaDefinition.class);
	schemaInstanceMap = new HashMap<String, StoreSchemaInstance>();
	executor = Executors.newFixedThreadPool(8);
    }

    StoreSchemaInstance getSchemaInstance(String schemaName) throws ServerException, IOException {
	rwlSchemas.r.lock();
	try {
	    StoreSchemaInstance schemaInstance = schemaInstanceMap.get(schemaName);
	    if (schemaInstance != null)
		return schemaInstance;
	} finally {
	    rwlSchemas.r.unlock();
	}
	rwlSchemas.w.lock();
	try {
	    StoreSchemaInstance schemaInstance = schemaInstanceMap.get(schemaName);
	    if (schemaInstance != null)
		return schemaInstance;
	    StoreSchemaDefinition schemaDefininition = get(schemaName);
	    if (schemaDefininition == null)
		throw new ServerException(Status.NOT_FOUND, "Schema not found : " + schemaName);
	    schemaInstance = new StoreSchemaInstance(directory, schemaName);
	    schemaInstanceMap.put(schemaName, schemaInstance);
	    return schemaInstance;
	} finally {
	    rwlSchemas.w.unlock();
	}
    }

    public StoreSchemaDefinition getSchema(String schemaName) throws ServerException, IOException {
	StoreSchemaDefinition schemaDefinition = super.get(schemaName);
	if (schemaDefinition != null)
	    return schemaDefinition;
	throw new ServerException(Status.NOT_FOUND, "Schema not found: " + schemaName);
    }

    Set<String> getSchemas() {
	rwlSchemas.r.lock();
	try {
	    TreeSet<String> set = new TreeSet<String>();
	    set.addAll(super.nameSet());
	    return set;
	} finally {
	    rwlSchemas.r.unlock();
	}
    }

    void createSchema(String schemaName, StoreSchemaDefinition schemaDefinition) throws ServerException, IOException {
	rwlSchemas.w.lock();
	try {
	    if (super.get(schemaName) != null)
		throw new ServerException(Status.CONFLICT, "The schema already exists: " + schemaName);
	    super.set(schemaName, schemaDefinition);
	} finally {
	    rwlSchemas.w.unlock();
	}
    }

    StoreSchemaDefinition deleteSchema(String schemaName) throws ServerException, IOException {
	rwlSchemas.w.lock();
	try {
	    StoreSchemaInstance schemaInstance = schemaInstanceMap.get(schemaName);
	    if (schemaInstance != null) {
		schemaInstance.close();
		schemaInstanceMap.remove(schemaName);
	    }
	    StoreSchemaDefinition schemaDefinition = super.delete(schemaName);
	    if (schemaDefinition == null)
		throw new ServerException(Status.NOT_FOUND, "Schema not found : " + schemaName);
	    return schemaDefinition;
	} finally {
	    rwlSchemas.w.unlock();
	}
    }

    public static void checkSchemaDefinition(StoreSchemaDefinition schemaDefinition) throws ServerException {

	// If not set, we set the default values for the replication and the
	// distribution factor
	if (schemaDefinition.replication_factor == null)
	    schemaDefinition.replication_factor = 1;
	if (schemaDefinition.distribution_factor == null)
	    schemaDefinition.distribution_factor = 1;
	int nodesNumber = schemaDefinition.replication_factor * schemaDefinition.distribution_factor;

	// If the node has been given, we check that the count conforms the
	// requirements
	if (schemaDefinition.nodes != null) {
	    if (schemaDefinition.nodes.length != schemaDefinition.replication_factor)
		throw new ServerException(Status.NOT_ACCEPTABLE,
				"The number of replicated nodes is not correct: " + schemaDefinition.nodes.length
						+ " versus " + schemaDefinition.replication_factor);
	    for (String[] nodes : schemaDefinition.nodes)
		if (nodes == null || nodes.length != schemaDefinition.distribution_factor)
		    throw new ServerException(Status.NOT_ACCEPTABLE, "The number of distributed nodes is not correct: "
				    + schemaDefinition.distribution_factor + " was expected.");
	    return;
	}

	// We retrieve the list of all available store nodes
	String[] nodes = ClusterManager.INSTANCE.getActiveNodes(StoreServer.SERVICE_NAME_STORE);
	if (nodes == null || nodes.length < nodesNumber)
	    throw new ServerException(Status.NOT_ACCEPTABLE,
			    "Not enough store servers to handle the request: " + nodesNumber + " nodes expected.");

	// We select the nodes using Murmur3 hashing
	Charset charset = Charset.defaultCharset();
	HashFunction m3 = Hashing.murmur3_128(RandomUtils.nextInt(0, Integer.MAX_VALUE));
	TreeMap<String, String> nodesMap = new TreeMap<String, String>();
	for (String node : nodes)
	    nodesMap.put(m3.hashString(node, charset).toString(), node);
	schemaDefinition.nodes = new String[schemaDefinition.replication_factor][];
	Iterator<String> nodeIterator = nodesMap.values().iterator();
	for (int i = 0; i < schemaDefinition.replication_factor; i++) {
	    String[] nodeArray = new String[schemaDefinition.distribution_factor];
	    for (int j = 0; j < schemaDefinition.distribution_factor; j++)
		nodeArray[j] = nodeIterator.next();
	    schemaDefinition.nodes[i] = nodeArray;
	}
    }

    public StoreSchemaMultiClient getNewSchemaClient(Integer msTimeOut) throws URISyntaxException {
	String[] masters = ClusterManager.INSTANCE.getMasterArray();
	if (masters == null)
	    return null;
	return new StoreSchemaMultiClient(executor, masters, msTimeOut == null ? 60000 : msTimeOut);
    }

}
