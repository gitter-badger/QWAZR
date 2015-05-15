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
package com.qwazr.utils.array;

import java.util.ArrayList;
import java.util.List;

public class FloatBufferedArray implements FloatBufferedArrayInterface {

	private final long maxSize;

	private final int initialArraySize;

	private int nextArraySize;

	private final List<float[]> arrays;

	private float[] currentArray;

	private int currentArrayPos;

	private float[] finalArray;

	private int totalSize;

	private FloatBufferedArray(final long maxSize, final int initialArraySize) {
		this.maxSize = maxSize;
		this.initialArraySize = initialArraySize;
		this.arrays = new ArrayList<float[]>();
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

	FloatBufferedArray(final long maxSize) {
		this(maxSize, 4096);
	}

	final void newCurrentArray() {
		currentArray = new float[nextArraySize];
		arrays.add(currentArray);
		currentArrayPos = 0;
		if (nextArraySize > maxSize - totalSize)
			nextArraySize = (int) (maxSize - totalSize);
	}

	@Override
	final public void add(final float value) {
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
	final public float[] getFinalArray() {
		if (finalArray != null)
			return finalArray;
		finalArray = new float[totalSize];
		int sizeLeft = totalSize;
		int buffer;
		int pos = 0;
		for (float[] array : arrays) {
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

	final protected void clear() {
		this.nextArraySize = initialArraySize;
		arrays.clear();
	}

}
