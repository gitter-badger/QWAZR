/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "NativeIntBufferedArray.h"
#include <boost/fusion/container/list.hpp>
#include <boost/fusion/include/list.hpp>
#include <iostream>

class array_node {

  jint *buffer;
  int size;
  array_node *next;

public:

  array_node(array_node *parent, jint* integers, int length) {
    int len = sizeof(jint) * length;
    buffer = (jint*) malloc(len);
    memcpy(buffer, integers, len);
    size = length;
    next = 0;
    if (parent != 0)
      parent->next = this;
  }

  void populate(jint *integers) {
    int len = sizeof(jint) * size;
    memcpy(integers, buffer, len);
    integers += size;
    if (next != 0)
      next->populate(integers);
  }

  virtual ~array_node() {
    if (buffer != 0) {
      free(buffer);
      buffer = 0;
    }
    if (next != 0) {
      delete next;
      next = 0;
    }
  }

};


class int_buffered_array {

  array_node *first;
  array_node *current;

public:

  long total_size;

  int_buffered_array() {
    total_size = 0;
    first = 0;
    current = 0;
  }

  void add(jint* jints, int length) {
    current = new array_node(current, jints, length);
    if (first == 0)
      first = current;
    total_size += length;
  }

  void populate(jint *integers) {
    if (first != 0)
      first->populate(integers);
  }

  virtual ~int_buffered_array() {
    if (first != 0) {
      delete first;
      first = 0;
    }
  }

};


JNIEXPORT jlong JNICALL Java_com_jaeksoft_searchlib_util_array_NativeIntBufferedArray_init
(JNIEnv *, jobject, jlong maxSize) {
  int_buffered_array *iba  = new int_buffered_array();
  return (jlong) iba;
}

JNIEXPORT void JNICALL Java_com_jaeksoft_searchlib_util_array_NativeIntBufferedArray_free
(JNIEnv *, jobject, jlong ref) {
  int_buffered_array *iba = (int_buffered_array*)ref;
  if (iba != 0)
    delete iba;
}

JNIEXPORT void JNICALL Java_com_jaeksoft_searchlib_util_array_NativeIntBufferedArray_add
(JNIEnv *env, jobject, jlong ref, jintArray integers, jint length) {
  int_buffered_array *iba = (int_buffered_array*)ref;
  if (iba == 0)
    return;
  jint *jints = (jint*) env->GetPrimitiveArrayCritical(integers, 0);
  iba->add(jints, length);
  env->ReleasePrimitiveArrayCritical(integers, jints, JNI_ABORT);
}


JNIEXPORT jlong JNICALL Java_com_jaeksoft_searchlib_util_array_NativeIntBufferedArray_getSize
(JNIEnv *, jobject, jlong ref) {
  int_buffered_array *iba = (int_buffered_array*)ref;
  return iba == 0 ? 0 : iba->total_size;
}

JNIEXPORT void JNICALL Java_com_jaeksoft_searchlib_util_array_NativeIntBufferedArray_populateFinalArray
(JNIEnv *env, jobject, jlong ref, jintArray integers) {
  int_buffered_array *iba = (int_buffered_array*)ref;
  if (iba ==0)
    return;
  jint *jints = (jint*) env->GetPrimitiveArrayCritical(integers, 0);
  iba->populate(jints);
  env->ReleasePrimitiveArrayCritical(integers, jints, 0);
}
