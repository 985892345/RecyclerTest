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
    private int mStartPosition, mFirstPosition, mLastPosition, mPreFirstP, mPreLastP;

    private HashSet<Integer> mAllSelected;
    private HashSet<Integer> mSingleSelected;
    private HashSet<Integer> mMultipleSelected;
    private HashSet<Integer> mFirstPositions;
    private HashSet<Integer> mLastPositions;

    private HashMap<Integer, Integer> mFirstToLast;
    private HashMap<Integer, Integer> mLastToFirst;

    private int mUpperBoundary = Integer.MIN_VALUE;
    private int mLowerBoundary = Integer.MAX_VALUE;

    private int WhichViewClicked;
    private final int TEXT_DO = 0;
    private final int TEXT_DONE = -1;
    private final int BUTTON_TOP = 1;
    private final int BUTTON_BOTTOM = 2;

    public RvAdapter(int dataSize, Context context) {
        this.dataSize = dataSize;
        this.context = context;
        mAllSelected = new HashSet<>();
        mSingleSelected = new HashSet<>();
        mMultipleSelected = new HashSet<>();
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

                        holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_single_selected_position));
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
                    mFirstToLast.remove(position);
                    mLastToFirst.remove(position);

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

                    mStartPosition = position;
                    mFirstPosition = mStartPosition;
                    
                    mFirstPositions.add(position);
                    mLastPositions.add(position);
                    mMultipleSelected.add(position);
                    mAllSelected.add(position);

                    notifyItemChanged(position);

                    //下面的remove是长按后删除单选的item
                    if (mSingleSelected.remove(position)) {
                        mAllSelected.remove(position);
                    }
                }else {//长按多选的item
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    mCanAutoScrollDown = false;
                    WhichViewClicked = TEXT_DONE;
                    mStartPosition = position;

                    for (int lastPosition : mFirstToLast.keySet()) {
                        if (mStartPosition >= lastPosition && mStartPosition <= mFirstToLast.get(lastPosition)) {
                            Log.d(TAG, "lastPosition = "+lastPosition+"    value = "+mFirstToLast.get(lastPosition));
                            mPreFirstP = lastPosition;
                            mFirstPosition = lastPosition;
                            mLastPosition = mFirstToLast.get(lastPosition);
                            mPreLastP = mLastPosition;
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
                    mStartPosition = position;
                    mFirstPosition = mStartPosition;
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
                    mStartPosition = position;
                    mLastPosition = mStartPosition;
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
    public int onStartPosition() {
        return mStartPosition;
    }

    private int mUnableSelect;
    @Override
    public int onGetUnableSelectRange() {
        return mUnableSelect;
    }

    @Override
    public void onSelectChange(int position, boolean isDragDown) {

        Log.d(TAG, "******************传过来的position = "+position);

        switch (WhichViewClicked) {
            case TEXT_DO:
                if (position < mFirstPosition) {//当前位置小于开始位置
                    mCanDrag = false;
                }else if (isDragDown && position < mLowerBoundary && position != mFirstPosition) {//向下滑动增加
                    Log.d(TAG, " ");
                    Log.d(TAG, "向下滑");
                    Log.d(TAG, "单一：删掉Last第"+(position-1));
                    Log.d(TAG, "单一：增加Last第"+position);
                    if (mAllSelected.contains(position) && mLowerBoundary == Integer.MAX_VALUE) {//当滑动到其他已经选择的区域，记录下边界位置
                        mLowerBoundary = position;
                        mCanDrag = false;
                    }else {
                        mCanDrag = true;
                        mLastPositions.remove(position - 1);
                        mLastPositions.add(position);
                        mMultipleSelected.add(position);
                        mAllSelected.add(position);

                        notifyItemChanged(position - 1);
                        notifyItemChanged(position);
                    }
                }else if (!isDragDown && position < mLowerBoundary - 1) {
                    //当往回滑不是自身结尾位置时（防止从其他区域往回滑而刷新）
                    Log.d(TAG, " ");
                    Log.d(TAG, "向上滑");
                    Log.d(TAG, "单一：删掉Last第"+position+1);
                    Log.d(TAG, "单一：增加Last第"+position);
                    mCanDrag = true;
                    mCanAutoScrollUp = true;
                    mLastPositions.remove(position + 1);
                    mLastPositions.add(position);
                    mMultipleSelected.remove(position + 1);
                    mAllSelected.remove(position + 1);

                    notifyItemChanged(position + 1);
                    notifyItemChanged(position);
                }else {
                    Log.d(TAG, "单一：Stop!!!   当前触摸位置为"+position);
                    mCanDrag = false;
                }
                break;
            case TEXT_DONE:
                if (isDragDown && mLastPosition + 1 < mLowerBoundary) {
                    Log.d(TAG, " ");
                    Log.d(TAG, "向下滑");
                    if (mAllSelected.contains(mLastPosition + 1) && mLowerBoundary == Integer.MAX_VALUE) {
                        mLowerBoundary = mLastPosition + 1;
                        Log.d(TAG, "整体：下边界 = "+mLowerBoundary);
                        mCanDrag = false;
                    }else {
                        Log.d(TAG, "整体：首位置由"+mFirstPosition+"————>"+(mFirstPosition + 1));
                        Log.d(TAG, "整体：末位置由"+mLastPosition+"————>"+(mLastPosition + 1));
                        mFirstPosition += 1;
                        mLastPosition += 1;
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
                        notifyItemChanged(mLastPosition - 1);
                    }
                }else if (!isDragDown &&  mFirstPosition - 1 > mUpperBoundary){
                    Log.d(TAG, " ");
                    Log.d(TAG, "向上滑");
                    if (mAllSelected.contains(mFirstPosition - 1) && mUpperBoundary == Integer.MIN_VALUE) {
                        mUpperBoundary = mFirstPosition - 1;
                        Log.d(TAG, "整体：上边界 = "+mUpperBoundary);
                        mCanDrag = false;
                    }else {
                        Log.d(TAG, "整体：首位置由"+mFirstPosition+"————>"+(mFirstPosition - 1));
                        Log.d(TAG, "整体：末位置由"+mLastPosition+"————>"+(mLastPosition - 1));
                        mFirstPosition -= 1;
                        mLastPosition -= 1;
                        mCanDrag = true;
                        mCanAutoScrollUp = true;
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
                    }
                }else {
                    Log.d(TAG, "整体：Stop!!!   当前首位置为"+mFirstPosition+"    当前末位置为"+mLastPosition);
                    mCanDrag = false;
                }
                break;
            case BUTTON_TOP:
                if (position > mLastPosition) {
                    mCanDrag = false;
                }else if (!isDragDown && position > mUpperBoundary  && position != mLastPosition) {
                    Log.d(TAG, " ");
                    Log.d(TAG, "向上滑");
                    if (mAllSelected.contains(position) && mUpperBoundary == Integer.MIN_VALUE) {
                        mUpperBoundary = position;
                        Log.d(TAG, "上TOP：上边界 = "+mUpperBoundary);
                        mCanDrag = false;
                    }else {
                        Log.d(TAG, "上TOP：上滑到"+position);
                        mCanDrag = true;
                        mCanAutoScrollUp = true;
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mFirstPositions.add(position);
                        mFirstPositions.remove(position + 1);

                        notifyItemChanged(position + 1);
                        notifyItemChanged(position);
                    }
                }else if (isDragDown && position > mUpperBoundary + 1) {
                    Log.d(TAG, " ");
                    Log.d(TAG, "向下滑");
                    Log.d(TAG, "上TOP：下滑到"+position+"     mUpper = "+mUpperBoundary);
                    mCanDrag = true;
                    mCanAutoScrollDown = true;
                    mAllSelected.remove(position - 1);
                    mMultipleSelected.remove(position - 1);
                    mFirstPositions.remove(position - 1);
                    mFirstPositions.add(position);

                    notifyItemChanged(position - 1);
                    notifyItemChanged(position);
                }else {
                    Log.d(TAG, "上TOP：Stop!!!   当前触摸位置为"+position);
                    mCanDrag = false;
                }
                break;
            case BUTTON_BOTTOM:
                if (position < mFirstPosition) {
                    mCanDrag = false;
                }else if (isDragDown && position < mLowerBoundary && position != mFirstPosition) {
                    Log.d(TAG, " ");
                    Log.d(TAG, "向下滑");
                    if (mAllSelected.contains(position) && mLowerBoundary == Integer.MAX_VALUE) {
                        mLowerBoundary = position;
                        Log.d(TAG, "下BOT：下边界 = "+mLowerBoundary);
                        mCanDrag = false;
                    }else {
                        Log.d(TAG, "下BOT：下滑到"+position+"     mLower = "+mLowerBoundary);
                        mCanDrag = true;
                        mCanAutoScrollDown = true;
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mLastPositions.remove(position - 1);
                        mLastPositions.add(position);
                        
                        notifyItemChanged(position - 1);
                        notifyItemChanged(position);
                    }
                }else if (!isDragDown && position < mLowerBoundary - 1) {
                    Log.d(TAG, " ");
                    Log.d(TAG, "向上滑");
                    Log.d(TAG, "下BOT：上滑到"+position);
                    mCanDrag = true;
                    mCanAutoScrollUp = true;
                    mAllSelected.remove(position + 1);
                    mMultipleSelected.remove(position + 1);
                    mLastPositions.remove(position + 1);
                    mLastPositions.add(position);

                    notifyItemChanged(position + 1);
                    notifyItemChanged(position);
                }else {
                    Log.d(TAG, "下BOT：Stop!!!   当前触摸位置为"+position);
                    mCanDrag = false;
                }
                break;
        }
    }

    @Override
    public void onSelectLastPosition(int finalPosition) {
        mCanDrag = false;
        mCanAutoScrollUp = true;
        mCanAutoScrollDown = true;

        switch (WhichViewClicked) {
            case TEXT_DO:
                finalPosition = Math.min(finalPosition, mLowerBoundary - 1);//落在下边界及以下
                finalPosition = Math.max(finalPosition, mFirstPosition);//落在首位及以上
                Log.d(TAG, " ");
                Log.d(TAG, "*****单一");
                Log.d(TAG, "放进第"+mFirstPosition+"与"+finalPosition);
                mFirstToLast.put(mFirstPosition, finalPosition);
                mLastToFirst.put(finalPosition, mFirstPosition);
                break;
            case TEXT_DONE:
                Log.d(TAG, " ");
                Log.d(TAG, "*****整体");
                Log.d(TAG, "删掉第"+mPreFirstP+"与"+mPreLastP);
                Log.d(TAG, "放进第"+mFirstPosition+"与"+mLastPosition);
                mFirstToLast.remove(mPreFirstP);
                mFirstToLast.put(mFirstPosition, mLastPosition);
                mLastToFirst.remove(mPreLastP);
                mLastToFirst.put(mLastPosition, mFirstPosition);
                break;
            case BUTTON_TOP:
                finalPosition = Math.max(finalPosition, mUpperBoundary + 1);//落在上边界及以上
                finalPosition = Math.min(finalPosition, mLastPosition);//落在末位及以下
                Log.d(TAG, " ");
                Log.d(TAG, "*****上按钮");
                Log.d(TAG, "首位由"+mFirstPosition+"————>"+finalPosition);
                mFirstToLast.remove(mFirstPosition);
                mFirstToLast.put(finalPosition, mLastPosition);
                mLastToFirst.put(mLastPosition, finalPosition);
                break;
            case BUTTON_BOTTOM:
                finalPosition = Math.min(finalPosition, mLowerBoundary - 1);//落在下边界及以下
                finalPosition = Math.max(finalPosition, mFirstPosition);//落在首位及以上
                Log.d(TAG, " ");
                Log.d(TAG, "*****下按钮");
                Log.d(TAG, "末位由"+mLastPosition+"————>"+finalPosition);
                mFirstToLast.put(mFirstPosition, finalPosition);
                mLastToFirst.remove(mLastPosition);
                mLastToFirst.put(finalPosition, mFirstPosition);
                break;
        }
        Log.d(TAG, mFirstToLast.toString());
        mUpperBoundary = Integer.MIN_VALUE;
        mLowerBoundary = Integer.MAX_VALUE;
    }

    public HashMap<Integer, Integer> getFirstToLast() {
        return mFirstToLast;
    }

    private void refreshUI(MyViewHolder holder, int position) {

        boolean isFirst = mFirstPositions.contains(position);
        boolean isLast = mLastPositions.contains(position);
        if (isFirst && isLast) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_single_selected_position));
            holder.vTop.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnTop.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_btn_top));
            holder.vBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnBottom.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_btn_bottom));
            holder.vLine.setBackgroundColor(Color.TRANSPARENT);
        }else if (isFirst) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_first_position));
            holder.vInsideBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
            holder.vTop.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnTop.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_btn_top));
        }else if (isLast) {
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_last_position));
            holder.vInsideTop.setBackgroundColor(ContextCompat.getColor(context, R.color.selected));
            holder.vBottom.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow));
            holder.btnBottom.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_btn_bottom));
            holder.vLine.setBackgroundColor(Color.TRANSPARENT);
        }else if (mMultipleSelected.contains(position)){
            holder.layoutTask.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_item_inside_position));
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
