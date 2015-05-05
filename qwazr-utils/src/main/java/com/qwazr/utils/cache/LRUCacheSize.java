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
package com.qwazr.utils.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class LRUCacheSize<K extends AbstractLRUCacheItem<K>> extends
		AbstractLRUCache<K> {

	private class EvictionQueue extends LinkedHashMap<K, K> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2876891913920705107L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, K> eldest) {
			if (size() <= maxSize)
				return false;
			cacheMap.remove(eldest.getKey());
			evictions.incrementAndGet();
			return true;
		}
	}

	private int maxSize;

	public LRUCacheSize(int maxSize) {
		this.maxSize = maxSize;
		setMaxSize_noLock(maxSize);
	}

	private void setMaxSize_noLock(int newMaxSize) {
		if (newMaxSize == maxSize)
			return;
		if (newMaxSize == 0) {
			clear_nolock();
			cacheMap = null;
			evictionQueue = null;
		} else {
			if (newMaxSize < maxSize) {
				cacheMap = null;
				evictionQueue = null;
			}
			if (cacheMap == null)
				cacheMap = new TreeMap<K, K>();
			if (evictionQueue == null)
				evictionQueue = new EvictionQueue();
		}
		maxSize = newMaxSize;
	}

	public void setMaxSize(int newMaxSize) {
		rwl.w.lock();
		try {
			setMaxSize_noLock(newMaxSize);
		} finally {
			rwl.w.unlock();
		}
	}

	final public int getMaxSize() {
		rwl.r.lock();
		try {
			return maxSize;
		} finally {
			rwl.r.unlock();
		}
	}
}
