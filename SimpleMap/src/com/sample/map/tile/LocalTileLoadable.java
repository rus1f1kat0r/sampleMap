/**
 * 
 */
package com.sample.map.tile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class LocalTileLoadable extends TileLoadableWrapper{
	private static final String TILE_PREFIX = "tile_";
	private static final String TILE_SUFFIX = ".png";

	private final WeakReference<Context> mContextRef;
	
	public LocalTileLoadable(Context appContext, Downloadable<Tile, Bitmap> mWrapped) {
		super(mWrapped);
		this.mContextRef = new WeakReference<Context>(appContext);
	}
	
	@Override
	public Bitmap download(Tile key, FastDownloadedCallback<Tile, Bitmap> callback) throws InterruptedException {
		Bitmap result = null;
		if (isAvailableLocal(key)){
			result = loadLocalTile(key, callback);
		}
		if (result == null){
			result = super.download(key, callback);
			if (result != null){
				callback.onDownloaded(key, result);
				saveLocally(key, result);
			}
		}
		return result;
	}
	
	public void saveLocally(Tile key, Bitmap result) {
		Context context = mContextRef.get();
		if (context != null && result != null){
//			removeOldFilesIfNeeded(context);
			writeFile(result, getLocalTile(context, key));
		}
	}

//	private void removeOldFilesIfNeeded(Context context) {
//		context.startService(new Intent(context, LocalCachePurger.class));
//	}

	public Bitmap loadLocalTile(Tile key, FastDownloadedCallback<Tile, Bitmap> callback) {
		Context context = mContextRef.get();
		if (context != null){
			File f = getLocalTile(context, key);
			Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
			callback.onDownloaded(key, b);
			synchronized (LocalCachePurger.class) {
				f.setLastModified(System.currentTimeMillis());
			}
			return b;
		} else {
			return null;
		}
	}

	public boolean isAvailableLocal(Tile key){
		Context context = mContextRef.get();
		if (context != null) {
			File tile = getLocalTile(context, key);
			return tile.exists();
		} else {
			return false;
		}
	}
	
	private File getLocalTile(Context context, Tile key){
		File cacheDir = context.getCacheDir();
		if (!cacheDir.exists()){
			cacheDir.mkdirs();
		}
		File tile = new File(cacheDir, TILE_PREFIX + key.row + "_" + key.col + TILE_SUFFIX);
		return tile;
	}
	
	private void writeFile(Bitmap bmp, File f) {
		OutputStream out = null;
		try {
			out = new FileOutputStream(f);
			bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally { 
			try { 
				if (out != null ) {
					out.close(); 
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			} 
		}
	}
	
	public Context getContext(){
		return mContextRef.get();
	}
}
