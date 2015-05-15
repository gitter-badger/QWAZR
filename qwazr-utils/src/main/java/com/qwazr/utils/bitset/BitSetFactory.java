/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.utils.bitset;

import java.util.Random;

import com.qwazr.utils.NativeUtils;
import com.qwazr.utils.array.IntBufferedArrayFactory;

public abstract class BitSetFactory {

	public final static BitSetFactory INSTANCE = NativeUtils.loaded() ? new NativeFactory()
			: new JavaFactory();

	public abstract BitSetInterface newInstance(final long numbits);

	public abstract BitSetInterface newInstance(final int numbits);

	final static private class NativeFactory extends BitSetFactory {

		@Override
		public BitSetInterface newInstance(long numbits) {
			return new NativeBitSet(numbits);
		}

		@Override
		public BitSetInterface newInstance(int numbits) {
			return new NativeBitSet(numbits);
		}

	}

	final static private class JavaFactory extends BitSetFactory {

		@Override
		public BitSetInterface newInstance(long numbits) {
			return new JavaBitSet(numbits);
		}

		@Override
		public BitSetInterface newInstance(int numbits) {
			return new JavaBitSet(numbits);
		}

	}

	private final static void test(BitSetFactory bitSetFactory,
			int[] randomArray1, int[] randomArray2) {
		System.gc();
		long startTime = System.currentTimeMillis();
		long freemem = Runtime.getRuntime().freeMemory();
		BitSetInterface bitSet1 = bitSetFactory
				.newInstance(randomArray1.length * 4);
		BitSetInterface bitSet2 = bitSetFactory
				.newInstance(randomArray2.length * 4);
		for (int v : randomArray1)
			bitSet1.set(v);
		for (int v : randomArray2)
			bitSet2.set(v);
		bitSet1.cardinality();
		bitSet1.and(bitSet2);
		System.out.println(bitSet2.cardinality());
		IntBufferedArrayFactory.result(bitSet1, startTime, freemem);

	}

	public final static void main(String[] str) {
		final int size = 10000000;

		Random random = new Random(System.currentTimeMillis());
		// Building the index
		long startTime = System.currentTimeMillis();
		long freemem = Runtime.getRuntime().freeMemory();
		int[] randomArray1 = new int[size];
		for (int i = 0; i < size; i++)
			randomArray1[i++] = random.nextInt(size * 4);
		int[] randomArray2 = new int[size];
		for (int i = 0; i < size; i++)
			randomArray2[i++] = random.nextInt(size * 4);
		IntBufferedArrayFactory.result(randomArray1, startTime, freemem);

		test(new JavaFactory(), randomArray1, randomArray2);
		test(new NativeFactory(), randomArray1, randomArray2);
	}
}
