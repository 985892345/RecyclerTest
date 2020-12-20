package com.ndhzs.recyclertest;

import androidx.recyclerview.widget.RecyclerView;

public abstract class DragSelectRvAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements DragSelectTouchListener.OnDragSelectListener {
}
