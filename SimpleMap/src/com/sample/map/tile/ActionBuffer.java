/**
 * 
 */
package com.sample.map.tile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

abstract class ActionBuffer<K, V>{
	private static final int ACTIVE_ACTIONS_MAX = 64;
	private static final int PENDING_ACTIONS_MAX = 64;
	
	private final Map<K, V> mActive;	
	private final LinkedList<Entry<K, V>> mPending;
		
	public ActionBuffer() {
		super();
		this.mActive = new HashMap<K, V>(ACTIVE_ACTIONS_MAX);
		this.mPending = new LinkedList<Entry<K, V>>();
	}
	
	protected abstract void performAction(K key, V value);
	
	protected void onActionRejected(K key, V value){
		
	}
	
	public void enque(K key, V value){
//		Log.d("ActionBuffer", "key " + key + "active size = " + mActive.size() + ", pending size " + mPending.size());
		if (!mActive.containsKey(key)){
			Entry<K, V> e = new Entry<K, V>(key, value);
			if (mActive.size() >= ACTIVE_ACTIONS_MAX){
				reorderToFrontErasingLast(e);
			} else {
				moveToActive(e);
			}
		}
	}

	private void moveToActive(Entry<K, V> e) {
		mActive.put(e.key, e.value);
		performAction(e.key, e.value);
	}
	
	public void remove(K key){
		if (mActive.containsKey(key)){
			mActive.remove(key);
			Entry<K, V> e = mPending.poll();
			if (e != null){
				moveToActive(e);
			}
		} else {
			mPending.remove(new Entry<K, V>(key, null));
		}
	}
	
	public boolean contains(K key){
		if (mActive.containsKey(key)){
			return true;
		} else {
			Entry<K, V> e = new Entry<K, V>(key, null);
			if (mPending.contains(e)){
				//get containing entry from the queue
				e = mPending.remove(mPending.indexOf(e));
				reorderToFront(e);
				return true;
			}
		}
		return false;
	}
	
	private void reorderToFront(Entry<K,V> e){
		mPending.remove(e);
		mPending.addFirst(e);
	}

	private void reorderToFrontErasingLast(Entry<K, V> e) {
		if (!mPending.remove(e) && mPending.size() >= PENDING_ACTIONS_MAX){
			Entry<K, V> entry = mPending.removeLast();
			onActionRejected(entry.key, entry.value);
		}
		mPending.addFirst(e);
	}
	
	private static class Entry<K, V>{
		private final K key;
		private final V value;
		
		public Entry(K key, V value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Entry<?, ?> other = (Entry<?, ?>) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}
		
	}
}
