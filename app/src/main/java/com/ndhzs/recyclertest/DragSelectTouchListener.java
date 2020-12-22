package com.ndhzs.recyclertest;

import android.content.Context;
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

            mAutoScrollRange = mRvHeight / 8;
            //取消动画效果
            ((SimpleItemAnimator) Objects.requireNonNull(rv.getItemAnimator())).setSupportsChangeAnimations(false);
        }

        int i = dragSelectListener.onGetUnableSelectRange();
        mUnableSelectRange = i == 0 ? 0 : mRvHeight / i;

        mTopBoundFrom = mUnableSelectRange;
        mTopBoundTo = mAutoScrollRange + mUnableSelectRange;
        mBottomBoundFrom = mRvHeight - mAutoScrollRange - mUnableSelectRange;
        mBottomBoundTo = mRvHeight - mUnableSelectRange;

        boolean isAbleToDrag = dragSelectListener.onCanDrag();
        if (isAbleToDrag) {
            mPreviousPosition = dragSelectListener.onStartPosition() - 1;
        }

        if (e.getAction() == MotionEvent.ACTION_CANCEL) {
            int firstPosition = dragSelectListener.onStartPosition();
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
                while (mPreviousPosition != position) {
                    mPreviousPosition += unitDifference;
                    dragSelectListener.onSelectChange(mPreviousPosition);
                }
            }
        }
    }

    private void processAutoScroll(MotionEvent event) {
        int y = (int) event.getY();

        if (y < mTopBoundFrom && dragSelectListener.onCanAutoScrollUp()) {
            mLastX = event.getX();
            mLastY = mUnableSelectRange;
            mScrollDistance = mMaxScrollDistance * -1;
            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else if (y <= mTopBoundTo && dragSelectListener.onCanAutoScrollUp()) {
            mLastX = event.getX();
            mLastY = event.getY();
            mScrollSpeedFactor = (((float) mTopBoundTo - (float) mTopBoundFrom) - ((float) y - (float) mTopBoundFrom)) / ((float) mTopBoundTo - (float) mTopBoundFrom);
            mScrollDistance = (int) ((float) mMaxScrollDistance * mScrollSpeedFactor * -1f);

            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else if (y >= mBottomBoundFrom && y <= mBottomBoundTo && dragSelectListener.onCanAutoScrollDown()) {
            mLastX = event.getX();
            mLastY = event.getY();
            mScrollSpeedFactor = (((float) y - (float) mBottomBoundFrom)) / ((float) mBottomBoundTo - (float) mBottomBoundFrom);
            mScrollDistance = (int) ((float) mMaxScrollDistance * mScrollSpeedFactor);

            if (!mIsInBottomSpot) {
                mIsInBottomSpot = true;
                startAutoScroll();
            }
        }else if (y > mBottomBoundTo && dragSelectListener.onCanAutoScrollDown()) {
            mLastX = event.getX();
            mLastY = mRvHeight - mUnableSelectRange;
            mScrollDistance = mMaxScrollDistance;
            if (!mIsInTopSpot) {
                mIsInTopSpot = true;
                startAutoScroll();
            }
        }else {
            if (y < 0) {
                y = 1;
            }else if (y > mRvHeight) {
                y = mRvHeight - 1;
            }
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
        if (dragSelectListener.onCanDrag()) {
            int scrollDistance;
            if (distance > 0) {
                scrollDistance = Math.min(distance, mMaxScrollDistance);
            }else {
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

        /**
         * @return 得到能否继续滑动的判断，此判断可以强行终止滑动，如果手指仍未离开，接口onSelectChange仍会返回手指移动到的位置
         */
        boolean onCanDrag();

        /**
         * @return 能否自动向上滑动
         */
        boolean onCanAutoScrollUp();

        /**
         * @return 能否自动向下滑动
         */
        boolean onCanAutoScrollDown();

        /**
         * @return 得到滑动开始时的位置
         */
        int onStartPosition();

        /**
         * 返回一个int整数，用mRvHeight除以，求出边界不能选择的范围，返回数越大，不能选择的范围越大
         * 建议返回RecyclerView加载的当前页的总item个数的倍数，1倍时会空一个item，0.5倍会空两个item
         * 此方法可以防止滑动结束后选择了在边界位置的item
         * @return int i = dragSelectListener.onGetUnableSelectRange();
         *         mUnableSelectRange = i == 0 ? 0 : mRvHeight / i;
         */
        int onGetUnableSelectRange();

        /**
         * @param position 通知adapter变化了的item位置（会包括开始位置和结尾位置），连续传入的两个位置不会相等
         */
        void onSelectChange(int position);

        /**
         * @param isFinish 是否结束了滑动
         * @param finalPosition 结束滑动时的item位置
         */
        void onSelectLastPosition(boolean isFinish, int finalPosition);
    }
}

