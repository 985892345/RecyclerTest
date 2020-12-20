package com.ndhzs.recyclertest;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.HashSet;

public class RvAdapter extends DragSelectRvAdapter<RvAdapter.MyViewHolder> {

    private static final String TAG = "123";
    private final int dataSize;
    private final Context context;
    private View rootView;
    private boolean mCanDrag;
    private int mFirstPosition, mLastPosition;

    private boolean mIsFinish;

    private HashSet<Integer> mAllSelected;
    private HashSet<Integer> mSingleSelected;
    private HashSet<Integer> mMultipleSelected;
    private HashSet<Integer> mTemporary;
    private HashSet<Integer> mFirstPositions;
    private HashSet<Integer> mLastPositions;

    private HashMap<Integer, Integer> mFirstToLast;
    private HashMap<Integer, Integer> mLastToFirst;

    private int WhichViewClicked;
    private final int TEXT_DO = 0;
    private final int TEXT_DONE = -1;
    private final int BUTTON_TOP = 1;
    private final int BUTTON_BOTTOM = 2;

    private int mTextDoneFirstClick;

    public RvAdapter(int dataSize, Context context) {
        this.dataSize = dataSize;
        this.context = context;
        mAllSelected = new HashSet<>();
        mSingleSelected = new HashSet<>();
        mMultipleSelected = new HashSet<>();
        mTemporary = new HashSet<>();
        mFirstPositions = new HashSet<>();
        mLastPositions = new HashSet<>();

        mFirstToLast = new HashMap<>();
        mLastToFirst = new HashMap<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_rv_item, parent, false);

        return new MyViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.tvTime.setText(String.valueOf(position));

        restoreUI(holder);
        refreshUI(holder, position);
        refreshClickListener(holder, position);

