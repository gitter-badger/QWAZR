/**
 * Copyright 2015 Emmanuel Keller / QWAZR
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
package com.qwazr.search.query;

import com.qwazr.search.analysis.UpdatableAnalyzer;
import com.qwazr.search.source.AbstractValueSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.Objects;

public class BoostedQuery extends AbstractQuery {

	final public AbstractQuery sub_query;
	final public AbstractValueSource value_source;

	public BoostedQuery() {
		super(null);
		sub_query = null;
		value_source = null;
	}

	BoostedQuery(Float boost, AbstractQuery sub_query, AbstractValueSource value_source) {
		super(boost);
		this.sub_query = sub_query;
		this.value_source = value_source;
	}

	@Override
	final protected Query getQuery(UpdatableAnalyzer analyzer, String queryString)
			throws IOException, ParseException, QueryNodeException {
		Objects.requireNonNull(sub_query, "The sub_query property is missing");
		Objects.requireNonNull(value_source, "The vaelue_source property is missing");
		return new org.apache.lucene.queries.function.BoostedQuery(sub_query.getQuery(analyzer, queryString),
				value_source.getValueSource());
	}
}