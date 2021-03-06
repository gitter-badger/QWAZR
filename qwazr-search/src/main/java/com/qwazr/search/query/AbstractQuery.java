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

package com.qwazr.search.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.qwazr.search.index.QueryContext;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.Query;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "query")
@JsonSubTypes({ @JsonSubTypes.Type(value = BooleanQuery.class), @JsonSubTypes.Type(value = BoostedQuery.class),
		@JsonSubTypes.Type(value = BoostQuery.class), @JsonSubTypes.Type(value = ConstantScoreQuery.class),
		@JsonSubTypes.Type(value = CustomScoreQuery.class), @JsonSubTypes.Type(value = DisjunctionMaxQuery.class),
		@JsonSubTypes.Type(value = DoubleRangeQuery.class), @JsonSubTypes.Type(value = DrillDownQuery.class),
		@JsonSubTypes.Type(value = FacetPathQuery.class), @JsonSubTypes.Type(value = FloatRangeQuery.class),
		@JsonSubTypes.Type(value = FunctionQuery.class), @JsonSubTypes.Type(value = FuzzyQuery.class),
		@JsonSubTypes.Type(value = IntRangeQuery.class), @JsonSubTypes.Type(value = LongRangeQuery.class),
		@JsonSubTypes.Type(value = MatchAllDocsQuery.class), @JsonSubTypes.Type(value = MatchNoDocsQuery.class),
		@JsonSubTypes.Type(value = MoreLikeThisQuery.class), @JsonSubTypes.Type(value = MultiFieldQueryParser.class),
		@JsonSubTypes.Type(value = PhraseQuery.class), @JsonSubTypes.Type(value = PrefixQuery.class),
		@JsonSubTypes.Type(value = RegexpQuery.class), @JsonSubTypes.Type(value = SpanFirstQuery.class),
		@JsonSubTypes.Type(value = SpanNearQuery.class), @JsonSubTypes.Type(value = SpanNotQuery.class),
		@JsonSubTypes.Type(value = SpanPositionsQuery.class), @JsonSubTypes.Type(value = SpanTermQuery.class),
		@JsonSubTypes.Type(value = StandardQueryParser.class), @JsonSubTypes.Type(value = TermQuery.class),
		@JsonSubTypes.Type(value = TermRangeQuery.class), @JsonSubTypes.Type(value = TermsQuery.class) })
public abstract class AbstractQuery {

	@JsonIgnore
	public abstract Query getQuery(QueryContext queryContext)
			throws IOException, ParseException, QueryNodeException, ReflectiveOperationException;

}
