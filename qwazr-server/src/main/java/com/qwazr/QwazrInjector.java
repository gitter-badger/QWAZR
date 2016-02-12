/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr;

import com.qwazr.annotations.Manager;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.library.LibraryManager;
import com.qwazr.library.annotations.Library;
import com.qwazr.utils.AnnotationsUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;

class QwazrInjector implements Consumer<Field> {

	private final Object object;

	QwazrInjector(Object object) {
		this.object = object;
	}

	@Override
	public void accept(Field field) {
		Manager manager = field.getAnnotation(Manager.class);
		if (manager == null)
			return;

		field.setAccessible(true);
		try {
			field.set(object, null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
