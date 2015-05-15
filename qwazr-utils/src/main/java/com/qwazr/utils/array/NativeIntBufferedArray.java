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

public class NativeIntBufferedArray implements IntBufferedArrayInterface {

	private long ref;

	private int pos;

	private int[] buffer = new int[16384];

	final private long maxSize;

	final private native long init(final long maxSize);

	NativeIntBufferedArray(long maxSize) {
		this.maxSize = maxSize;
		reset();
	}

	@Override
	final public void reset() {
		pos = 0;
		ref = init(maxSize);
	}

	final private native void free(final long ref);

	@Override
	protected void finalize() {
		free(ref);
		ref = 0;
	}

	final private void flushBuffer() {
		add(ref, buffer, pos);
		pos = 0;
	}

	final private native void add(final long ref, final int[] buffer,
			final int length);

	@Override
	final public void add(final int value) {
		if (pos == buffer.length)
			flushBuffer();
		buffer[pos++] = value;
	}

	final private native long getSize(final long ref);

	@Override
	final public long getSize() {
		return getSize(ref) + pos;
	}

	public native void populateFinalArray(final long ref, final int[] finalArray);

	@Override
	final public int[] getFinalArray() {
		if (pos > 0)
			flushBuffer();
		int[] finalArray = new int[(int) getSize()];
		populateFinalArray(ref, finalArray);
		return finalArray;
	}

}
