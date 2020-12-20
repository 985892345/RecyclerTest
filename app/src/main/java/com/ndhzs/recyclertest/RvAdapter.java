package com.ndhzs.recyclertest;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
                mUnableSelect = 6;

                if (!mMultipleSelected.contains(position)) {//长按一个空的和单选的item
                    mCanDrag = true;
                    mCanAutoScrollUp = false;
                    mIsLongPressMultiSelect = false;
                    WhichViewClicked = TEXT_VIEW;

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
                    mIsLongPressMultiSelect = true;
                    WhichViewClicked = TEXT_VIEW;
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
                    mFirstPosition = position;
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
    private boolean mIsLongPressMultiSelect = false;
    @Override
    public void onSelectChange(int position) {
        //此处position只会传入改变值
        //等于的时候无所谓,因为除了第一次会有等于以外，就不会出现等于
        mIsDragDown = position >= mPreviousPosition;
        mPreviousPosition = position;

        switch (WhichViewClicked) {
            case TEXT_VIEW:
                if (mIsLongPressMultiSelect) {//如果正在长按已经长按选择了的item
                    Toast.makeText(context, "该item已经被长按选择，该功能还未实现！", Toast.LENGTH_SHORT).show();
                }else if (mIsFirstClick) {//记录第一次长按空的或单选的item的位置（单选的item会变成多选）
                    mIsFirstClick = false;
                    mFirstPositions.add(position);
                    mLastPositions.add(position);
                    mMultipleSelected.add(position);
                    mTemporary.add(position);

                    notifyItemChanged(position);
                }else {//空的或单选的item的位置改变（单选的item会变成多选）
                    Log.d(TAG, "onSelectChange: "+position);
                    if (position < mFirstPosition) {//当前位置小于开始位置
                        mCanDrag = false;
                    }else if (mCanDrag && mAllSelected.contains(position)) {//能滑动且当位置被其他已经选择的时候，记录位置
                        mLowerBoundary = position;
                        mCanDrag = false;
                    }else if (position >= mLowerBoundary) {//mMetFirstPosition初始值为极限值，上一个if执行，这个才执行
                        mCanDrag = false;
                    }else {//能滑动的且相邻的空白位置或往回滑动
                        mCanDrag = true;
                        Log.d(TAG, "onSelectChange: true");
                        if (!mTemporary.contains(position)) {//向下滑动增加
                            mTemporary.add(position);
                            mLastPositions.remove(position - 1);
                            mLastPositions.add(position);
                            mMultipleSelected.add(position);

                            notifyItemChanged(position - 1);
                            notifyItemChanged(position);
                        }else if (mTemporary.contains(position) && !mLastPositions.contains(position)) {
                            //当往回滑不是自身结尾位置时（其他已选区域的结尾位置在前面就已经拦截了）
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
            case BUTTON_TOP:
                if (mIsFirstClick) {//第一次长按不处理
                    mIsFirstClick = false;
                }else {
                    mCanDrag = true;
                    if (mIsDragDown) {//向下滑动
                        if (mLastPositions.contains(position)) {//如果向下滑动碰到下边界，记录位置
                            mLowerBoundary = position;
                        }
                        if (position <= mLowerBoundary) {
                            //mMetLastPosition默认值为极限值，当上一个if记录位置后，则向下滑动只能到自身边界结束
                            mCanAutoScrollDown = true;
                            mAllSelected.remove(position - 1);
                            mMultipleSelected.remove(position - 1);
                            mFirstPositions.remove(position - 1);
                            mFirstPositions.add(position);

                            notifyItemChanged(position - 1);
                            notifyItemChanged(position);
                        }else {//当触摸位置在下边界以下
                            mCanDrag = false;
                        }
                    }else {//向上滑动
                        if (mLastPositions.contains(position)) {
                            mUpperBoundary = position;
                        }
                        if (position > mUpperBoundary) {
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
                    }
                }
                break;
            case BUTTON_BOTTOM:
                if (mIsFirstClick) {
                    mIsFirstClick = false;
                }else {
                    mCanDrag = true;
                    if (mIsDragDown) {
                        if (mFirstPositions.contains(position)) {
                            mLowerBoundary = position;
                        }
                        if (position < mLowerBoundary) {
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
                    }else {
                        if (mFirstPositions.contains(position)) {
                            mUpperBoundary = position;
                        }
                        if (position >= mUpperBoundary) {
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
                    }
                }
                break;
        }
    }

    @Override
    public void onSelectLastPosition(boolean isFinish, int theLastPosition) {
        mIsFinish = isFinish;
        mCanDrag = false;
        mIsFirstClick = true;
        mCanAutoScrollUp = true;
        mCanAutoScrollDown = true;
        mIsLongPressMultiSelect = false;
        mUpperBoundary = Integer.MIN_VALUE;
        mLowerBoundary = Integer.MAX_VALUE;

        switch (WhichViewClicked) {
            case TEXT_VIEW:

                mAllSelected.addAll(mTemporary);
                mTemporary.clear();

                theLastPosition = Math.max(theLastPosition, mFirstPosition);
                mMultipleSelectedRange.put(mFirstPosition, theLastPosition);
                break;
            case BUTTON_TOP:
                break;
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
