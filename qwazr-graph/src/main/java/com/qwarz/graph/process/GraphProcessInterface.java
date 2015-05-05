/**
 * Copyright 2015 OpenSearchServer Inc.
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
 */
package com.qwarz.graph.process;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.qwarz.graph.model.GraphBase;
import com.qwarz.graph.model.GraphNode;
import com.qwarz.graph.model.GraphNodeResult;
import com.qwarz.graph.model.GraphRequest;
import com.qwazr.utils.server.ServerException;

public interface GraphProcessInterface {

	/**
	 * Create a new graph database
	 * 
	 * @param base
	 *            a Graph database instance
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void createDataIndex(GraphBase base) throws URISyntaxException,
			IOException, ServerException;

	/**
	 * Load the database
	 * 
	 * @param base
	 *            a Graph database instance
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void load(GraphBase base) throws IOException, ServerException;

	/**
	 * Delete a graph database
	 * 
	 * @param base
	 *            a Graph database instance
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void deleteDataIndex(GraphBase base) throws URISyntaxException,
			IOException, ServerException;

	/**
	 * Create a new node. If the node does not exists, it is created. If the
	 * node exist, it is updated.
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param node_id
	 *            the ID of the node
	 * @param node
	 *            the content of the node
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void createUpdateNode(GraphBase base, String node_id, GraphNode node)
			throws IOException, URISyntaxException, ServerException;

	/**
	 * Create a list of node. If a node does not exists, it is created.
	 * Otherwise it is updated.
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param nodes
	 *            a map of nodes
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void createUpdateNodes(GraphBase base, Map<String, GraphNode> nodes)
			throws IOException, URISyntaxException, ServerException;

	/**
	 * Retrieve a node. If the node does not exists, an IOException is thrown
	 * thrown.
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param node_id
	 *            the id of the node
	 * @return a node instance
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	GraphNode getNode(GraphBase base, String node_id) throws IOException,
			URISyntaxException, ServerException;

	/**
	 * Retrieve a list of nodes.
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param node_ids
	 *            a collection of node id
	 * @return a map with the nodes
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	Map<String, GraphNode> getNodes(GraphBase base, Collection<String> node_ids)
			throws IOException, URISyntaxException, ServerException;

	/**
	 * Delete a node.
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param node_id
	 *            the ID of the node to delete
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void deleteNode(GraphBase base, String node_id) throws IOException,
			URISyntaxException, ServerException;

	/**
	 * Execute a Graph request
	 * 
	 * @param base
	 *            a Graph database instance
	 * @param request
	 *            the Graph request definition
	 * @param results
	 *            a collection to fill with the results
	 * @throws URISyntaxException
	 *             if the server parameters are wrong
	 * @throws IOException
	 *             if any I/O error occurs
	 * @throws ServerException
	 *             if any server exception occurs
	 */
	void request(GraphBase base, GraphRequest request,
			List<GraphNodeResult> results) throws ServerException, IOException,
			URISyntaxException;

}
