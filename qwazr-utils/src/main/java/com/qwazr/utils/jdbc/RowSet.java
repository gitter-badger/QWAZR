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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

class RowSet implements Set<Map.Entry<String, Object>> {

    final Map<String, Integer> columnMap;

    final Object[] columns;

    RowSet(Map<String, Integer> columnMap, int columnCount) {
	this.columnMap = columnMap;
	this.columns = new Object[columnCount];
    }

    RowSet(Map<String, Integer> columnMap, int columnCount, ResultSet rs) throws SQLException {
	this(columnMap, columnCount);
	for (Map.Entry<String, Integer> entry : columnMap.entrySet()) {
	    int columnIndex = entry.getValue();
	    Object object = rs.getObject(columnIndex + 1);
	    columns[columnIndex] = object;
	}
    }

    @Override
    public int size() {
	return columns.length;
    }

    @Override
    public boolean isEmpty() {
	return columns.length == 0;
    }

    @Override
    public boolean contains(Object o) {
	if (o == null)
	    return false;
	if (!(o instanceof Map.Entry))
	    return false;
	for (Map.Entry<String, Object> entry : this)
	    if (entry.equals(o))
		return true;
	return false;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
	return new RowIterator(columnMap.entrySet().iterator(), columns);
    }

    @Override
    public Object[] toArray() {
	return toArray(new Map.Entry[columns.length]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
	Map.Entry<String, Object>[] entries = (Map.Entry<String, Object>[]) a;
	int i = 0;
	for (Map.Entry<String, Object> entry : this)
	    entries[i++] = entry;
	return (T[]) entries;
    }

    @Override
    public boolean add(Map.Entry<String, Object> stringObjectEntry) {
	throw new RuntimeException("Immutable collection");
    }

    @Override
    public boolean remove(Object o) {
	throw new RuntimeException("Immutable collection");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
	for (Object o : c)
	    if (!contains(c))
		return false;
	return true;
    }

    @Override
    public boolean addAll(Collection<? extends Map.Entry<String, Object>> c) {
	throw new RuntimeException("Immutable collection");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
	throw new RuntimeException("Immutable collection");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
	throw new RuntimeException("Immutable collection");
    }

    @Override
    public void clear() {
	throw new RuntimeException("Immutable collection");
    }
}
