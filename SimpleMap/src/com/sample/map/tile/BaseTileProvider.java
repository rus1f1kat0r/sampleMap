package com.sample.map.tile;

import java.lang.ref.WeakReference;

import com.sample.map.R;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public abstract class BaseTileProvider implements TileProvider {
	
	private static final int TILE_SIZE = 255;
	private static final int ROWS = 100;
	private static final int COLUMNS = 100;
	
	private final Bitmap mPlaceHolder;
	
	private final WeakReference<Context> mContextRef;
	
	private final DataSetObservable mDatasetObservable;
	private OnTileAvailableListener mTileAvailableListener;
	
	public BaseTileProvider(Context mContextRef) {
		super();
		this.mContextRef = new WeakReference<Context>(mContextRef);
		this.mDatasetObservable = new DataSetObservable();
		this.mPlaceHolder = BitmapFactory.decodeResource(
				getContext().getResources(), R.drawable.place_holder);
	}
	
	public void registerDatasetObserver(DataSetObserver observer){
		mDatasetObservable.registerObserver(observer);
	}
	
	public void unregisterDatasetObserver(DataSetObserver observer){
		mDatasetObservable.unregisterObserver(observer);
	}
	
	public void notifyDataSetChanged(){
		mDatasetObservable.notifyChanged();
	}
	
	public void notifyDataSetInvalidated(){
		mDatasetObservable.notifyInvalidated();
	}

	@Override
	public int rowCount() {
		return ROWS;
	}

	@Override
	public int colomnCount() {
		return COLUMNS;
	}

	@Override
	public int tileSize() {
		return TILE_SIZE;
	}

	@Override
	public Bitmap requestTile(int row, int column) {
		return mPlaceHolder;
	}
	
	public Bitmap getTilePlaceHolder(){
		return mPlaceHolder;
	}

	public Context getContext(){
		return mContextRef.get();
	}
	
	public void release(){
		removeOnTileAvailableListener();
	}
	
	public void setOnTileAvailableListener(OnTileAvailableListener listener){
		this.mTileAvailableListener = listener;
	}
	
	public void removeOnTileAvailableListener(){
		this.mTileAvailableListener = null;
	}
	
	protected void notifyTileAvailable(int row, int col, Bitmap bmp){
		if (mTileAvailableListener != null){
			mTileAvailableListener.onTileAvailable(row, col, bmp);
		}
	}
	
	public interface OnTileAvailableListener{
		void onTileAvailable(int row, int col, Bitmap bmp);
	}
}