        holder.tvTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMultipleSelected.contains(position)) {
                    if (!mSingleSelected.contains(position)) {
                        mSingleSelected.add(position);
                        mAllSelected.add(position);

                        holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.item_single_selected_position));
                        holder.vLine.setBackgroundColor(Color.TRANSPARENT);
                    }else {
                        mSingleSelected.remove(position);
                        mAllSelected.remove(position);
                        restoreUI(holder);
                    }
                }
                if (mFirstPositions.contains(position) && mLastPositions.contains(position)) {

                    mAllSelected.remove(position);
                    mMultipleSelected.remove(position);
                    mFirstPositions.remove(position);
                    mLastPositions.remove(position);

                    restoreUI(holder);
                }
            }
        });

        holder.tvTask.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mUnableSelect = 6;

                if (!mMultipleSelected.contains(position)) {//长按一个空的和单选的item
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    WhichViewClicked = TEXT_DO;

                    mFirstPosition = position;

                    //下面的remove是长按后删除单选的item
                    if (mSingleSelected.contains(position)) {
                        mSingleSelected.remove(position);
                        mAllSelected.remove(position);
                    }
                }else {//长按多选的item
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    mCanAutoScrollDown = false;
                    WhichViewClicked = TEXT_DONE;
                    mTextDoneFirstClick = position;
                    Log.d(TAG, position + "    "+mFirstToLast.toString());
                    for (int key : mFirstToLast.keySet()) {
                        if (position >= key && position <= mFirstToLast.get(key)) {
                            Log.d(TAG, "key = "+key+"    value = "+mFirstToLast.get(key));
                            mFirstPosition = key;
                            mLastPosition = mFirstToLast.get(key);
                            break;
                        }
                    }
                }

                return true;
            }
        });

        holder.btnTop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mUnableSelect = 12;

                if (mFirstPositions.contains(position)) {
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    mCanAutoScrollDown = false;
                    WhichViewClicked = BUTTON_TOP;
                    mFirstPosition = position;
                    mLastPosition = mFirstToLast.get(mFirstPosition);
                }
                return mCanDrag;
            }
        });

        holder.btnBottom.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mUnableSelect = 12;

                if (mLastPositions.contains(position)) {
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    mCanAutoScrollDown = false;
                    WhichViewClicked = BUTTON_BOTTOM;
                    mLastPosition = position;
                    mFirstPosition = mLastToFirst.get(mLastPosition);
                }
                return mCanDrag;
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSize;
    }

    @Override
    public boolean onCanDrag() {
        return mCanDrag;
    }

    private boolean mCanAutoScrollUp = true;
    @Override
    public boolean onCanAutoScrollUp() {
        return mCanAutoScrollUp;
    }

    private boolean mCanAutoScrollDown = true;
    @Override
    public boolean onCanAutoScrollDown() {
        return mCanAutoScrollDown;
    }

    @Override
    public int onFirstPosition() {
        return mFirstPosition;
    }

    private int mUnableSelect;
    @Override
    public int onGetUnableSelectRange() {
        return mUnableSelect;
    }

    private int mPreviousPosition = mFirstPosition;
    private boolean mIsDragDown;
    private boolean mIsFirstClick = true;
    private int mUpperBoundary = Integer.MIN_VALUE;
    private int mLowerBoundary = Integer.MAX_VALUE;
    @Override
    public void onSelectChange(int position) {
        //此处position只会传入改变值
        //等于的时候无所谓,因为除了第一次会有等于以外，就不会出现等于
        mIsDragDown = position >= mPreviousPosition;
        mPreviousPosition = position;

        switch (WhichViewClicked) {
            case TEXT_DO:
                if (mIsFirstClick) {//记录第一次长按空的或单选的item的位置（单选的item会变成多选）
                    mIsFirstClick = false;
                    mFirstPositions.add(position);
                    mLastPositions.add(position);
                    mMultipleSelected.add(position);
                    mTemporary.add(position);

                    notifyItemChanged(position);
                }else {//空的或单选的item的位置改变（单选的item会变成多选）
                    if (position < mFirstPosition) {//当前位置小于开始位置
                        mCanDrag = false;
                    }else if (mCanDrag && mAllSelected.contains(position)) {//能滑动且当滑动到其他已经选择的区域，记录下边界位置
                        mLowerBoundary = position;
                        mCanDrag = false;
                    }else if (position < mLowerBoundary){//能滑动的且相邻的空白位置或往回滑动
                        mCanDrag = true;
                        if (!mTemporary.contains(position)) {//向下滑动增加
                            mTemporary.add(position);
                            mLastPositions.remove(position - 1);
                            mLastPositions.add(position);
                            mMultipleSelected.add(position);

                            notifyItemChanged(position - 1);
                            notifyItemChanged(position);
                        }else if (mTemporary.contains(position) && !mLastPositions.contains(position)) {
                            //当往回滑不是自身结尾位置时（防止从其他区域往回滑而刷新）
                            mCanAutoScrollUp = true;
                            mLastPositions.add(position);
                            mTemporary.remove(position + 1);
                            mLastPositions.remove(position + 1);
                            mMultipleSelected.remove(position + 1);

                            notifyItemChanged(position + 1);
                            notifyItemChanged(position);
                        }
                    }
                }
                break;
            case TEXT_DONE:
                if (mIsFirstClick) {
                    mIsFirstClick = false;
                }else {
                    if (mIsDragDown && mLastPosition + 1 < mLowerBoundary) {
                        mFirstPosition += 1;
                        mLastPosition += 1;
                        if ((mLastPositions.contains(mLastPosition) || mSingleSelected.contains(mLastPosition)) && mLowerBoundary == Integer.MAX_VALUE) {
                            mLowerBoundary = mLastPosition;
                            mCanDrag = false;
                        }else if (mLastPosition < mLowerBoundary) {
                            mCanDrag = true;
                            mCanAutoScrollDown = true;
                            mAllSelected.remove(mFirstPosition - 1);
                            mAllSelected.add(mLastPosition);
                            mMultipleSelected.remove(mFirstPosition - 1);
                            mMultipleSelected.add(mLastPosition);
                            mFirstPositions.remove(mFirstPosition - 1);
                            mFirstPositions.add(mFirstPosition);
                            mLastPositions.remove(mLastPosition - 1);
                            mLastPositions.add(mLastPosition);

                            notifyItemChanged(mFirstPosition);
                            notifyItemChanged(mFirstPosition - 1);
                            notifyItemChanged(mLastPosition);
                            notifyItemChanged(mFirstPosition - 1);
                        }else {
                            mCanDrag = false;
                        }
                    }else if (!mIsDragDown && mFirstPosition - 1 > mUpperBoundary) {
                        mFirstPosition -= 1;
                        mLastPosition -= 1;
                        if ((mLastPositions.contains(mFirstPosition) || mSingleSelected.contains(mFirstPosition)) && mUpperBoundary == Integer.MIN_VALUE) {
                            mUpperBoundary = mFirstPosition;
                            mCanDrag = false;
                        }else if (mFirstPosition > mUpperBoundary) {
                            mCanDrag = true;
                            mCanAutoScrollDown = true;
                            mAllSelected.remove(mLastPosition + 1);
                            mAllSelected.add(mFirstPosition);
                            mMultipleSelected.remove(mLastPosition + 1);
                            mMultipleSelected.add(mFirstPosition);
                            mFirstPositions.remove(mFirstPosition + 1);
                            mFirstPositions.add(mFirstPosition);
                            mLastPositions.remove(mLastPosition + 1);
                            mLastPositions.add(mLastPosition);

                            notifyItemChanged(mFirstPosition);
                            notifyItemChanged(mFirstPosition + 1);
                            notifyItemChanged(mLastPosition);
                            notifyItemChanged(mLastPosition + 1);
                        }else {
                            mCanDrag = false;
                        }
                    }
                }
                break;
            case BUTTON_TOP:
                if (mIsFirstClick) {//第一次长按不处理
                    mIsFirstClick = false;
                }else if (!mIsDragDown && position > mUpperBoundary && position < mLastPosition) {
                    if ((mLastPositions.contains(position) || mSingleSelected.contains(position)) && mUpperBoundary == Integer.MIN_VALUE) {
                        mUpperBoundary = position;
                    }
                    if (position > mUpperBoundary) {
                        mCanDrag = true;
                        mCanAutoScrollUp = true;
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mFirstPositions.add(position);
                        mFirstPositions.remove(position + 1);

                        notifyItemChanged(position + 1);
                        notifyItemChanged(position);
                    }else {
                        mCanDrag = false;
                    }
                }else if (mIsDragDown && position > mUpperBoundary && position <= mLastPosition && !mFirstPositions.contains(position)) {
                    mCanDrag = true;
                    mCanAutoScrollDown = true;
                    mAllSelected.remove(position - 1);
                    mMultipleSelected.remove(position - 1);
                    mFirstPositions.remove(position - 1);
                    mFirstPositions.add(position);

                    notifyItemChanged(position - 1);
                    notifyItemChanged(position);
                }else {
                    mCanDrag = false;
                }
                break;
            case BUTTON_BOTTOM:
                if (mIsFirstClick) {
                    mIsFirstClick = false;
                }else if (mIsDragDown && position > mFirstPosition && position < mLowerBoundary) {
                    if ((mFirstPositions.contains(position) || mSingleSelected.contains(position)) && mLowerBoundary == Integer.MAX_VALUE) {
                        mLowerBoundary = position;
                    }
                    if (position < mLowerBoundary) {
                        mCanDrag = true;
                        mCanAutoScrollDown = true;
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mLastPositions.add(position);
                        mLastPositions.remove(position - 1);

                        notifyItemChanged(position - 1);
                        notifyItemChanged(position);
                    }else {
                        mCanDrag = false;
                    }
                }else if (!mIsDragDown && position >= mFirstPosition && position < mLowerBoundary && !mLastPositions.contains(position)) {
                    mCanDrag = true;
                    mCanAutoScrollUp = true;
                    mAllSelected.remove(position + 1);
                    mMultipleSelected.remove(position + 1);
                    mLastPositions.remove(position + 1);
                    mLastPositions.add(position);

                    notifyItemChanged(position + 1);
                    notifyItemChanged(position);
                }else {
                    mCanDrag = false;
                }
                break;

        }
    }

    @Override
    public void onSelectLastPosition(boolean isFinish, int finalPosition) {
        mIsFinish = isFinish;
        mCanDrag = false;
        mIsFirstClick = true;
        mCanAutoScrollUp = true;
        mCanAutoScrollDown = true;

        switch (WhichViewClicked) {
            case TEXT_DO:
                mAllSelected.addAll(mTemporary);
                mTemporary.clear();

                finalPosition = Math.max(finalPosition, mFirstPosition);
                mFirstToLast.put(mFirstPosition, finalPosition);
                mLastToFirst.put(finalPosition, mFirstPosition);
                break;
            case TEXT_DONE:
                int PreFirstPosition = mFirstPosition + mTextDoneFirstClick - finalPosition;
                int PreLastPosition = mLastPosition + mTextDoneFirstClick - finalPosition;
                mFirstToLast.remove(PreFirstPosition);
                mFirstToLast.put(mFirstPosition, mLastPosition);
                mLastToFirst.remove(PreLastPosition);
                mLastToFirst.put(mLastPosition, mFirstPosition);
                break;
            case BUTTON_TOP:
                finalPosition = Math.max(finalPosition, mUpperBoundary + 1);
                mFirstToLast.remove(mFirstPosition);
                mFirstToLast.put(finalPosition, mLastPosition);
                mLastToFirst.put(mLastPosition, finalPosition);
                break;
            case BUTTON_BOTTOM:
                finalPosition = Math.min(finalPosition, mLowerBoundary - 1);
                mFirstToLast.put(mFirstPosition, finalPosition);
                mLastToFirst.remove(mLastPosition);
                mLastToFirst.put(finalPosition, mFirstPosition);
                break;
        }
        mUpperBoundary = Integer.MIN_VALUE;
        mLowerBoundary = Integer.MAX_VALUE;
    }

    public HashMap<Integer, Integer> getMBeforeToAfter() {
        return mFirstToLast;
    }

    private void refreshUI(MyViewHolder holder, int position) {

        boolean isFirst = mFirstPositions.contains(position);
        boolean isLast = mLastPositions.contains(position);
        if (isFirst && isLast) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.item_single_selected_position));
            holder.vTop.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnTop.setBackground(ContextCompat.getDrawable(context, R.drawable.item_btn_top));
            holder.vBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnBottom.setBackground(ContextCompat.getDrawable(context, R.drawable.item_btn_bottom));
            holder.vLine.setBackgroundColor(Color.TRANSPARENT);
        }else if (isFirst) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.item_first_position));
            holder.vInsideBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
            holder.vTop.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnTop.setBackground(ContextCompat.getDrawable(context, R.drawable.item_btn_top));
        }else if (isLast) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.item_last_position));
            holder.vInsideTop.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
            holder.vBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnBottom.setBackground(ContextCompat.getDrawable(context, R.drawable.item_btn_bottom));
            holder.vLine.setBackgroundColor(Color.TRANSPARENT);
        }else if (mMultipleSelected.contains(position)){
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.item_inside_position));
            holder.vInsideTop.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
            holder.vInsideBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
        }else {
            restoreUI(holder);
        }
    }

    private void refreshClickListener(MyViewHolder holder, int position) {

    }

    private void restoreUI(MyViewHolder holder) {
        holder.layoutTask.setBackgroundColor(ContextCompat.getColor(context, R.color.littleGray));
        holder.vLine.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));
        holder.vInsideTop.setBackgroundColor(Color.TRANSPARENT);
        holder.vInsideBottom.setBackgroundColor(Color.TRANSPARENT);
        holder.vTop.setBackgroundColor(Color.TRANSPARENT);
        holder.vBottom.setBackgroundColor(Color.TRANSPARENT);
        holder.btnTop.setBackgroundColor(Color.TRANSPARENT);
        holder.btnBottom.setBackgroundColor(Color.TRANSPARENT);
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutTask;
        TextView tvTime;
        TextView tvTask;
        Button btnTop;
        Button btnBottom;
        View vTop;
        View vBottom;
        View vInsideTop;
        View vInsideBottom;
        View vLine;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutTask = itemView.findViewById(R.id.layout_task);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTask = itemView.findViewById(R.id.tv_task);
            btnTop = itemView.findViewById(R.id.btn_first_top);
            btnBottom = itemView.findViewById(R.id.btn_last_bottom);
            vTop = itemView.findViewById(R.id.view_first_top);
            vBottom = itemView.findViewById(R.id.view_last_bottom);
            vInsideTop = itemView.findViewById(R.id.view_inside_top);
            vInsideBottom = itemView.findViewById(R.id.view_inside_bottom);
            vLine = itemView.findViewById(R.id.view_line);
        }
    }
}
