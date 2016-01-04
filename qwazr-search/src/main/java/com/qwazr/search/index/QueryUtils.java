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
 **/
package com.qwazr.search.index;

import com.qwazr.search.analysis.AnalyzerContext;
import com.qwazr.search.analysis.UpdatableAnalyzer;
import com.qwazr.search.field.FieldDefinition;
import com.qwazr.search.field.FieldUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.TimeTracker;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class QueryUtils {

	final static SortField buildSortField(Map<String, FieldDefinition> fields, String field,
			QueryDefinition.SortEnum sortEnum) throws ServerException {

		final boolean reverse;
		final Object missingValue;
		switch (sortEnum) {
		case ascending:
			missingValue = null;
			reverse = false;
			break;
		case ascending_missing_first:
			missingValue = SortField.STRING_LAST;
			reverse = false;
			break;
		case ascending_missing_last:
			missingValue = SortField.STRING_FIRST;
			reverse = false;
			break;
		case descending:
			missingValue = null;
			reverse = true;
			break;
		case descending_missing_first:
			missingValue = SortField.STRING_LAST;
			reverse = true;
			break;
		case descending_missing_last:
			missingValue = SortField.STRING_FIRST;
			reverse = true;
			break;
		default:
			missingValue = null;
			reverse = false;
			break;
		}

		final SortField sortField;

		if ("$score".equals(field)) {
			sortField = new SortField(null, SortField.Type.SCORE, !reverse);
		} else if ("$doc".equals(field)) {
			sortField = new SortField(null, SortField.Type.DOC, reverse);
		} else {
			FieldDefinition fieldDefinition = fields.get(field);
			if (fieldDefinition == null)
				throw new ServerException(Response.Status.BAD_REQUEST, "Unknown sort field: " + field);
			sortField = FieldUtils.getSortField(fieldDefinition, field, reverse);
		}
		if (missingValue != null)
			sortField.setMissingValue(missingValue);
		return sortField;
	}

	final static Sort buildSort(Map<String, FieldDefinition> fields,
			LinkedHashMap<String, QueryDefinition.SortEnum> sorts) throws ServerException {
		if (sorts.isEmpty())
			return null;
		final SortField[] sortFields = new SortField[sorts.size()];
		int i = 0;
		for (Map.Entry<String, QueryDefinition.SortEnum> sort : sorts.entrySet())
			sortFields[i++] = buildSortField(fields, sort.getKey(), sort.getValue());
		if (sortFields.length == 1)
			return new Sort(sortFields[0]);
		return new Sort(sortFields);
	}

	final static String getFinalQueryString(QueryDefinition queryDef) {
		// Deal wih query string
		final String qs;
		// Check if we have to escape some characters
		if (queryDef.escape_query != null && queryDef.escape_query) {
			if (queryDef.escaped_chars != null && queryDef.escaped_chars.length > 0)
				qs = StringUtils.escape_chars(queryDef.query_string, queryDef.escaped_chars);
			else
				qs = QueryParser.escape(queryDef.query_string);
		} else
			qs = queryDef.query_string;
		return qs;
	}

	final static Query getLuceneQuery(QueryContext queryContext)
			throws QueryNodeException, ParseException, IOException, ReflectiveOperationException {

		Query query = queryContext.queryDefinition.query == null ?
				new MatchAllDocsQuery() :
				queryContext.queryDefinition.query.getBoostedQuery(queryContext);

		return query;
	}

	static private Pair<TotalHitCountCollector, TopDocsCollector> getCollectorPair(List<Collector> collectors,
			boolean bNeedScore, int numHits, Sort sort) throws IOException {
		final TotalHitCountCollector totalHitCollector;
		final TopDocsCollector topDocsCollector;
		if (numHits == 0) {
			totalHitCollector = new TotalHitCountCollector();
			topDocsCollector = null;
			collectors.add(0, totalHitCollector);
		} else {
			if (sort != null)
				topDocsCollector = TopFieldCollector.create(sort, numHits, true, bNeedScore, bNeedScore);
			else
				topDocsCollector = TopScoreDocCollector.create(numHits);
			totalHitCollector = null;
			collectors.add(0, topDocsCollector);
		}
		return Pair.of(totalHitCollector, topDocsCollector);
	}

	final static ResultDefinition search(final QueryContext queryContext)
			throws ServerException, IOException, QueryNodeException, InterruptedException, ParseException,
			ReflectiveOperationException {

		final QueryDefinition queryDef = queryContext.queryDefinition;

		Query query = getLuceneQuery(queryContext);

		final IndexSearcher indexSearcher = queryContext.indexSearcher;
		final IndexReader indexReader = indexSearcher.getIndexReader();
		final TimeTracker timeTracker = new TimeTracker();

		final AnalyzerContext analyzerContext = queryContext.analyzer.getContext();
		final Sort sort = queryDef.sorts == null ? null : buildSort(analyzerContext.fields, queryDef.sorts);
		final Facets facets;
		final SortedSetDocValuesReaderState facetState;

		final int numHits = queryDef.getEnd();
		final boolean bNeedScore = sort != null ? sort.needsScores() : true;

		final QueryCollectors queryCollectors = new QueryCollectors(bNeedScore, sort, numHits, queryDef.facets,
				queryDef.functions, analyzerContext.fields);

		indexSearcher.search(query, queryCollectors.finalCollector);
		final TopDocs topDocs = queryCollectors.getTopDocs();
		final Integer totalHits = queryCollectors.getTotalHits();

		timeTracker.next("search_query");

		if (queryCollectors.facetsCollector != null) {
			facetState = new DefaultSortedSetDocValuesReaderState(indexReader);
			facets = new SortedSetDocValuesFacetCounts(facetState, queryCollectors.facetsCollector);
			timeTracker.next("facet_count");
		} else {
			facetState = null;
			facets = null;
		}

		Map<String, String[]> postingsHighlightersMap = null;
		if (queryDef.postings_highlighter != null && topDocs != null) {
			postingsHighlightersMap = new LinkedHashMap<>();
			for (Map.Entry<String, Integer> entry : queryDef.postings_highlighter.entrySet()) {
				String field = entry.getKey();
				PostingsHighlighter highlighter = new PostingsHighlighter(entry.getValue());
				String highlights[] = highlighter.highlight(field, query, indexSearcher, topDocs);
				if (highlights != null) {
					postingsHighlightersMap.put(field, highlights);
				}
			}
			timeTracker.next("postings_highlighters");
		}

		return new ResultDefinition(analyzerContext.fields, timeTracker, indexSearcher, totalHits, topDocs, queryDef,
				facetState, facets, postingsHighlightersMap, queryCollectors.functionsCollectors, query);
	}

}
