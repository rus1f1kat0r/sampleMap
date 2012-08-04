package com.sample.map.tile;

import android.net.Uri;
import android.provider.MediaStore.MediaColumns;

final class Tile implements MediaColumns{

	static final String CONTENT_URI_PATH = "tiles";
	static final String TABLE_NAME = "tile";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + TileContentProvider.AUTHORITY + "/" + CONTENT_URI_PATH);
	public static final Uri CONTENT_URI_OLDEST = Uri.withAppendedPath(CONTENT_URI, "old");
	
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." 
			+ TileContentProvider.AUTHORITY + "." + TABLE_NAME;
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
			+ TileContentProvider.AUTHORITY + "." + TABLE_NAME;

	static final String LAST_ACCESS = "last_access";
	static final String KEY_ROW = "row";
	static final String KEY_COL = "col";
	
	final int row;
	final int col;
	
	Tile(int row, int col) {
		super();
		this.row = row;
		this.col = col;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + row;
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
		Tile other = (Tile) obj;
		if (col != other.col)
			return false;
		if (row != other.row)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Tile [row=" + row + ", col=" + col + "]";
	}
	
	public Uri getUri(){
		return getUri(row, col);
	}
	
	public static Uri getUri(int row, int col){
		return Uri.withAppendedPath(CONTENT_URI, row + "/" + col);
	}
	
}