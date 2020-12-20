package com.ndhzs.recyclertest;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Objects;

public class DragSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private static final String TAG = "123";
    private RecyclerView mRecyclerView;
    private OnDragSelectListener dragSelectListener;

    private final int mMaxScrollDistance = 16;
    private int mAutoScrollRange, mUnableSelectRange;
    private int mTopBoundFrom, mTopBoundTo, mBottomBoundFrom, mBottomBoundTo;

    private boolean mIsInTopSpot, mIsInBottomSpot;
    private float mLastX, mLastY;

    private int mScrollDistance;
    private float mScrollSpeedFactor;

    private int mRvHeight, mRvWidth;

    private int mPreviousPosition;

    private boolean mIsFirstDo = true;

    private OverScroller mScroller;
    private final Runnable mScrollRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mScroller != null && mScroller.computeScrollOffset())
            {
                scrollBy(mScrollDistance);
                ViewCompat.postOnAnimation(mRecyclerView, mScrollRunnable);
            }
        }
    };

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        if (mRecyclerView == null) {
            this.mRecyclerView = rv;
            dragSelectListener = (OnDragSelectListener) rv.getAdapter();
            mRvHeight = rv.getHeight();
            mRvWidth = rv.getWidth();

            mAutoScrollRange = mRvHeight / 4;
            mUnableSelectRange = mRvHeight / 10;

            mTopBoundFrom = mUnableSelectRange;
            mTopBoundTo = mAutoScrollRange + mAutoScrollRange;
            mBottomBoundFrom = mRvHeight - mAutoScrollRange - mUnableSelectRange;
            mBottomBoundTo = mRvHeight - mUnableSelectRange;

            //取消动画效果
            ((SimpleItemAnimator) Objects.requireNonNull(rv.getItemAnimator())).setSupportsChangeAnimations(false);
        }

        boolean isAbleToDrag = dragSelectListener.onIsAbleToDrag();
        if (isAbleToDrag) {
            mPreviousPosition = dragSelectListener.onFirstPosition() - 1;
        }


        if (e.getAction() == MotionEvent.ACTION_CANCEL) {
            int firstPosition = dragSelectListener.onFirstPosition();
            dragSelectListener.onSelectChange(firstPosition);
            dragSelectListener.onSelectLastPosition(true, firstPosition);
            isAbleToDrag = false;
        }
        return isAbleToDrag;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                processAutoScroll(e);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                dragSelectListener.onSelectLastPosition(true, mPreviousPosition);
                reset();
                break;
        }
    }

    private void updateSelectedRange(float x, float y) {
        if (x < 0) {
            x = 1;
        }else if (x > mRvWidth) {
            x = mRvWidth - 1;
        }
        View child = mRecyclerView.findChildViewUnder(x, y);
        if (child != null) {
            int position = mRecyclerView.getChildLayoutPosition(child);
            if (position != mPreviousPosition) {
                int unitDifference = (position - mPreviousPosition) / Math.abs(position - mPreviousPosition);
                while (mPreviousPosition != position){
                    mPreviousPosition += unitDifference;
                    Log.d(TAG, "updateSelectedRange: position = "+mPreviousPosition);
                    dragSelectListener.onSelectChange(mPreviousPosition);
                }
            }
        }
    }

    private void processAutoScroll(MotionEvent event) {
        int y = (int) event.getY();

        if (y < mTopBoundFrom) {
            mLastX = event.getX();
            mLastY = 2 * mUnableSelectRange;
            mScrollDistance = mMaxScrollDistance * -1;
            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else if (y <= mTopBoundTo) {
            mLastX = event.getX();
            mLastY = event.getY();
            mScrollSpeedFactor = (((float) mTopBoundTo - (float) mTopBoundFrom) - ((float) y - (float) mTopBoundFrom)) / ((float) mTopBoundTo - (float) mTopBoundFrom);
            mScrollDistance = (int) ((float) mMaxScrollDistance * mScrollSpeedFactor * -1f);

            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else if (y >= mBottomBoundFrom && y <= mBottomBoundTo) {
            mLastX = event.getX();
            mLastY = event.getY();
            mScrollSpeedFactor = (((float) y - (float) mBottomBoundFrom)) / ((float) mBottomBoundTo - (float) mBottomBoundFrom);
            mScrollDistance = (int) ((float) mMaxScrollDistance * mScrollSpeedFactor);

            if (!mIsInBottomSpot) {
                mIsInBottomSpot = true;
                startAutoScroll();
            }
        }else if (y > mBottomBoundTo) {
            mLastX = event.getX();
            mLastY = mRvHeight - 2 * mUnableSelectRange;
            mScrollDistance = mMaxScrollDistance;
            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else {
            updateSelectedRange(event.getX(), y);
            mIsInBottomSpot = false;
            mIsInTopSpot = false;
            mLastX = Float.MIN_VALUE;
            mLastY = Float.MIN_VALUE;
            stopAutoScroll();
        }
    }

    private void startAutoScroll() {
        initScroller(mRecyclerView.getContext());
        if (mScroller.isFinished()) {
            mRecyclerView.removeCallbacks(mScrollRunnable);
            mScroller.startScroll(0, mScroller.getCurrY(), 0, 5000, 100000);
            ViewCompat.postOnAnimation(mRecyclerView, mScrollRunnable);
        }
    }

    private void initScroller(Context context) {
        if (mScroller == null)
            mScroller = new OverScroller(context, new LinearInterpolator());
    }

    private void stopAutoScroll() {
        if (mScroller != null && !mScroller.isFinished()) {
            mRecyclerView.removeCallbacks(mScrollRunnable);
            mScroller.abortAnimation();
        }
    }

    private void scrollBy(int distance) {
        if (dragSelectListener.onIsAbleToDrag()) {
            int scrollDistance;
            if (distance > 0) {
                scrollDistance = Math.min(distance, mMaxScrollDistance);
            } else {
                scrollDistance = Math.max(distance, -mMaxScrollDistance);
            }
            mRecyclerView.scrollBy(0, scrollDistance);
        }
        updateSelectedRange(mLastX, mLastY);
    }

    private void reset() {
        mIsInTopSpot = false;
        mIsInBottomSpot = false;
        stopAutoScroll();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }

    interface OnDragSelectListener {
        boolean onIsAbleToDrag();
        int onFirstPosition();
        void onSelectChange(int position);
        void onSelectLastPosition(boolean isFinish, int theLastPosition);
    }
}

