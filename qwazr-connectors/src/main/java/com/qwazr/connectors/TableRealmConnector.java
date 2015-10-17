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
 **/
package com.qwazr.connectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qwazr.database.TableServiceImpl;
import com.qwazr.database.model.ColumnDefinition;
import com.qwazr.database.model.TableRequest;
import com.qwazr.database.model.TableRequestResult;
import com.qwazr.utils.json.JsonMapper;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TableRealmConnector extends AbstractConnector implements IdentityManager {

	public final String table_name = null;
	public final String login_column = null;
	public final String password_column = null;
	public final String roles_column = null;

	private TableServiceImpl tableService = null;

	@Override
	public void load(File parentDir) {
		tableService = new TableServiceImpl();
		Set<String> tables = tableService.list(null, true);
		if (!tables.contains(table_name)) {
			tableService.createTable(table_name, null, true);
			tableService.addColumn(table_name, login_column,
							new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.INDEXED), null,
							true);
			tableService.addColumn(table_name, password_column,
							new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.STORED), null,
							true);
			tableService.addColumn(table_name, roles_column,
							new ColumnDefinition(ColumnDefinition.Type.STRING, ColumnDefinition.Mode.STORED), null,
							true);
		}
	}

	@Override
	public void unload() {

	}

	@Override
	public Account verify(Account account) {
		return account;
	}

	@Override
	public Account verify(String id, Credential credential) {

		// This realm only support one type of credential
		if (!(credential instanceof PasswordCredential))
			throw new RuntimeException("Unsupported credential type: " + credential.getClass().getName());
		PasswordCredential passwordCredential = (PasswordCredential) credential;

		// We build the query
		ObjectNode jsonQuery = JsonMapper.MAPPER.createObjectNode();
		jsonQuery.put(login_column, id);
		TableRequest request = new TableRequest(0, 1, null, null, jsonQuery);
		TableRequestResult result = tableService.queryRows(table_name, request);
		if (result.count == null || result.count == 0)
			return null;
		Map<String, Object> row = result.rows.get(0);
		Object password = row.get(password_column);
		if (password == null)
			return null;

		// The password is stored hashed
		String digest = DigestUtils.md5Hex(new String(passwordCredential.getPassword()));
		if (!digest.equals(password))
			return null;

		//We retrieve the roles
		Object object = row.get(roles_column);
		LinkedHashSet<String> roles = new LinkedHashSet<String>();
		if (object instanceof ArrayList<?>) {
			for (Object o : (ArrayList<?>) object)
				roles.add(o.toString());
		} else
			roles.add(object.toString());

		return new Account() {
			@Override
			public Principal getPrincipal() {
				return new Principal() {
					@Override
					public String getName() {
						return id;
					}
				};
			}

			@Override
			public Set<String> getRoles() {
				return roles;
			}
		};
	}

	@Override
	public Account verify(Credential credential) {
		return null;
	}
}
