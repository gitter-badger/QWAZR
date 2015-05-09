/**
 * Copyright 2014-2015 OpenSearchServer Inc.
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
package com.qwazr.utils.json.client;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;

import com.qwazr.utils.ExceptionUtils.ExceptionHolder;

public abstract class JsonMultiClientAbstract<K, V> implements Iterable<V> {

	private final V[] clientsArray;
	private final HashMap<K, V> clientsMap;
	protected final ExecutorService executor;

	protected JsonMultiClientAbstract(ExecutorService executor,
			V[] clientArray, K[] clientKeys, int msTimeOut)
			throws URISyntaxException {
		this.executor = executor;
		clientsArray = clientArray;
		clientsMap = new HashMap<K, V>();
		for (K clientKey : clientKeys)
			clientsMap.put(clientKey, newClient(clientKey, msTimeOut));
		clientsMap.values().toArray(clientsArray);
	}

	/**
	 * Create a new client
	 * 
	 * @param url
	 *            the destination URL
	 * @param msTimeOut
	 *            the default time out
	 * @return a new JsonClient
	 * @throws URISyntaxException
	 *             if any error occurs
	 */
	protected abstract V newClient(K clientKey, int msTimeOut)
			throws URISyntaxException;

	@Override
	public Iterator<V> iterator() {
		return new JsonClientIterator();
	}

	/**
	 * Fill a collection with the URLs of the clients
	 * 
	 * @param urlCollection
	 *            The collection to fill
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
	 * @param url
	 *            the URL of the client
	 * @return the client which handle this URL
	 */
	public V getClientByUrl(String url) {
		return clientsMap.get(url.intern());
	}

	private class JsonClientIterator implements Iterator<V> {

		private int count = clientsArray.length;
		private int pos = RandomUtils.nextInt(0, clientsArray.length);

		@Override
		public boolean hasNext() {
			return count > 0;
		}

		@Override
		public V next() {
			V client = clientsArray[pos++];
			if (pos == clientsArray.length)
				pos = 0;
			count--;
			return client;
		}

		@Override
		public void remove() {
			throw new RuntimeException("Not available");
		}

	}

	public class WebAppExceptionHolder extends
			ExceptionHolder<WebApplicationException> {

		public WebAppExceptionHolder(Logger logger) {
			super(logger);
		}
	}
}
