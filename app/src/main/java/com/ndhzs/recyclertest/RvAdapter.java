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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RvAdapter extends DragSelectRvAdapter<RvAdapter.MyViewHolder> {

    private static final String TAG = "123";
    private final int dataSize;
    private final Context context;
    private View rootView;
    private boolean mIsAbleToDrag;
    private int mFirstPosition;

    private boolean mIsFinish;

    private HashSet<Integer> mAllSelected;
    private HashSet<Integer> mSingleSelected;
    private HashSet<Integer> mMultipleSelected;
    private HashSet<Integer> mTemporary;
    private HashSet<Integer> mFirstPositions;
    private HashSet<Integer> mLastPositions;

    private HashMap<Integer, Integer> mMultipleSelectedRange;

    private int WhichViewClicked;
    private final int TEXT_VIEW = 0;
    private final int BUTTON_TOP = 1;
    private final int BUTTON_BOTTOM = 2;

    private int test = 0;

    public RvAdapter(int dataSize, Context context) {
        this.dataSize = dataSize;
        this.context = context;
        mAllSelected = new HashSet<>();
        mSingleSelected = new HashSet<>();
        mMultipleSelected = new HashSet<>();
        mTemporary = new HashSet<>();
        mFirstPositions = new HashSet<>();
        mLastPositions = new HashSet<>();

        mMultipleSelectedRange = new HashMap<>();
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
                if (!mMultipleSelected.contains(position)) {
                    mIsAbleToDrag = true;
                    WhichViewClicked = TEXT_VIEW;

                    mFirstPosition = position;

                    //下面的remove是长按后删除单选的item
                    mSingleSelected.remove(position);
                    mAllSelected.remove(position);
                }else {
                    mIsAbleToDrag = true;
                    WhichViewClicked = TEXT_VIEW;
                }

                return true;
            }
        });

        holder.btnTop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mFirstPositions.contains(position)) {
                    mIsAbleToDrag = true;
                    WhichViewClicked = BUTTON_TOP;
                    mFirstPosition = position;
                }
                return mIsAbleToDrag;
            }
        });

        holder.btnBottom.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mLastPositions.contains(position)) {
                    mIsAbleToDrag = true;
                    WhichViewClicked = BUTTON_BOTTOM;
                    mFirstPosition = position;
                }
                return mIsAbleToDrag;
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataSize;
    }

    @Override
    public boolean onIsAbleToDrag() {
        return mIsAbleToDrag;
    }

    @Override
    public int onFirstPosition() {
        return mFirstPosition;
    }

    private int mPreviousPosition = mFirstPosition;
    private boolean mIsDragDown;
    private boolean mIsFirstClick = true;
    private int mOtherFirstPosition = Integer.MAX_VALUE;
    private boolean mIsLongPressMultiSelect = false;
    @Override
    public void onSelectChange(int position) {
        //此处position只会传入改变值
        //等于的时候无所谓,因为除了第一次会有等于以外，就不会出现等于
        mIsDragDown = position >= mPreviousPosition;
        mPreviousPosition = position;

        switch (WhichViewClicked) {
            case TEXT_VIEW:
                if (mIsFirstClick && !mMultipleSelected.contains(position) && !mIsLongPressMultiSelect) {
                    mIsFirstClick = false;
                    mFirstPositions.add(position);
                    mLastPositions.add(position);
                    mMultipleSelected.add(position);
                    mTemporary.add(position);

                    notifyItemChanged(position);
                }else if (mIsLongPressMultiSelect || mIsFirstClick && mMultipleSelected.contains(position)) {
                    mIsLongPressMultiSelect = true;
                    //长按已经多选了的item
                }else {
                    if (position < mFirstPosition) {
                        mIsAbleToDrag = false;
                    }else if (mIsAbleToDrag && mAllSelected.contains(position)) {
                        mOtherFirstPosition = position;
                        mIsAbleToDrag = false;
                    }else if (position >= mOtherFirstPosition) {
                        mIsAbleToDrag = false;
                    }else {
                        mIsAbleToDrag = true;
                        if (!mTemporary.contains(position)) {
                            mTemporary.add(position);
                            mLastPositions.remove(position - 1);
                            mLastPositions.add(position);
                            mMultipleSelected.add(position);

                            notifyItemChanged(position - 1);
                            notifyItemChanged(position);
                        }else if (mTemporary.contains(position)) {
                            mTemporary.remove(position + 1);
                            mLastPositions.remove(position + 1);
                            mLastPositions.add(position);
                            mMultipleSelected.remove(position + 1);

                            notifyItemChanged(position + 1);
                            notifyItemChanged(position);
                        }
                    }
                }
                break;
            case BUTTON_TOP:
                if (mIsFirstClick) {
                    mIsFirstClick = false;
                }else {
                    if (mIsDragDown) {
                        mAllSelected.remove(position - 1);
                        mMultipleSelected.remove(position - 1);
                        mFirstPositions.remove(position - 1);
                        mFirstPositions.add(position);
                        Log.d(TAG, "下remove = "+(position - 1)+"    add = "+position);

                        notifyItemChanged(position - 1);
                    }else {
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mFirstPositions.add(position);
                        mFirstPositions.remove(position + 1);
                        Log.d(TAG, "上remove = "+(position + 1)+"    add = "+position);

                        notifyItemChanged(position + 1);
                    }
                    notifyItemChanged(position);
                }
                break;
            case BUTTON_BOTTOM:
                if (mIsFirstClick) {
                    mIsFirstClick = false;
                }else {
                    if (mIsDragDown) {
                        mAllSelected.add(position);
                        mMultipleSelected.add(position);
                        mLastPositions.add(position);
                        mLastPositions.remove(position - 1);

                        notifyItemChanged(position - 1);
                    }else {
                        mAllSelected.remove(position + 1);
                        mMultipleSelected.remove(position + 1);
                        mLastPositions.remove(position + 1);
                        mLastPositions.add(position);

                        notifyItemChanged(position + 1);
                    }
                    notifyItemChanged(position);
                }
                break;
        }

    }

    @Override
    public void onSelectLastPosition(boolean isFinish, int theLastPosition) {
        mIsFinish = isFinish;
        mIsAbleToDrag = false;
        mIsFirstClick = true;

        switch (WhichViewClicked) {
            case TEXT_VIEW:
                mIsLongPressMultiSelect = false;

                mAllSelected.addAll(mTemporary);
                mTemporary.clear();

                mOtherFirstPosition = Integer.MAX_VALUE;

                theLastPosition = Math.max(theLastPosition, mFirstPosition);
                mMultipleSelectedRange.put(mFirstPosition, theLastPosition);
                break;
            case BUTTON_TOP:
            case BUTTON_BOTTOM:
                break;
        }
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
