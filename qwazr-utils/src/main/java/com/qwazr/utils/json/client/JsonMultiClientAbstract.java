/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.json.client;

import com.qwazr.utils.RandomArrayIterator;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

/**
 * This class represents a connection to a set of servers
 *
 * @param <K> the type of the key used to locate one server
 * @param <V> The type of the class which handle the connection to one server
 */
public abstract class JsonMultiClientAbstract<K, V> implements Iterable<V> {

	private final V[] clientsArray;
	private final HashMap<K, V> clientsMap;
	protected final ExecutorService executor;

	/**
	 * Create a new multi client
	 *
	 * @param executor    An executor service for parallels connections
	 * @param clientArray an array of client connection
	 * @param clientKeys  an array of key values used to create the client connection
	 * @param msTimeOut   the default timeout
	 * @throws URISyntaxException thrown in case of wrong URI syntax
	 */
	protected JsonMultiClientAbstract(ExecutorService executor, V[] clientArray, K[] clientKeys, Integer msTimeOut)
			throws URISyntaxException {
		this.executor = executor;
		clientsArray = clientArray;
		clientsMap = new HashMap<K, V>();
		for (K clientKey : clientKeys)
			clientsMap.put(clientKey, newClient(clientKey, msTimeOut));
		clientsMap.values().toArray(clientsArray);
	}

	/**
	 * Create a new single client
	 *
	 * @param clientKey the key value of the single client
	 * @param msTimeOut the default time out
	 * @return a new JsonClient
	 * @throws URISyntaxException if any error occurs
	 */
	protected abstract V newClient(K clientKey, Integer msTimeOut) throws URISyntaxException;

	@Override
	public Iterator<V> iterator() {
		return new RandomArrayIterator<V>(clientsArray);
	}

	/**
	 * Fill a collection with the URLs of the clients
	 *
	 * @param clientKeyCollection The collection to fill
	 */
	public void fillClientUrls(Collection<K> clientKeyCollection) {
		clientKeyCollection.addAll(clientsMap.keySet());
	}

	/**
	 * @return the number of clients
	 */
	public int size() {
		return clientsArray.length;
	}

	/**
	 * @param url the URL of the client
	 * @return the client which handle this URL
	 */
	public V getClientByUrl(String url) {
		return clientsMap.get(url.intern());
	}

	/**
	 * @param pos the position of the client
	 * @return a json client
	 */
	protected V getClientByPos(Integer pos) {
		return clientsArray[pos];
	}

}
