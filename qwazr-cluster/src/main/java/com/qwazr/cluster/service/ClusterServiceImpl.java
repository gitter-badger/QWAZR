/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.cluster.service;

import com.qwazr.cluster.manager.ClusterManager;
import com.qwazr.cluster.manager.ClusterNode;
import com.qwazr.utils.server.ServerException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ClusterServiceImpl implements ClusterServiceInterface {

	@Context
	HttpServletRequest request;

	@Override
	public ClusterStatusJson list() {
		try {
			//System.out.println("PRINCIPAL: " + request.getUserPrincipal().getName());
			//if (!request.isUserInRole(ClusterServer.SERVICE_NAME_CLUSTER))
			//	throw new ServerException(Status.UNAUTHORIZED);
			List<ClusterNode> clusterNodeList = ClusterManager.INSTANCE.getNodeList();
			if (clusterNodeList == null)
				return null;
			ClusterStatusJson clusterStatus = new ClusterStatusJson(ClusterManager.INSTANCE);
			for (ClusterNode clusterNode : clusterNodeList)
				clusterStatus.addNodeStatus(clusterNode);
			return clusterStatus;
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public Map<String, ClusterNodeJson> getNodes() {
		try {
			Map<String, ClusterNodeJson> nodeMap = new HashMap<String, ClusterNodeJson>();
			List<ClusterNode> clusterNodeList;
			clusterNodeList = ClusterManager.INSTANCE.getNodeList();
			if (clusterNodeList == null)
				return nodeMap;
			for (ClusterNode clusterNode : clusterNodeList)
				if (clusterNode.services != null && !clusterNode.services.isEmpty())
					nodeMap.put(clusterNode.address, new ClusterNodeJson(clusterNode));
			return nodeMap;
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public Response check(String checkValue, String checkAddr) {
		ClusterManager.INSTANCE.check(checkAddr);
		return Response.ok().header(ClusterServiceInterface.HEADER_CHECK_NAME, checkValue).build();
	}

	@Override
	public ClusterNodeStatusJson register(ClusterNodeJson register) {
		if (register == null)
			throw new ServerException(Status.NOT_ACCEPTABLE).getJsonException();
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return manager.upsertNode(register).getStatus();
		} catch (Exception e) {
			throw ServerException.getJsonException(e);
		}
	}

	@Override
	public Response unregister(String address) {
		if (address == null)
			throw new ServerException(Status.NOT_ACCEPTABLE).getJsonException();
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			ClusterNode clusterNode = manager.removeNode(address);
			return clusterNode == null ? Response.status(Status.NOT_FOUND).build() : Response.ok().build();
		} catch (Exception e) {
			throw ServerException.getTextException(e);
		}
	}

	@Override
	public String[] getActiveNodesByService(String service_name, String group) {
		if (service_name == null)
			throw new ServerException(Status.NOT_ACCEPTABLE).getJsonException();
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return ClusterManager.getActiveNodes(manager.getNodeSetCacheService(service_name, group));
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public String getActiveNodeRandomByService(String service_name, String group) {
		if (service_name == null)
			throw new ServerException(Status.NOT_ACCEPTABLE).getJsonException();
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return ClusterManager.getActiveNodeRandom(manager.getNodeSetCacheService(service_name, group));
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public String getActiveNodeMasterByService(String service_name, String group) {
		if (service_name == null)
			throw new ServerException(Status.NOT_ACCEPTABLE).getJsonException();
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return manager.getNodeSetCacheService(service_name, group).leader;
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public TreeMap<String, ClusterServiceStatusJson.StatusEnum> getServiceMap(String group) {
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return manager.getServicesStatusMap(group);
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

	@Override
	public ClusterServiceStatusJson getServiceStatus(String service_name, String group) {
		ClusterManager manager = ClusterManager.INSTANCE;
		try {
			return ClusterManager.getStatus(manager.getNodeSetCacheService(service_name, group));
		} catch (ServerException e) {
			throw e.getJsonException();
		}
	}

}
