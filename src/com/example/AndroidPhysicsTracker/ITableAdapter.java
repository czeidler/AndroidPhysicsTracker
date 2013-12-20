package com.example.AndroidPhysicsTracker;

import android.content.Context;
import android.view.View;

public interface ITableAdapter<T> {
    public int getRowCount();

    public int getColumnWeight(int column);
    public int getColumnCount();

    public T getRow(int row);
    public View getView(Context context, int row, int column);

    public int addRow(T row);
    public void removeRow(int row);

    public void addListener(ITableAdapterListener listener);

    public interface ITableAdapterListener {
        public void onRowAdded(ITableAdapter<?> table, int row);
        public void onRowRemoved(ITableAdapter<?> table, int row);
        public void onRowUpdated(ITableAdapter<?> table, int row);
    }
}

