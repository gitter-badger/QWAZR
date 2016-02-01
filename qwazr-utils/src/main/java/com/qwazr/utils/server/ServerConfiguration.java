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
 **/
package com.qwazr.utils.server;

import com.qwazr.utils.StringUtils;

import java.io.File;

public class ServerConfiguration {

	public enum VariablesEnum {

		QWAZR_DATA,

		QWAZR_REALM,

		QWAZR_AUTHTYPE,

		WEBAPP_PORT,

		WEBSERVICE_PORT,

		LISTEN_ADDR,

		PUBLIC_ADDR
	}

	/**
	 * The hostname or address uses for the listening socket
	 */
	final String listenAddress;

	/**
	 * The public hostname or address and port for external access (node
	 * communication)
	 */
	final String publicAddress;

	/**
	 * The port TCP port used by the listening socket
	 */
	final int servletPort;

	/**
	 * The port TCP port used by the listening socket
	 */
	final int restPort;

	/**
	 *
	 */
	final String webServiceRealm;

	/**
	 *
	 */
	final String webServiceAuthType;

	/**
	 * The data directory
	 */
	final File dataDirectory;

	public ServerConfiguration() {

		dataDirectory = buildDataDir(getPropertyOrEnv(VariablesEnum.QWAZR_DATA));
		webServiceRealm = getPropertyOrEnv(VariablesEnum.QWAZR_REALM);
		webServiceAuthType = getPropertyOrEnv(VariablesEnum.QWAZR_AUTHTYPE);

		servletPort = getPropertyOrEnvInt(VariablesEnum.WEBAPP_PORT, 9090);
		restPort = getPropertyOrEnvInt(VariablesEnum.WEBSERVICE_PORT, 9091);

		listenAddress = getPropertyOrEnv(VariablesEnum.LISTEN_ADDR, "localhost");
		publicAddress = getPropertyOrEnv(VariablesEnum.PUBLIC_ADDR, listenAddress);

	}

	private static File buildDataDir(String path) {
		if (!StringUtils.isEmpty(path))
			return new File(path);
		return new File(System.getProperty("user.dir"));
	}

	final protected Integer getPropertyOrEnvInt(Enum<?> key, Integer defaultValue) {
		String value = getPropertyOrEnv(key);
		return value == null ? defaultValue : Integer.parseInt(value.trim());
	}

	final protected String getPropertyOrEnv(Enum<?> key) {
		return getPropertyOrEnv(key, null);
	}

	final protected String getPropertyOrEnv(Enum<?> key, String defaultValue) {
		return getProperty(key.name(), getEnv(key, defaultValue));
	}

	final protected String getEnv(Enum<?> key, String defaultValue) {
		return defaultValue(System.getenv(key.name()), defaultValue);
	}

	final protected String getProperty(String key, String defaultValue) {
		return defaultValue(System.getProperty(key), defaultValue);
	}

	final protected String defaultValue(String value, String defaultValue) {
		if (value == null)
			return defaultValue;
		value = value.trim();
		return StringUtils.isEmpty(value) ? defaultValue : value;
	}

}


