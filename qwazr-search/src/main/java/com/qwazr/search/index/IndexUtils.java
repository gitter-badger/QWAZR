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
 **/
package com.qwazr.search.index;

import com.datastax.driver.core.utils.UUIDs;
import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.search.analysis.AnalyzerContext;
import com.qwazr.search.field.FieldDefinition;
import com.qwazr.search.field.FieldUtils;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.server.ServerException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.Similarity;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class IndexUtils {

	private final static Document newLuceneDocument(final AnalyzerContext context, final Map<String, Object> document)
			throws IOException {
		final Document doc = new Document();
		for (Map.Entry<String, Object> field : document.entrySet()) {
			final String fieldName = field.getKey();
			if (FieldDefinition.ID_FIELD.equals(fieldName))
				continue;
			FieldDefinition fieldDef = context.fields == null ? null : context.fields.get(fieldName);
			if (fieldDef == null)
				throw new IOException("No field definition for the field: " + fieldName);
			Object fieldValue = field.getValue();
			if (fieldValue == null)
				continue;
			FieldUtils.newLuceneField(fieldDef, fieldName, fieldValue, doc);
		}
		return doc;
	}

	static final Object addNewLuceneDocument(final AnalyzerContext context, final Map<String, Object> document,
			IndexWriter indexWriter) throws IOException {
		final Document doc = newLuceneDocument(context, document);
		Object id = document.get(FieldDefinition.ID_FIELD);
		if (id == null)
			id = UUIDs.timeBased();
		final String id_string = id.toString();
		doc.add(new StringField(FieldDefinition.ID_FIELD, id_string, Field.Store.NO));
		final Term termId = new Term(FieldDefinition.ID_FIELD, id_string);

		final Document facetedDoc = context.facetsConfig.build(doc);
		if (termId == null)
			indexWriter.addDocument(facetedDoc);
		else
			indexWriter.updateDocument(termId, facetedDoc);
		return id;
	}

	private static final Field[] newFieldList(final AnalyzerContext context, final Map<String, Object> document)
			throws IOException {
		final Field[] fields = new Field[document.size() - 1];
		final AtomicInteger i = new AtomicInteger();
		fieldCollection(context, document, new Consumer<Field>() {
			@Override
			public void accept(Field field) {
				fields[i.getAndIncrement()] = field;
			}
		});
		return fields;
	}

	static final void updateDocValues(final AnalyzerContext context, final Map<String, Object> document,
			IndexWriter indexWriter) throws ServerException, IOException {
		Object id = document.get(FieldDefinition.ID_FIELD);
		if (id == null)
			throw new ServerException(Response.Status.BAD_REQUEST,
					"The field " + FieldDefinition.ID_FIELD + " is missing");
		final Term termId = new Term(FieldDefinition.ID_FIELD, id.toString());
		indexWriter.updateDocValues(termId, newFieldList(context, document));
	}

	final static String[] similarityClassPrefixes = { "", "com.qwazr.search.similarity.",
			"org.apache.lucene.search.similarities." };

	final static Similarity findSimilarity(String similarity)
			throws InterruptedException, ReflectiveOperationException, IOException {
		return (Similarity) ClassLoaderUtils
				.findClass(ClassLoaderManager.classLoader, similarity, similarityClassPrefixes).newInstance();
	}

}
