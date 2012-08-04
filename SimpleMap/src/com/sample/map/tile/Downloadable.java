package com.sample.map.tile;


public interface Downloadable<K, V> {

	V download(K key, FastDownloadedCallback<K, V> callback)
			throws InterruptedException;
}
