package com.sample.map.view;

import com.sample.map.tile.BaseTileProvider;
import com.sample.map.tile.BaseTileProvider.OnTileAvailableListener;
import com.sample.map.view.MapScroller.OnScrollListener;

import android.R;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MapView extends View {

	private BaseTileProvider mTileProvider;
	private final DatasetObserver mObserver = new DatasetObserver();
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

	private final VisibleAreaInfo mVisibleArea;
	
	public MapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mScroller = new MapScroller(context, new ScrollListener());
		this.mPaint = new Paint();
		this.mVisibleArea = new VisibleAreaInfo();
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
			mVisibleArea.init();
			mChanged = false;
		}
		if (mRows != 0 && mCols != 0){
			drawTiles(canvas);
		}
	}

	private void drawTiles(Canvas canvas){
        final Rect rr = canvas.getClipBounds();
        Log.d("DEBUG", "rectangle of invalidation:" + rr);
        //TODO draw don't try to draw outside the dirty rectangle
		int rowStart = mVisibleArea.firstVisibleRow;
		int yOffset = mOffsetY + (rowStart * mTileSize);
		for (int r = 0; r < mVisibleArea.visibleRowsCount; r++){
			int colStart = mVisibleArea.firstVisibleCol;
			int xOffset = mOffsetX + (colStart * mTileSize);
			for (int c = 0; c < mVisibleArea.visibleColsCount; c ++){
//				Log.d(VIEW_LOG_TAG, "yOff " + yOffset + " xOff " + xOffset);
//				Log.d(VIEW_LOG_TAG, "row " + row + " col " + col);
				onDrawTile(canvas, rowStart + r, colStart + c, yOffset, xOffset);
				xOffset += mTileSize;
			}
			yOffset += mTileSize;
		}
	}
	
	protected void onDrawTile(Canvas canvas, int row, int col, int yOffset,
			int xOffset) {
		Bitmap tile = null;
		if (mVisibleArea.hasTile(row, col)){
			BitmapHolder holder = mVisibleArea.tile(row, col);
			tile = holder.bmp;
			holder.calculateAlpha();
			if (holder.animating){
				drawTileBackground(canvas, yOffset, xOffset);
				invalidate(xOffset, yOffset, xOffset + mTileSize, yOffset + mTileSize);
			}
			mPaint.setAlpha(holder.alpha);
		} else {
			tile = mTileProvider.requestTile(row, col);
			if (tile != null && tile != mTileProvider.getTilePlaceHolder()){
				mVisibleArea.setTile(row, col, tile);
			} else {
				drawTileBackground(canvas, yOffset, xOffset);
				return;
			}
		}
		if (!tile.isRecycled()){
			canvas.drawBitmap(tile, xOffset, yOffset, mPaint);
		}
	}
	
	private void drawTileBackground(Canvas canvas, int yOffset, int xOffset) {
		//always opaque
		mPaint.setAlpha(255);
		Bitmap placeholder = mTileProvider.getTilePlaceHolder();
		canvas.drawBitmap(placeholder, xOffset, yOffset, mPaint);
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
	
	private int getVisibleRows(int startRow){
		//
		Double d = Math.ceil((double)(getHeight() - mOffsetY) / mTileSize);
		return Math.min(mRows, d.intValue()) - startRow;
	}
	
	private int getVisibleCols(int startCol){
		Double d = Math.ceil((double)(getWidth() - mOffsetX) / mTileSize);
		return Math.min(mCols, d.intValue()) - startCol;
	}
		
	private boolean isTileInsideX(int column, int xOffset){
		return xOffset <= getWidth() && column < mCols;
	}
	
	private boolean isTileInsideY(int row, int yOffset){
		return yOffset <= getHeight() && row < mRows;
	}
	
	private Rect tileOnScreen(int row, int col){
		int t = (row * mTileSize) + mOffsetY; 
		int l = (col * mTileSize) + mOffsetX;
		return new Rect(l, t, l + mTileSize, t + mTileSize);
	}
	
	public void setTileProvider(BaseTileProvider provider){
		if (mTileProvider != null){
			mTileProvider.unregisterDatasetObserver(mObserver);
			mTileProvider.removeOnTileAvailableListener();
			mTileProvider = null;
		}
		if (provider != null){
			mTileProvider = provider;
			mTileProvider.registerDatasetObserver(mObserver);
			mTileProvider.setOnTileAvailableListener(mObserver);
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
	
	private void doScroll(){
		mVisibleArea.init();
		justifyOverscrollIfNeeded();
		invalidate();
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
	
	private class DatasetObserver extends DataSetObserver implements OnTileAvailableListener{

		@Override
		public void onChanged() {
			super.onChanged();
			mChanged = true;
			invalidate();
			Log.d(VIEW_LOG_TAG, "onChanged()");
		}

		@Override
		public void onInvalidated() {
			super.onInvalidated();
		}

		@Override
		public void onTileAvailable(int row, int col) {
			if (mVisibleArea.isVisibleTile(row, col)){
				mVisibleArea.setTile(row, col, mTileProvider.requestTile(row, col), true);

	            final Rect r = tileOnScreen(row, col);
	            Log.d("DEBUG", "rectangle of tile:" + r);
				invalidate(r);
			}
		}
		
	}
	
	private class ScrollListener implements OnScrollListener{

		@Override
		public void onScroll(int distX, int distY) {
			if (Math.abs(distX) > 0 || Math.abs(distY) > 0){
//				Log.d(VIEW_LOG_TAG, "scroll ( "+distX + ", " + distY + ")");
				mOffsetX += distX;
				mOffsetY += distY;
				doScroll();
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
		
		private BitmapHolder[][] tiles;
		
		public VisibleAreaInfo() {
			super();
		}

		public VisibleAreaInfo(VisibleAreaInfo info){
			this.firstVisibleRow = info.firstVisibleRow;
			this.firstVisibleCol = info.firstVisibleCol;
			this.visibleRowsCount = info.visibleRowsCount;
			this.visibleColsCount = info.visibleColsCount;
			this.tiles = info.tiles;
		}
		private void init(){
			VisibleAreaInfo oldInfo = new VisibleAreaInfo(this);
			
			this.firstVisibleRow = getFirstVisibleTileRow();
			this.firstVisibleCol = getFirstVisibleTileCol();
			this.visibleRowsCount = getVisibleRows(firstVisibleRow);
			this.visibleColsCount = getVisibleCols(firstVisibleCol);
//			Log.d(VIEW_LOG_TAG, "init()" + this.toString());

			tiles = new BitmapHolder[visibleRowsCount] [visibleColsCount];
			
			for (int r = 0; r < visibleRowsCount; r++){
				for (int c = 0; c < visibleColsCount; c++){
					tiles [r][c] = oldInfo.tile(firstVisibleRow + r, firstVisibleCol + c);
				}
			}
		}
		
		private BitmapHolder tile(int row, int col){
			if (isVisibleTile(row, col)){
				return tiles[row - firstVisibleRow] [col - firstVisibleCol];
			} else {
				return null;
			}
		}
		
		private boolean isVisibleTile(int row, int col){
			return 		row >= firstVisibleRow && row - firstVisibleRow < visibleRowsCount
					&& 	col >= firstVisibleCol && col - firstVisibleCol < visibleColsCount;
		}

		@Override
		public String toString() {
			return "VisibleAreaInfo [firstRow=" + firstVisibleRow
					+ ", firstCol=" + firstVisibleCol
					+ ", RowsCount=" + visibleRowsCount
					+ ", ColsCount=" + visibleColsCount + "]";
		}
		
		private boolean hasTile(int row, int col){
			return 	isVisibleTile(row, col) 
					&& tiles[row - firstVisibleRow] [col - firstVisibleCol] != null;
		}
		
		private void setTile(int row, int col, Bitmap tile){
			BitmapHolder b = new BitmapHolder(tile, false);
			tiles[row - firstVisibleRow] [col - firstVisibleCol] = b;
		}
		
		private void setTile(int row, int col, Bitmap tile, boolean animate){
			BitmapHolder b = new BitmapHolder(tile, animate);
			tiles[row - firstVisibleRow] [col - firstVisibleCol] = b;
		}
	}
	
	private static class BitmapHolder {
		private static final long FADE_DURATION = 200;
		private final Bitmap bmp;
		private int alpha;
		private boolean animating;
		private long animStart;
		
		private BitmapHolder(Bitmap bmp, boolean animate){
			this.bmp = bmp;
			this.animStart = System.currentTimeMillis();
			this.animating = animate;
			this.alpha = animate ? 0 : 255;
		}
		
		private void calculateAlpha(){
			if (!animating){
				return ;
			}
			if (FADE_DURATION + animStart < System.currentTimeMillis()){
				animating = false;
				alpha = 255;
				return;
			}
			float k = ((System.currentTimeMillis() - animStart) * 1.0f) / (float)FADE_DURATION;
			alpha = (int) (k * 255);
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
