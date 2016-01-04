/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCacheExpirable<K extends AbstractLRUCacheItem<K>> extends
		AbstractLRUCache<K> {

	private class EvictionQueue extends LinkedHashMap<K, K> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8430267687758426426L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, K> eldest) {
			if (!eldest.getValue().expired())
				return false;
			cacheMap.remove(eldest.getKey());
			evictions.incrementAndGet();
			return true;
		}
	}

	public LRUCacheExpirable() {
		evictionQueue = new EvictionQueue();
	}

}
