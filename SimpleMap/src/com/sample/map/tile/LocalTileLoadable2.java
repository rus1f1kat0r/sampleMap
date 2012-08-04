/**
 * 
 */
package com.sample.map.tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;

public class LocalTileLoadable2 extends LocalTileLoadable {

	public LocalTileLoadable2(Context appContext,
			Downloadable<Tile, Bitmap> mWrapped) {
		super(appContext, mWrapped);
	}

	@Override
	public void saveLocally(Tile key, Bitmap result) {
		Context context = getContext();
		if (context == null || result == null){
			return;
		}
		OutputStream os = null;
		try{
			ContentValues values = new ContentValues();
			values.put(Tile.KEY_ROW, key.row);
			values.put(Tile.KEY_COL, key.col);
			Uri tileUri = context.getContentResolver().insert(Tile.CONTENT_URI, values);
			os = context.getContentResolver().openOutputStream(tileUri);
			result.compress(CompressFormat.PNG, 80, os);
//			Log.d("TileLoadable", "saved tile to db");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null){
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public Bitmap loadLocalTile(Tile key, FastDownloadedCallback<Tile, Bitmap> callback) {
		InputStream is = null;
		try{	
			Context context = getContext();
			if (context != null){
				is = context.getContentResolver().openInputStream(key.getUri());
				Bitmap bmp = BitmapFactory.decodeStream(is);
				callback.onDownloaded(key, bmp);
				context.getContentResolver().update(key.getUri(), new ContentValues(), null, null);
//				Log.d("TileLoadable", "loaded tile from db " + bmp);
				return bmp;
			}
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if (is != null){
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
		return null;
	}

	@Override
	public boolean isAvailableLocal(Tile key) {
		Context context = getContext();
		if (context != null){
			Cursor c = null;
			try{
				c = context.getContentResolver().query(key.getUri(), new String[]{Tile._ID}, null, null, null);
				return c.getCount() > 0;
			} finally {
				if (c != null){
					c.close();					
				}
			}
		} else {
			return false;
		}
	}

	
}
