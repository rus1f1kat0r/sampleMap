/**
 * 
 */
package com.sample.map.tile;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * @author Kirill
 *
 */
public class LocalCachePurger extends IntentService {
	private static final long LOCAL_CACHE_SIZE_MAX = 25 * 1024 * 1024;
	
	public LocalCachePurger() {
		super("Cache Purger");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		File cacheDir = getCacheDir();
		long size = 0;
		File[] files = cacheDir.listFiles();
		for (File f:files) {
			size += f.length();
		}
		Log.d(getClass().getSimpleName(), "cacheDir size = " + size);
		if (size > LOCAL_CACHE_SIZE_MAX){
			synchronized(LocalCachePurger.class){
				Arrays.sort(files, new Comparator<File>(){
					public int compare(File f1, File f2){
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					} 
				});
			}
			List<File> fs = new LinkedList<File>();
			Collections.addAll(fs, files);
			while (size > LOCAL_CACHE_SIZE_MAX && fs.size() > 0){
				File f = fs.get(0);
				long fsize = f.length();
				Log.d(getClass().getSimpleName(), "file " + f.getName() + " is about to be deleted " + fsize);
				size -= f.delete() ? fsize : 0;
				fs.remove(f);
			}
			Log.d(getClass().getSimpleName(), "cacheDir size after clearing = " + size);
		}
	}

}
