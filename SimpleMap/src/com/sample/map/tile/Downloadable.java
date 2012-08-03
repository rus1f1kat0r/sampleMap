package com.sample.map.tile;

public interface Downloadable<K, V> {
	V download (K key) throws InterruptedException;
}
