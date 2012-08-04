package com.sample.map.tile;

import com.sample.common.LocalBinder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class TileDownloader extends Service {
	
	protected static final String LOG_TAG = "TileDownloader";
	
	private /*final */TileCache mCache;
	private /*final */BroadcastReceiver mNetworkStateReceiver;
	
	public TileDownloader() {
		super();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return new LocalBinder<TileDownloader>(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.mCache = TileCache.getInstance(getApplicationContext());
		mNetworkStateReceiver = new NetworkStateReceiver();
		registerReceiver(mNetworkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		Log.d(LOG_TAG, "onCreate()");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		startService(new Intent(this, LocalCachePurger.class));
		unregisterReceiver(mNetworkStateReceiver);
		Log.d(LOG_TAG, "onDestroy()");
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onLowMemory()
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		//mCache.purgeCache();
	}
	
	public Bitmap getTile(Tile t, FastDownloadedCallback<Tile, Bitmap> callback){
		try {
			return mCache.download(t, callback);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean isTileAvailable(Tile t){
		return mCache.isLoaded(t);
	}

	private final class NetworkStateReceiver extends BroadcastReceiver{

		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
				Bundle extras = intent.getExtras();		
				if (extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)){
					Log.w("NetworkStateReceiver", "NO_CONNECTION is available");
				} else {
					Log.i("NetworkStateReceiver", "CONNECTION is available");
					onConnectionAvailable(context);
				}
			}
		}

		private void onConnectionAvailable(Context context) {
			mCache.purgeCache();
		}
	}

}