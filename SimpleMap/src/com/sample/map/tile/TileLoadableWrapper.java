/**
 * 
 */
package com.sample.map.tile;

import android.graphics.Bitmap;

public class TileLoadableWrapper implements Downloadable<Tile, Bitmap> {

	private final Downloadable<Tile, Bitmap> mWrapped;
	
	public TileLoadableWrapper(Downloadable<Tile, Bitmap> mWrapped) {
		super();
		this.mWrapped = mWrapped;
	}
	/* (non-Javadoc)
	 * @see com.sample.map.tile.Downloadable#download(java.lang.Object)
	 */
	@Override
	public Bitmap download(Tile key) throws InterruptedException {
		return mWrapped.download(key);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return mWrapped.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		return mWrapped.equals(o);
	}
}
