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
package com.qwazr.graph.test;

import com.qwazr.graph.model.GraphDefinition;
import com.qwazr.graph.model.GraphDefinition.PropertyTypeEnum;
import com.qwazr.graph.model.GraphNode;
import com.qwazr.utils.json.JsonMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FullTest {

	private static volatile boolean started;

	public static final String BASE_URL = "http://localhost:9091/graph";
	public static final String TEST_BASE = "graph-test";
	public static final int PRODUCT_NUMBER = 1000;
	public static final int VISIT_NUMBER = 1000;

	public static final ContentType APPLICATION_JSON_UTF8 = ContentType.create("application/json", Consts.UTF_8);

	@Test
	public void test000CreateDatabase() throws IOException {

		HashMap<String, PropertyTypeEnum> node_properties = new HashMap<String, PropertyTypeEnum>();
		node_properties.put("type", PropertyTypeEnum.indexed);
		node_properties.put("date", PropertyTypeEnum.indexed);
		node_properties.put("name", PropertyTypeEnum.stored);
		node_properties.put("user", PropertyTypeEnum.stored);
		HashSet<String> edge_types = new HashSet<String>();
		edge_types.add("see");
		edge_types.add("buy");
		GraphDefinition graphDef = new GraphDefinition(node_properties, edge_types);

		HttpResponse response = Request.Post(BASE_URL + '/' + TEST_BASE)
				.bodyString(JsonMapper.MAPPER.writeValueAsString(graphDef), APPLICATION_JSON_UTF8).connectTimeout(60000)
				.socketTimeout(60000).execute().returnResponse();
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
	}

	@Test
	public void test100PutProductNodes() throws IOException {
		for (int i = 0; i < PRODUCT_NUMBER; i++) {
			GraphNode node = new GraphNode();
			node.properties = new HashMap<String, Object>();
			node.properties.put("type", "product");
			node.properties.put("name", "product" + i);
			HttpResponse response = Request.Post(BASE_URL + '/' + TEST_BASE + "/node/p" + i)
					.bodyString(JsonMapper.MAPPER.writeValueAsString(node), APPLICATION_JSON_UTF8).connectTimeout(60000)
					.socketTimeout(60000).execute().returnResponse();
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		}
	}

	@Test
	public void test110PutVisitNodes() throws IOException {
		for (int i = 0; i < VISIT_NUMBER; i += 100) {
			Map<String, GraphNode> nodeMap = new LinkedHashMap<String, GraphNode>();
			for (int k = 0; k < 100; k++) {
				GraphNode node = new GraphNode();
				node.properties = new HashMap<String, Object>();
				node.properties.put("type", "visit");
				node.properties.put("user", "user" + RandomUtils.nextInt(0, 100));
				node.properties.put("date", "201501" + RandomUtils.nextInt(10, 31));
				node.edges = new HashMap<String, Set<Object>>();
				int seePages = RandomUtils.nextInt(3, 12);
				Set<Object> set = new TreeSet<Object>();
				for (int j = 0; j < seePages; j++)
					set.add("p" + RandomUtils.nextInt(0, PRODUCT_NUMBER / 2));
				node.edges.put("see", set);
				if (RandomUtils.nextInt(0, 10) == 0) {
					int buyItems = RandomUtils.nextInt(1, 5);
					set = new TreeSet<Object>();
					for (int j = 0; j < buyItems; j++)
						set.add("p" + RandomUtils.nextInt(0, PRODUCT_NUMBER / 2));
					node.edges.put("buy", set);
				}
				nodeMap.put("v" + (i + k), node);
			}
			HttpResponse response = Request.Post(BASE_URL + '/' + TEST_BASE + "/node")
					.bodyString(JsonMapper.MAPPER.writeValueAsString(nodeMap), APPLICATION_JSON_UTF8)
					.connectTimeout(60000).socketTimeout(60000).execute().returnResponse();
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		}
	}

	private boolean nodeExists(int visiteNodeId) throws IOException {
		HttpResponse response = Request.Get(BASE_URL + '/' + TEST_BASE + "/node/v" + visiteNodeId).connectTimeout(60000)
				.socketTimeout(60000).execute().returnResponse();
		Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
		Assert.assertThat(response.getEntity().getContentType().getValue(), Is.is(APPLICATION_JSON_UTF8.toString()));
		return response.getStatusLine().getStatusCode() == 200;
	}

	@Test
	public void test200PutRandomEdges() throws IOException {
		for (int i = 0; i < VISIT_NUMBER / 100; i++) {
			int visitNodeId = RandomUtils.nextInt(VISIT_NUMBER / 2, VISIT_NUMBER);
			if (!nodeExists(visitNodeId))
				continue;
			int productNodeId = RandomUtils.nextInt(PRODUCT_NUMBER / 2, PRODUCT_NUMBER);
			HttpResponse response = Request
					.Post(BASE_URL + '/' + TEST_BASE + "/node/v" + visitNodeId + "/edge/see/p" + productNodeId)
					.connectTimeout(60000).socketTimeout(60000).execute().returnResponse();
			if (response.getStatusLine().getStatusCode() == 500)
				System.out.println(IOUtils.toString(response.getEntity().getContent()));
			Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
			Assert.assertThat(response.getEntity().getContentType().getValue(),
					Is.is(APPLICATION_JSON_UTF8.toString()));
		}
	}

	@Test
	public void test210DeleteRandomEdges() throws IOException {
		for (int i = 0; i < VISIT_NUMBER / 100; i++) {
			int visiteNodeId = RandomUtils.nextInt(0, VISIT_NUMBER / 2);
			if (!nodeExists(visiteNodeId))
				continue;
			int productNodeId = RandomUtils.nextInt(0, PRODUCT_NUMBER / 2);
			HttpResponse response = Request
					.Delete(BASE_URL + '/' + TEST_BASE + "/node/v" + visiteNodeId + "/edge/see/p" + productNodeId)
					.connectTimeout(60000).socketTimeout(60000).execute().returnResponse();
			Assert.assertThat(response.getStatusLine().getStatusCode(), AnyOf.anyOf(Is.is(200), Is.is(404)));
		}
	}

	@Test
	public void test999DeleteDatabase() throws IOException {
		HttpResponse response = Request.Delete(BASE_URL + '/' + TEST_BASE).connectTimeout(60000).socketTimeout(60000)
				.execute().returnResponse();
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());

	}
}
