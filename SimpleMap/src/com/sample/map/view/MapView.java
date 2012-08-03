package com.sample.map.view;

import com.sample.map.tile.BaseTileProvider;
import com.sample.map.view.MapScroller.OnScrollListener;

import android.R;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MapView extends View {

	private BaseTileProvider mTileProvider;
	private final DataSetObserver mObserver = new DatasetObserver();
	private final MapScroller mScroller;
	private final Paint mPaint;
		
	private int mOffsetX;
	private int mOffsetY;
	private boolean mScrolling;
	private boolean mDragging;
	
	private boolean mChanged = false;
	private int mTileSize;
	private int mRows;
	private int mCols;
	
	private int mFirstVisibleRow;
	private int mFirstVisibleCol;
	
	public MapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mScroller = new MapScroller(context, new ScrollListener());
		this.mPaint = new Paint();
	}

	public MapView(Context context, AttributeSet attrs) {
		this(context, attrs, R.style.Theme_Black);
	}

	public MapView(Context context) {
		this(context, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDragging = true;
			break;
		case MotionEvent.ACTION_UP:
			mDragging = false;
			justifyOverscrollIfNeeded();
			break;
		default:
			break;
		}
		return mScroller.onTouch(this, event);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(parentWidth, parentHeight);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mTileProvider == null){
			return;
		}		
		if (mChanged){
			mTileSize = mTileProvider.tileSize();	
			mRows = mTileProvider.rowCount();
			mCols = mTileProvider.colomnCount();
			mChanged = false;
		}
		if (mRows != 0 && mCols != 0){
			drawTiles(canvas);
		}
	}
	
	private void drawTiles(Canvas canvas){
		int rowStart = getFirstVisibleTileRow(mOffsetY);
		int yOffset = mOffsetY + (rowStart * mTileSize);
		for (int row = rowStart; isTileInsideY(row, yOffset); row++){
			int colStart = getFirstVisibleTileCol(mOffsetX);
			int xOffset = mOffsetX + (colStart * mTileSize);
			for (int col = colStart; isTileInsideX(col, xOffset); col++){
//				Log.d(VIEW_LOG_TAG, "yOff " + yOffset + " xOff " + xOffset);
//				Log.d(VIEW_LOG_TAG, "row " + row + " col " + col);
				onDrawTile(canvas, row, col, yOffset, xOffset);
				xOffset += mTileSize;
			}
			yOffset += mTileSize;
		}
	}

	protected void onDrawTile(Canvas canvas, int row, int col, int yOffset,
			int xOffset) {
		Bitmap tile = mTileProvider.requestTile(row, col);
		if (!tile.isRecycled()){
			canvas.drawBitmap(tile, xOffset, yOffset, mPaint);
		}
	}
	
	private int getFirstVisibleTileRow(int offset){
		int r = Math.max(0-offset, 0) / mTileSize;
//		Log.d(VIEW_LOG_TAG, "first visible row = " + r  + " offsety = " + offset);
		return r;
	}
	
	private int getFirstVisibleTileCol(int offset){
		int c = Math.max(0-offset, 0) / mTileSize;
//		Log.d(VIEW_LOG_TAG, "first visible column = " + c);
		return c;
	}
	
	private int getFirstVisibleTileRow(){
		return getFirstVisibleTileRow(mOffsetY);
	}
	
	private int getFirstVisibleTileCol(){
		return getFirstVisibleTileCol(mOffsetX);
	}
	
	private boolean isTileInsideX(int column, int xOffset){
		return xOffset <= getWidth() && column < mCols;
	}
	
	private boolean isTileInsideY(int row, int yOffset){
		return yOffset <= getHeight() && row < mRows;
	}
	
	public void setTileProvider(BaseTileProvider provider){
		if (mTileProvider != null){
			mTileProvider.unregisterDatasetObserver(mObserver);
			mTileProvider = null;
		}
		if (provider != null){
			mTileProvider = provider;
			mTileProvider.registerDatasetObserver(mObserver);
		}
		mChanged = true;
		invalidate();
	}
	
	public BaseTileProvider getTileProvider(){
		return mTileProvider;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mTileProvider != null){
			mTileProvider.release();
		}
	}
	
	private int computeMapWidth(){
		return mCols * mTileSize;
	}
	
	private int computeMapHeight(){
		return mRows * mTileSize;
	}

	private int isOverScrolledX() {
		if (mOffsetX > 0){
			return -mOffsetX;
		} else if (getWidth() - mOffsetX > computeMapWidth()){
			return getWidth() - mOffsetX - computeMapWidth();
		}
		return 0;
	}
	
	private int isOverScrolledY() {
		if (mOffsetY > 0){
			return -mOffsetY;
		} else if (getHeight() - mOffsetY > computeMapHeight()){
			return getHeight() - mOffsetY - computeMapHeight();
		}
		return 0;
	}
	
	private void justifyOverscrollIfNeeded(){
		if (!mDragging){
			int overscrollX = isOverScrolledX();
			int overscrollY = isOverScrolledY();
			if (overscrollX != 0 || overscrollY != 0){
				justifyOverscroll(overscrollX, overscrollY);
			}
		}
	}
	
	private void justifyOverscroll(int xoffset, int yoffset) {
		mScroller.finishScrolling();
		mScroller.scroll(-xoffset, -yoffset);
	}
	
	public boolean isScrolling(){
		return mScrolling;
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
	    Parcelable superState = super.onSaveInstanceState();
	    MapState state = new MapState(superState);
	    state.xOffset = mOffsetX;
	    state.yOffset = mOffsetY;
	    return state;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
	    if(state instanceof MapState) {
	    	MapState mapstate = (MapState)state;
	    	super.onRestoreInstanceState(mapstate.getSuperState());
	    	this.mOffsetX = mapstate.xOffset;
	    	this.mOffsetY = mapstate.yOffset;
	    } else {
	    	super.onRestoreInstanceState(state);	    	
	    }
	}
	
	private class DatasetObserver extends DataSetObserver{

		@Override
		public void onChanged() {
			super.onChanged();
			mChanged = true;
			invalidate();
		}

		@Override
		public void onInvalidated() {
			super.onInvalidated();
		}
		
	}
	
	private class ScrollListener implements OnScrollListener{

		@Override
		public void onScroll(int distX, int distY) {
			if (Math.abs(distX) > 0 || Math.abs(distY) > 0){
				Log.d(VIEW_LOG_TAG, "scroll ( "+distX + ", " + distY + ")");
				mOffsetX+= distX;
				mOffsetY+= distY;
				justifyOverscrollIfNeeded();
				invalidate();
			}
		}

		@Override
		public void onScrollStarted() {
			mScrolling = true;
		}

		@Override
		public void onScrollFinished() {
			mScrolling = false;
		}		
	}
	
	private class VisibleAreaInfo{
		private int firstVisibleRow;
		private int firstVisibleCol;
		
		private int visibleRowsCount;
		private int visibleColsCount;
		
		private Bitmap[][] tiles;
		
		private void init(){
			this.firstVisibleRow = getFirstVisibleTileRow();
			this.firstVisibleCol = getFirstVisibleTileCol();
		}
		
		private Bitmap tile(int row, int col){
			if (hasTile(row, col)){
				return tiles[row - firstVisibleRow] [col - firstVisibleCol];
			} else {
				return null;
			}
		}
		
		private boolean hasTile(int row, int col){
			return 		row >= firstVisibleRow && row - firstVisibleRow < visibleRowsCount
					&& 	col >= firstVisibleCol && col - firstVisibleCol < visibleColsCount;
		}
	}
	
	protected static class MapState extends BaseSavedState {
	   int xOffset;
	   int yOffset;

	    MapState(Parcelable superState) {
	      super(superState);
	    }

	    private MapState(Parcel in) {
	      super(in);
	      this.xOffset = in.readInt();
	      this.yOffset = in.readInt();
	    }

	    @Override
	    public void writeToParcel(Parcel out, int flags) {
	      super.writeToParcel(out, flags);
	      out.writeInt(xOffset);
	      out.writeInt(yOffset);
	    }
	    //required field that makes Parcelables from a Parcel
	    public static final Parcelable.Creator<MapState> CREATOR =
	        new Parcelable.Creator<MapState>() {
	          public MapState createFromParcel(Parcel in) {
	            return new MapState(in);
	          }
	          public MapState[] newArray(int size) {
	            return new MapState[size];
	          }
	    };
	}
}
