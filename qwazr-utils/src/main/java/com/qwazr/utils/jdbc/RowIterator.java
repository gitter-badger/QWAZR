/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils.jdbc;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

class RowIterator implements Iterator<Map.Entry<String, Object>> {

    private final Iterator<Map.Entry<String, Integer>> iterator;
    private final Object[] columns;

    RowIterator(Iterator<Map.Entry<String, Integer>> iterator, Object[] columns) {
	this.iterator = iterator;
	this.columns = columns;
    }

    @Override
    public boolean hasNext() {
	return iterator.hasNext();
    }

    @Override
    public Map.Entry<String, Object> next() {
	Map.Entry<String, Integer> entry = iterator.next();
	return new AbstractMap.SimpleEntry(entry.getKey(), columns[entry.getValue()]);
    }
}
