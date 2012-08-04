/**
 * 
 */
package com.sample.map.tile;

import android.app.IntentService;
import android.content.Intent;

public class LocalCachePurger extends IntentService {
	
	public LocalCachePurger() {
		super("Cache Purger");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		getContentResolver().delete(Tile.CONTENT_URI_OLDEST, null, null);
	}
}
