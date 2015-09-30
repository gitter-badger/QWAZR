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
import java.util.Set;

/**
 * Represents a row from a ResultSet. A convenient way to retrieve data from
 * ResultSet if you don't want to use POJO. *
 */
public class Row extends AbstractMap<String, Object> {

    private final RowSet rowSet;

    Row(RowSet rowSet) {
	this.rowSet = rowSet;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
	return rowSet;
    }

    /**
     * @param columnNumber
     *            the number of the column
     * @return the value for the give column
     */
    final public Object get(int columnNumber) {
	Object col = rowSet.columns[columnNumber];
	if (col == null)
	    return null;
	return col;
    }

    /**
     * @param label
     *            the label of the column
     * @return the value for the given column label
     */
    final public Object get(Object label) {
	Integer colNumber = rowSet.columnMap.get(label.toString());
	if (colNumber == null)
	    return null;
	if (colNumber > rowSet.columns.length)
	    return null;
	return rowSet.columns[colNumber];
    }

    @Override
    public boolean containsValue(Object value) {
	if (rowSet.columns == null)
	    return false;
	for (Object col : rowSet.columns)
	    if (col == value)
		return true;
	return false;
    }

}
