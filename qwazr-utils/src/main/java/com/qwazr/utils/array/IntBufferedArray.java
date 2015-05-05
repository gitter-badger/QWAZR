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

import java.util.ArrayList;
import java.util.List;

public class IntBufferedArray implements IntBufferedArrayInterface {

	private final long maxSize;

	private int initialArraySize;

	private int nextArraySize;

	private final List<int[]> arrays;

	private int[] currentArray;

	private int currentArrayPos;

	private int[] finalArray;

	private int totalSize;

	private IntBufferedArray(final long maxSize, final int initialArraySize) {
		this.maxSize = maxSize;
		this.initialArraySize = initialArraySize;
		this.arrays = new ArrayList<int[]>();
		reset();
	}

	@Override
	final public void reset() {
		this.nextArraySize = initialArraySize;
		this.arrays.clear();
		this.totalSize = 0;
		this.finalArray = null;
		newCurrentArray();
	}

	IntBufferedArray(final long maxSize) {
		this(maxSize, 16384);
	}

	final void newCurrentArray() {
		currentArray = new int[nextArraySize];
		arrays.add(currentArray);
		currentArrayPos = 0;
		long leftSize = maxSize - totalSize;
		if (nextArraySize > leftSize)
			nextArraySize = (int) leftSize;
	}

	@Override
	final public void add(final int value) {
		if (currentArrayPos == currentArray.length)
			newCurrentArray();
		currentArray[currentArrayPos++] = value;
		totalSize++;
	}

	@Override
	final public long getSize() {
		return totalSize;
	}

	@Override
	final public int[] getFinalArray() {
		if (finalArray != null)
			return finalArray;
		finalArray = new int[totalSize];
		int sizeLeft = totalSize;
		int buffer;
		int pos = 0;
		for (int[] array : arrays) {
			buffer = array.length;
			if (buffer > sizeLeft)
				buffer = sizeLeft;
			System.arraycopy(array, 0, finalArray, pos, buffer);
			pos += buffer;
			sizeLeft -= buffer;
		}
		clear();
		return finalArray;
	}

	final private void clear() {
		this.nextArraySize = initialArraySize;
		arrays.clear();
	}

}
