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
package com.qwazr.connectors.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.qwazr.connectors.CassandraConnector;
import com.qwazr.utils.threads.ThreadUtils;
import com.qwazr.utils.threads.ThreadUtils.CallableExceptionCatcher;
import com.qwazr.utils.threads.ThreadUtils.ProcedureExceptionCatcher;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CassandraTest {

	private final static Logger logger = Logger.getLogger(CassandraTest.class
			.getName());

	private CassandraConnector getCassandra() throws IOException {
		return (CassandraConnector) ConnectorsTestContext.getTestContext()
				.getReadOnlyMap().get("cassandra");
	}

	private final static String CREATE_SCHEMA = "CREATE KEYSPACE qwazr_connector_test WITH REPLICATION "
			+ "= { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }";

	private final static String CREATE_TABLE = "CREATE TABLE qwazr_connector_test.test"
			+ "(item_id timeuuid, cat_id int, status text, PRIMARY KEY (item_id))";

	private final static String CREATE_INDEX = "CREATE INDEX ON qwazr_connector_test.test(cat_id)";

	@Test
	public void test_02_create() throws IOException {
		try {
			CassandraConnector cassandra = getCassandra();
			Assert.assertTrue(cassandra.execute(CREATE_SCHEMA).wasApplied());
			Assert.assertTrue(cassandra.execute(CREATE_TABLE).wasApplied());
			Assert.assertTrue(cassandra.execute(CREATE_INDEX).wasApplied());
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		}
	}

	private static long finalTime = 0;

	@Test
	public void test_10_transaction() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		try {
			getCassandra().execute(
					"SELECT count(*) FROM qwazr_connector_test.test").all();
			finalTime = System.currentTimeMillis() + 10000;
			List<ProcedureExceptionCatcher> threadList = new ArrayList<ProcedureExceptionCatcher>();
			for (int i = 0; i < 50; i++) {
				threadList.add(new InsertThread());
				threadList.add(new SelectUpdateThread());
			}
			ThreadUtils.invokeAndJoin(executor, threadList);
			for (CallableExceptionCatcher<?> callable : threadList)
				callable.checkException();
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		} finally {
			executor.shutdown();
		}
	}

	private final static String DROP_TABLE = "DROP TABLE qwazr_connector_test.test";
	private final static String DROP_SCHEMA = "DROP SCHEMA qwazr_connector_test";

	@Test
	public void test_98_drop() throws IOException {
		try {
			CassandraConnector cassandra = getCassandra();
			Assert.assertTrue(cassandra.execute(DROP_TABLE).wasApplied());
			Assert.assertTrue(cassandra.execute(DROP_SCHEMA).wasApplied());
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		}
	}

	private String INSERT = "INSERT INTO qwazr_connector_test.test "
			+ "(item_id, cat_id) VALUES (now(), ?)";

	private class InsertThread extends ProcedureExceptionCatcher {

		@Override
		public void execute() throws Exception {
			long id = Thread.currentThread().getId();
			logger.info("Starts - id: " + id);
			CassandraConnector cassandra = getCassandra();
			int count = 0;
			while (System.currentTimeMillis() < finalTime) {
				Assert.assertTrue(cassandra.execute(INSERT,
						RandomUtils.nextInt(0, 10)).wasApplied());
				count++;
			}
			logger.info("Ends - id: " + id + " - count: " + count);
		}
	}

	private String SELECT = "SELECT * FROM qwazr_connector_test.test"
			+ " WHERE cat_id=?";

	private String UPDATE = "UPDATE qwazr_connector_test.test"
			+ " SET status='ok' WHERE item_id=?";

	private class SelectUpdateThread extends ProcedureExceptionCatcher {

		@Override
		public void execute() throws Exception {
			long id = Thread.currentThread().getId();
			logger.info("Starts - id: " + id);
			CassandraConnector cassandra = getCassandra();
			int count = 0;
			while (System.currentTimeMillis() < finalTime) {
				ResultSet result = cassandra.execute(SELECT,
						RandomUtils.nextInt(0, 10));
				Iterator<Row> it = result.iterator();
				while (it.hasNext()) {
					Row row = it.next();
					cassandra.execute(UPDATE, row.getUUID("item_id"))
							.wasApplied();
					count++;
				}
			}
			logger.info("Ends - id: " + id + " - count: " + count);
		}
	}
}
