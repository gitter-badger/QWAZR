package com.qwazr.utils;

import org.apache.commons.lang3.RandomUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RandomArrayIterator<T> implements Iterator<T> {

	private final T[] objects;

	private int pos;

	private int count;

	public RandomArrayIterator(T[] objects) {
		this.objects = objects;
		if (objects != null) {
			pos = RandomUtils.nextInt(0, objects.length);
			count = objects.length;
		} else {
			pos = 0;
			count = 0;
		}
	}

	@Override
	public boolean hasNext() {
		return count > 0;
	}

	@Override
	public T next() {
		if (count == 0)
			throw new NoSuchElementException();
		final T object = objects[pos++];
		if (pos == objects.length)
			pos = 0;
		count--;
		return object;
	}

	@Override
	public void remove() {
		throw new RuntimeException("Not available");
	}

}
