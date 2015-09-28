/**
 * Copyright 2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils;

import java.util.Collection;

public class ArrayUtils extends org.apache.commons.lang3.ArrayUtils {

    public static boolean startsWith(final byte[] array, final byte[] prefix) {
	if (array == null)
	    return false;
	if (prefix == null)
	    return false;
	if (prefix.length > array.length)
	    return false;
	for (int i = 0; i < prefix.length; i++)
	    if (array[i] != prefix[i])
		return false;
	return true;
    }

    public static double[] toPrimitiveDouble(Collection<Double> collection) {
	double[] array = new double[collection.size()];
	int i = 0;
	for (double val : collection)
	    array[i++] = val;
	return array;
    }

    public static int[] toPrimitiveInt(Collection<Integer> collection) {
	int[] array = new int[collection.size()];
	int i = 0;
	for (int val : collection)
	    array[i++] = val;
	return array;
    }

    public static long[] toPrimitiveLong(Collection<Long> collection) {
	long[] array = new long[collection.size()];
	int i = 0;
	for (long val : collection)
	    array[i++] = val;
	return array;
    }

    public static float[] toPrimitiveFloat(Collection<Float> collection) {
	float[] array = new float[collection.size()];
	int i = 0;
	for (float val : collection)
	    array[i++] = val;
	return array;
    }

}