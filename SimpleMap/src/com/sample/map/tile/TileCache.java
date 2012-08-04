/**
 * 
 */
package com.sample.map.tile;

import android.content.Context;
import android.graphics.Bitmap;

class TileCache extends DownloaderCache<Tile, Bitmap> {

	private static TileCache sInstance;
	
	private TileCache(Context context) {
		super(new LocalTileLoadable2(context, new TileDownloadable()));
	}
	
	public static TileCache getInstance(Context context){
		if (sInstance == null){
			sInstance = new TileCache(context);
		}
		return sInstance;
	}
	
}
