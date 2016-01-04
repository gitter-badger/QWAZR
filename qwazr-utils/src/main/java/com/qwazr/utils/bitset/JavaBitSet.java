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
package com.qwazr.utils.bitset;

import java.util.BitSet;

public class JavaBitSet implements BitSetInterface {

	private final BitSet bitSet;

	JavaBitSet(long numBits) {
		this.bitSet = new BitSet((int) numBits);
	}

	JavaBitSet(int numBits) {
		this.bitSet = new BitSet(numBits);
	}

	private JavaBitSet(BitSet bitSet) {
		this.bitSet = (BitSet) bitSet.clone();
	}

	@Override
	final public BitSetInterface clone() {
		return new JavaBitSet(bitSet);
	}

	@Override
	final public long size() {
		return this.bitSet.size();
	}

	@Override
	final public boolean get(final long bit) {
		return this.bitSet.get((int) bit);
	}

	@Override
	final public void set(final long bit) {
		this.bitSet.set((int) bit);
	}

	@Override
	final public void set(final int[] bits) {
		for (int bit : bits)
			this.bitSet.set(bit);
	}

	@Override
	final public void set(final long[] bits) {
		for (long bit : bits)
			bitSet.set((int) bit);
	}

	@Override
	final public long cardinality() {
		return bitSet.cardinality();
	}

	@Override
	final public void flip(final long startBit, final long endBit) {
		this.bitSet.flip((int) startBit, (int) endBit);
	}

	@Override
	final public void and(final BitSetInterface bitSet) {
		this.bitSet.and(((JavaBitSet) bitSet).bitSet);
	}

	@Override
	final public void or(final BitSetInterface bitSet) {
		this.bitSet.or(((JavaBitSet) bitSet).bitSet);
	}

	@Override
	final public void clear(final long bit) {
		this.bitSet.clear((int) bit);
	}

	@Override
	final public long nextSetBit(final long index) {
		return this.bitSet.nextSetBit((int) index);
	}

}
