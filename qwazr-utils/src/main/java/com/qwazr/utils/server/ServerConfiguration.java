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

	public enum PrefixEnum {

		WEBAPP,

		WEBSERVICE
	}

	public enum VariablesEnum {

		QWAZR_DATA,

		LISTEN_ADDR,

		PUBLIC_ADDR,

		REALM,

		AUTHTYPE,

		PORT
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

	final class Connector {

		/**
		 * The port TCP port used by the listening socket
		 */
		final int port;

		final String realm;

		final String authType;

		private Connector(PrefixEnum prefix, int defaultPort) {
			port = getPropertyOrEnvInt(prefix, VariablesEnum.PORT, defaultPort);
			realm = getPropertyOrEnv(prefix, VariablesEnum.REALM);
			authType = getPropertyOrEnv(prefix, VariablesEnum.AUTHTYPE);
		}
	}

	/**
	 * The data directory
	 */
	final File dataDirectory;

	final Connector webServiceConnector;

	final Connector webAppConnector;

	public ServerConfiguration() {

		dataDirectory = buildDataDir(getPropertyOrEnv(null, VariablesEnum.QWAZR_DATA));

		webAppConnector = new Connector(PrefixEnum.WEBAPP, 9090);
		webServiceConnector = new Connector(PrefixEnum.WEBSERVICE, 9091);

		listenAddress = getPropertyOrEnv(null, VariablesEnum.LISTEN_ADDR, "localhost");
		publicAddress = getPropertyOrEnv(null, VariablesEnum.PUBLIC_ADDR, listenAddress);
	}

	private static File buildDataDir(String path) {
		if (!StringUtils.isEmpty(path))
			return new File(path);
		return new File(System.getProperty("user.dir"));
	}

	final protected Integer getPropertyOrEnvInt(PrefixEnum prefix, Enum<?> key, Integer defaultValue) {
		String value = getPropertyOrEnv(prefix, key);
		return value == null ? defaultValue : Integer.parseInt(value.trim());
	}

	final protected String getPropertyOrEnv(PrefixEnum prefix, Enum<?> key) {
		return getPropertyOrEnv(prefix, key, null);
	}

	final protected String getPropertyOrEnv(PrefixEnum prefix, Enum<?> key, String defaultValue) {
		return getProperty(prefix, key, getEnv(prefix, key, defaultValue));
	}

	final public static String getKey(PrefixEnum prefix, Enum<?> key) {
		return prefix == null ? key.name() : prefix.name() + '_' + key.name();
	}

	final protected String getEnv(PrefixEnum prefix, Enum<?> key, String defaultValue) {
		return defaultValue(System.getenv(getKey(prefix, key)), defaultValue);
	}

	final protected String getProperty(PrefixEnum prefix, Enum<?> key, String defaultValue) {
		return defaultValue(System.getProperty(getKey(prefix, key)), defaultValue);
	}

	final protected String defaultValue(String value, String defaultValue) {
		if (value == null)
			return defaultValue;
		value = value.trim();
		return StringUtils.isEmpty(value) ? defaultValue : value;
	}

}


