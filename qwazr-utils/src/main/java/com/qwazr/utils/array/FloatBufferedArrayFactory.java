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
package com.qwazr.utils.array;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.util.Random;

import com.qwazr.utils.NativeUtils;

public abstract class FloatBufferedArrayFactory {

	public final static FloatBufferedArrayFactory INSTANCE = NativeUtils
			.loaded() ? new NativeFactory() : new JavaFactory();

	public abstract FloatBufferedArrayInterface newInstance(final long maxSize);

	final static private class NativeFactory extends FloatBufferedArrayFactory {

		@Override
		public FloatBufferedArrayInterface newInstance(final long maxSize) {
			return new NativeFloatBufferedArray(maxSize);
		}
	}

	final static private class JavaFactory extends FloatBufferedArrayFactory {

		@Override
		public FloatBufferedArrayInterface newInstance(final long maxSize) {
			return new FloatBufferedArray(maxSize);
		}
	}

	public final static void main(String[] str) {
		final int size = 1000000;

		Random random = new Random(System.currentTimeMillis());
		// Building the index
		long startTime = System.currentTimeMillis();
		long freemem = Runtime.getRuntime().freeMemory();
		float[] randomArray = new float[size];
		for (int i = 0; i < size; i++)
			randomArray[i++] = random.nextFloat();
		IntBufferedArrayFactory.result(randomArray, startTime, freemem);

		// Testing Native Array reduced size
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		float[] nativeArray1 = new float[size];
		int i = 0;
		for (float v : randomArray)
			nativeArray1[i++] = v;
		IntBufferedArrayFactory.result(nativeArray1, startTime, freemem);
		check(randomArray, nativeArray1);
		IntBufferedArrayFactory.result(nativeArray1, startTime, freemem);

		// Testing Native Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		float[] nativeArray = new float[size * 4];
		i = 0;
		for (float v : randomArray)
			nativeArray[i++] = v;
		IntBufferedArrayFactory.result(nativeArray, startTime, freemem);
		check(randomArray, nativeArray);
		IntBufferedArrayFactory.result(nativeArray, startTime, freemem);

		// Testing FastUTIL
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		FloatArrayList fastUtilArray = new FloatArrayList(size * 4);
		for (float v : randomArray)
			fastUtilArray.add(v);
		IntBufferedArrayFactory.result(fastUtilArray, startTime, freemem);
		check(randomArray, fastUtilArray.toFloatArray());
		IntBufferedArrayFactory.result(fastUtilArray, startTime, freemem);

		// Testing Buffered Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		FloatBufferedArray floatBufferedArray = new FloatBufferedArray(size * 4);
		for (float v : randomArray)
			floatBufferedArray.add(v);
		IntBufferedArrayFactory.result(floatBufferedArray, startTime, freemem);
		check(randomArray, floatBufferedArray.getFinalArray());
		IntBufferedArrayFactory.result(floatBufferedArray, startTime, freemem);

		// Testing Native Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		FloatBufferedArrayInterface ibai = INSTANCE.newInstance(size * 4);
		for (float v : randomArray)
			ibai.add(v);
		IntBufferedArrayFactory.result(ibai, startTime, freemem);
		check(randomArray, ibai.getFinalArray());
		IntBufferedArrayFactory.result(ibai, startTime, freemem);
	}

	private static void check(float[] randomArray, float[] finalArray) {
		if (randomArray.length > finalArray.length) {
			System.err.println("BufferedArray corrupted (size)");
			return;
		}
		int pos = 0;
		for (float value : randomArray)
			if (finalArray[pos++] != value) {
				System.err.println("BufferedArray corrupted (content) " + pos);
				return;
			}
		System.out.println("BufferedArray ok");
	}
}
