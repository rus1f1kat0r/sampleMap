package com.sample.map.tile;

import com.sample.common.ServiceDelegate;
import com.sample.common.ServiceDelegate.BoundMode;
import com.sample.common.ServiceTask;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

public class DefaultTileProvider extends BaseTileProvider implements FastDownloadedCallback<Tile, Bitmap>{

	private final ActionBuffer<Tile, TileRequestTask> mPendingDownloads;
	private final ServiceDelegate<TileDownloader> mServiceDelegate;
	
	public DefaultTileProvider(Context mContextRef) {
		super(mContextRef);
		this.mPendingDownloads = new ActionBuffer<Tile, TileRequestTask>() {
			@Override
			protected void performAction(Tile key, TileRequestTask value) {
				value.execute();
			}
			
			@Override
			protected void onActionRejected(Tile key, TileRequestTask value) {
				super.onActionRejected(key, value);
				value.cancel(false);
			}
		};
		this.mServiceDelegate = new ServiceDelegate<TileDownloader>(mContextRef);
		mServiceDelegate.setDefaultIntent(new Intent(mContextRef, TileDownloader.class));
	}
	
	@Override
	public Bitmap requestTile(int row, int column) {
		Tile tile = new Tile(row, column);
		if (!mPendingDownloads.contains(tile)){
			TileDownloader service = mServiceDelegate.getService();
			if (service != null && service.isTileAvailable(tile)){
				//use self as a callback when call from ui thread 
				Bitmap t = service.getTile(tile, this);
				if (t != null){
					return t;
				}
			} else {
				TileRequestTask task = new TileRequestTask(tile);
				mPendingDownloads.enque(tile, task);
			}
		}
		return super.requestTile(row, column);
	}
	
	@Override
	public void release() {
		super.release();
		mServiceDelegate.doUnbindService();
	}
	
	@Override
	public void onDownloaded(Tile k, Bitmap v) {
		notifyTileAvailable(k.row, k.col, v);
	}
	
	private class TileRequestTask extends ServiceTask<Void, Bitmap, Bitmap, TileDownloader>{

		private final Tile mTile;
		private final FastDownloadedCallback<Tile, Bitmap> callback = new FastDownloadedCallback<Tile, Bitmap>() {
			
			@Override
			public void onDownloaded(Tile k, Bitmap v) {
				//return to UI thread
				publishProgress(v);
			}
		};
		
		public TileRequestTask(Tile tile) {			
			super(mServiceDelegate, BoundMode.KEEP_ALIVE, false);
			this.mTile = tile;
		}
		
		@Override
		protected Bitmap doInBackgroundService(TileDownloader service,
				Void... params) throws Exception {			
			return service.getTile(mTile, callback);
		}
		
		@Override
		protected void onError(Status status) {
			super.onError(status);
			mPendingDownloads.remove(mTile);
		}
		
		@Override
		protected void onProgressUpdate(Bitmap... values) {
			super.onProgressUpdate(values);
			onDownloaded(mTile, values[0]);
		}
		
		@Override
		protected void onPostExecuteService(Bitmap r) {
			super.onPostExecuteService(r);
			mPendingDownloads.remove(mTile);
//			notifyDataSetChanged();
//			notifyTileAvailable(mTile.row, mTile.col, r);
		}
	}
}
