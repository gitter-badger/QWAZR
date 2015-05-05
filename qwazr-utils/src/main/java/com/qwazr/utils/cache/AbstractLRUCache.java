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
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.qwazr.utils.LockUtils;

public abstract class AbstractLRUCache<K extends AbstractLRUCacheItem<K>> {

	final protected LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	protected TreeMap<K, K> cacheMap;
	protected LinkedHashMap<K, K> evictionQueue;
	protected AtomicLong evictions;
	private AtomicLong lookups;
	private AtomicLong hits;
	private AtomicLong inserts;

	protected AbstractLRUCache() {
		this.evictions = new AtomicLong(0);
		this.lookups = new AtomicLong(0);
		this.inserts = new AtomicLong(0);
		this.hits = new AtomicLong(0);
		this.cacheMap = null;
		this.evictionQueue = null;
	}

	final protected K getAndPromote(final K newItem) {
		rwl.w.lock();
		try {
			if (cacheMap == null)
				return newItem;
			lookups.incrementAndGet();
			K prevItem = cacheMap.get(newItem);
			if (prevItem != null) {
				evictionQueue.remove(prevItem);
				evictionQueue.put(prevItem, prevItem);
				hits.incrementAndGet();
				return prevItem;
			}
			evictionQueue.put(newItem, newItem);
			cacheMap.put(newItem, newItem);
			inserts.incrementAndGet();
			return newItem;
		} finally {
			rwl.w.unlock();
		}
	}

	final public void put(final K item) {
		rwl.w.lock();
		try {
			if (cacheMap == null)
				return;
			evictionQueue.put(item, item);
			cacheMap.put(item, item);
			inserts.incrementAndGet();
		} finally {
			rwl.w.unlock();
		}
	}

	final public boolean remove(final K key) {
		rwl.w.lock();
		try {
			if (cacheMap == null)
				return false;
			K item1 = cacheMap.remove(key);
			K item2 = evictionQueue.remove(key);
			if (item1 == null && item2 == null)
				return false;
			evictions.incrementAndGet();
			return true;
		} finally {
			rwl.w.unlock();
		}
	}

	public K getAndJoin(K item) throws Exception {
		item = getAndPromote(item);
		item.join();
		return item;
	}

	final protected void clear_nolock() {
		if (cacheMap != null)
			cacheMap.clear();
		if (evictionQueue != null)
			evictionQueue.clear();
	}

	final public void clear() {
		rwl.w.lock();
		try {
			clear_nolock();
		} finally {
			rwl.w.unlock();
		}
	}

	final public int getSize() {
		rwl.r.lock();
		try {
			return cacheMap == null ? 0 : cacheMap.size();
		} finally {
			rwl.r.unlock();
		}
	}

	final public long getEvictions() {
		return evictions.get();
	}

	final public long getLookups() {
		return lookups.get();
	}

	final public long getHits() {
		return hits.get();
	}

	public long getInserts() {
		return inserts.get();
	}

	public float getHitRatio() {
		float h = hits.get();
		float l = lookups.get();
		if (h == 0 || l == 0)
			return 0;
		return h / l;
	}

}
