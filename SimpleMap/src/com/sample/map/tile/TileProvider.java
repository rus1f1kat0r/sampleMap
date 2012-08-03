package com.sample.map.tile;

import android.graphics.Bitmap;

public interface TileProvider {

	int rowCount();
	int colomnCount();
	
	int tileSize();
	
	Bitmap requestTile(int row, int column);
}
