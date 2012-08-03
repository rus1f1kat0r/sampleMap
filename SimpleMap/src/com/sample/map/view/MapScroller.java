package com.sample.map.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Scroller;

public class MapScroller extends Scroller implements OnTouchListener {
	
	private static final int MSG_SCROLL = 10;
	private static final int SCRLL_DURATION_DEF= 150;
    private static final int SCRLL_DELTA_MIN = 1;

	private final OnScrollListener mScrollListener;
	private final OnGestureListener mGestureListener = new GestureListener();
	
    private final GestureDetector mGestureDetector;
    
    private boolean mIsScrollingPerformed;
    private int mLastScrollX;
    private int mLastScrollY;
    private float mLastTouchX;
    private float mLastTouchY;
    
    private final Handler mAnimationHandler = new AnimationHandler();
    
	public MapScroller(Context context, OnScrollListener listener) {
		super(context);
		this.mScrollListener = listener;
		this.mGestureDetector = new GestureDetector(context, mGestureListener);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
            mLastTouchX = event.getX();
            mLastTouchY = event.getY();
            forceFinished(true);
            clearMessages();
            handled = true;
			break;
		case MotionEvent.ACTION_MOVE:
			int dX = (int)(event.getX() - mLastTouchX);
            int dY = (int)(event.getY() - mLastTouchY);
            if (dX != 0 || dY != 0) {
                startScrolling();
                mScrollListener.onScroll(dX, dY);
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
            }
            handled = true;
			break;
		default:
			break;
		}
		return mGestureDetector.onTouchEvent(event) || handled;
	}
	
	public void scroll(int dx, int dy, int duration){
        forceFinished(true);
        
        mLastScrollX = 0;
        mLastScrollY = 0;        
        startScroll(mLastScrollX, mLastScrollY, dx, dy, duration);
        setNextMessage(MSG_SCROLL);
        startScrolling();
	}
	
	public void scroll(int dx, int dy){
		scroll(dx, dy, SCRLL_DURATION_DEF);
	}
	
	private void clearMessages() {
        mAnimationHandler.removeMessages(MSG_SCROLL);
	}

	public boolean isScrolling(){
		return mIsScrollingPerformed;
	}
	
	private void startScrolling(){
		if (!isScrolling()){
			mIsScrollingPerformed = true;
			mScrollListener.onScrollStarted();			
		}
	}
	
    void finishScrolling() {
        if (mIsScrollingPerformed) {
            mScrollListener.onScrollFinished();
            mIsScrollingPerformed = false;
        }
    }
    
    private void setNextMessage(int message) {
        clearMessages();
        mAnimationHandler.sendEmptyMessage(message);
    }

	private final class GestureListener extends SimpleOnGestureListener{
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return true;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
            mLastScrollY = 0;
            mLastScrollX = 0;
            final int maxY = 0x7FFFFFFF;
            final int minY = -maxY;
            fling(mLastScrollX, mLastScrollY, (int) - velocityX, (int) -velocityY, minY, maxY, minY, maxY);
            setNextMessage(MSG_SCROLL);
            return true;
		}
	}
	
	private final class AnimationHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
            computeScrollOffset();
            int currY = getCurrY();
            int deltaY = mLastScrollY - currY;
            mLastScrollY = currY;
            int currX = getCurrX();
            int deltaX = mLastScrollX - currX;
            mLastScrollX = currX;
            if (deltaX != 0 || deltaY != 0) {
                mScrollListener.onScroll(deltaX, deltaY);
            }
            
            // scrolling is not finished when it comes to final Y
            // so, finish it manually 
            if (Math.abs(currY - getFinalY()) < SCRLL_DELTA_MIN 
            		&& Math.abs(currX - getFinalX()) < SCRLL_DELTA_MIN) {
                currY = getFinalY();
                currX = getFinalX();
                forceFinished(true);
            }
            if (!isFinished()) {
                this.sendEmptyMessage(msg.what);
            } else {
                finishScrolling();
            }
		}
		
	}
	
	public interface OnScrollListener{
		
		public void onScroll(int distX, int distY);
		
		public void onScrollStarted();
		
		public void onScrollFinished();
	}
}
