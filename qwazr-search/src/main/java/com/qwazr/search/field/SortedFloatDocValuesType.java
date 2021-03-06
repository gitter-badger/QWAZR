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

import com.qwazr.search.index.FieldConsumer;
import com.qwazr.search.index.QueryDefinition;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;

class SortedFloatDocValuesType extends FieldTypeAbstract {

	SortedFloatDocValuesType(final String fieldName, final FieldDefinition fieldDef) {
		super(fieldName, fieldDef);
	}

	@Override
	final public void fillValue(final Object value, final FieldConsumer consumer) {
		if (value instanceof Number)
			consumer.accept(new SortedNumericDocValuesField(fieldName,
					NumericUtils.floatToSortableInt(((Number) value).floatValue())));
		else
			consumer.accept(new SortedNumericDocValuesField(fieldName,
					NumericUtils.floatToSortableInt(Float.parseFloat(value.toString()))));
	}

	@Override
	final public SortField getSortField(final QueryDefinition.SortEnum sortEnum) {
		final SortField sortField = new SortedNumericSortField(fieldName, SortField.Type.FLOAT,
				SortUtils.sortReverse(sortEnum));
		SortUtils.sortFloatMissingValue(sortEnum, sortField);
		return sortField;
	}

	@Override
	final public ValueConverter getConverter(final LeafReader reader) throws IOException {
		SortedNumericDocValues docValues = reader.getSortedNumericDocValues(fieldName);
		if (docValues == null)
			return super.getConverter(reader);
		return new ValueConverter.FloatSetDVConverter(docValues);
	}

}
