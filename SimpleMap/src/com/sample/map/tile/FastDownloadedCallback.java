/**
 * 
 */
package com.sample.map.tile;

public interface FastDownloadedCallback<K, V>{
	void onDownloaded(K k, V v);
}