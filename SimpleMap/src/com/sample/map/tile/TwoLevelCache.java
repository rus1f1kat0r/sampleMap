package com.sample.map.tile;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoLevelCache <K, V>{
    private static final int HARD_CACHE_CAPACITY = 20;

    private final HashMap<K, V> mHardCache =
        new CacheMap(HARD_CACHE_CAPACITY / 2, 0.75f, true);
    
    private final ConcurrentHashMap<K, SoftReference<V>> mSoftCache =
        new ConcurrentHashMap<K, SoftReference<V>>(HARD_CACHE_CAPACITY );

	public V get(K key) {
        // First try the hard reference cache
        synchronized (mHardCache) {
            final V val = mHardCache.get(key);
            if (val != null) {
                // Move element to first position, so that it is removed last
                mHardCache.remove(key);
                mHardCache.put(key, val);
                return val;
            }
        }
        // Then try the soft reference cache
        SoftReference<V> valReference = mSoftCache.get(key);
        if (valReference != null) {
            final V v = valReference.get();
            if (v != null) {
                return v;
            } else {
                // Soft reference has been Garbage Collected
                mSoftCache.remove(key);
            }
        }
        return null;
	}

	public void put(K key, V val) {
        if (val != null) {
            synchronized (mHardCache) {
                mHardCache.put(key, val);
            }
        }
	}
	
	public V putIfAbsent(K key, V val){
		V v = get(key);
		if (v == null) {
			synchronized (mHardCache) {
		        return mHardCache.put(key, val);
			}
		} 
		return v;
	}
	
	public boolean remove(K key, V v){
		synchronized (mHardCache) {
			if (mHardCache.containsKey(key) && mHardCache.get(key).equals(v)) {
		       mHardCache.remove(key);
		       return true;
		   } else return false;
		}
	}

	public void reset() {
        mHardCache.clear();
        mSoftCache.clear();
    }
	
	private final class CacheMap extends LinkedHashMap<K, V> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2380258801705624930L;

		private CacheMap(int initialCapacity, float loadFactor,
				boolean accessOrder) {
			super(initialCapacity, loadFactor, accessOrder);
		}

		@Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to soft reference cache
                mSoftCache.put(eldest.getKey(), new SoftReference<V>(eldest.getValue()));
                return true;
            } else
                return false;
        }
	}
}
