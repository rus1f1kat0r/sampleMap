package com.sample.map.tile;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

class DownloaderCache<K, V> implements Downloadable<K, V>{

	private final TwoLevelCache<K, Future<V>> mCache;
	private final Downloadable<K, V> mDownloadInterface;
	
	public DownloaderCache(Downloadable<K, V> d) {
		super();
		this.mCache = new TwoLevelCache<K, Future<V>>();
		this.mDownloadInterface = d;
	}

	@Override
	public V download(K key, FastDownloadedCallback<K, V> callback) throws InterruptedException{
		while(true){
			Future<V> f = createFutureTask(key, callback);
			try{
				return f.get();
			}
			catch (CancellationException e) {
				mCache.remove(key, f);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException){
					throw (RuntimeException) cause;
				} else if (cause instanceof Error){
					throw (Error) cause;
				} else {
					throw new IllegalStateException("Not unchecked", cause);
				}
			}
		}
	}

	private Future<V> createFutureTask(final K key, final FastDownloadedCallback<K, V> callback) {
		Future<V> f = mCache.get(key);
		if (f == null){
			Callable<V> eval = new Callable<V>() {

				@Override
				public V call() throws Exception {
					return mDownloadInterface.download(key, callback);
				}
			};
			FutureTask<V> ft = new FutureTask<V>(eval);
			f = mCache.putIfAbsent(key, ft);
			if (f == null){
				f = ft;
				ft.run();
			}
		}
		return f;
	}
	
	boolean isLoaded(K key){
		Future<V> f = mCache.get(key);
		return f != null && f.isDone();
	}
	
	void purgeCache(){
		mCache.reset();
	}
	
}
