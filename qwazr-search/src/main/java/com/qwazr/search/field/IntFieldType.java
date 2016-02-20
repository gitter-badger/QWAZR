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
 */
package com.qwazr.search.field;

import com.qwazr.search.index.QueryDefinition;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.search.SortField;

import java.util.Collection;

class IntFieldType extends StorableFieldType {

	IntFieldType(FieldDefinition fieldDefinition) {
		super(fieldDefinition);
	}

	@Override
	final public void fillDocument(final String fieldName, final Object value, Document doc) {
		if (value instanceof Collection)
			addCollection(fieldName, (Collection) value, doc);
		else if (value instanceof Number)
			doc.add(new IntField(fieldName, ((Number) value).intValue(), store));
		else
			doc.add(new IntField(fieldName, Integer.parseInt(value.toString()), store));
	}

	private final void addCollection(String fieldName, Collection<Object> values, Document doc) {
		for (Object value : values)
			doc.add(new IntField(fieldName, (int) value, store));
	}

	@Override
	public final SortField getSortField(String fieldName, QueryDefinition.SortEnum sortEnum) {
		final SortField sortField = new SortField(fieldName, SortField.Type.INT, FieldUtils.sortReverse(sortEnum));
		FieldUtils.sortIntMissingValue(sortEnum, sortField);
		return sortField;
	}

}
