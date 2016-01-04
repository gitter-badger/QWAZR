/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Random;

import com.qwazr.utils.NativeUtils;

public abstract class IntBufferedArrayFactory {

	public final static IntBufferedArrayFactory INSTANCE = NativeUtils.loaded() ? new NativeFactory()
			: new JavaFactory();

	public abstract IntBufferedArrayInterface newInstance(final long maxSize);

	final static private class NativeFactory extends IntBufferedArrayFactory {

		@Override
		public IntBufferedArrayInterface newInstance(final long maxSize) {
			return new NativeIntBufferedArray(maxSize);
		}
	}

	final static private class JavaFactory extends IntBufferedArrayFactory {

		@Override
		public IntBufferedArrayInterface newInstance(final long maxSize) {
			return new IntBufferedArray(maxSize);
		}
	}

	public static final void result(Object object, long startTime, long freemem) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		System.out.println(object.getClass().getSimpleName() + " Time: "
				+ elapsedTime + " Memory: "
				+ (freemem - Runtime.getRuntime().freeMemory()) / 1024);
	}

	public final static void main(String[] str) {
		final int size = 10000000;

		Random random = new Random(System.currentTimeMillis());
		// Building the index
		long startTime = System.currentTimeMillis();
		long freemem = Runtime.getRuntime().freeMemory();
		int[] randomArray = new int[size];
		for (int i = 0; i < size; i++)
			randomArray[i++] = random.nextInt();
		result(randomArray, startTime, freemem);

		// Testing Native Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		int[] nativeArray1 = new int[size];
		int i = 0;
		for (int v : randomArray)
			nativeArray1[i++] = v;
		result(nativeArray1, startTime, freemem);

		// Testing Native Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		int[] nativeArray = new int[size * 4];
		i = 0;
		for (int v : randomArray)
			nativeArray[i++] = v;
		check(randomArray, nativeArray);
		result(nativeArray, startTime, freemem);

		// Testing FastUTIL
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		IntArrayList fastUtilArray = new IntArrayList(size * 4);
		for (int v : randomArray)
			fastUtilArray.add(v);
		check(randomArray, fastUtilArray.toIntArray());
		result(fastUtilArray, startTime, freemem);

		// Testing Buffered Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		IntBufferedArray intBufferedArray = new IntBufferedArray(size * 4);
		for (int v : randomArray)
			intBufferedArray.add(v);
		result(intBufferedArray, startTime, freemem);
		check(randomArray, intBufferedArray.getFinalArray());
		result(intBufferedArray, startTime, freemem);

		// Testing Native Array
		System.gc();
		startTime = System.currentTimeMillis();
		freemem = Runtime.getRuntime().freeMemory();
		IntBufferedArrayInterface ibai = INSTANCE.newInstance(size * 4);
		for (int v : randomArray)
			ibai.add(v);
		result(ibai, startTime, freemem);
		check(randomArray, ibai.getFinalArray());
		result(ibai, startTime, freemem);
	}

	private static void check(int[] randomArray, int[] finalArray) {
		if (randomArray.length > finalArray.length) {
			System.err.println("BufferedArray corrupted (size)");
			return;
		}
		int pos = 0;
		for (int value : randomArray)
			if (finalArray[pos++] != value) {
				System.err.println("BufferedArray corrupted (content) " + pos);
				return;
			}
		System.out.println("BufferedArray ok");
	}
}
