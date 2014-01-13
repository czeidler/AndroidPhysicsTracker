package com.example.AndroidPhysicsTracker;


import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;


class MarkerData {
    private int runId;
    private PointF positionReal;

    public MarkerData(int run) {
        runId = run;
        positionReal = new PointF();
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public PointF getPosition() {
        return positionReal;
    }

    public void setPosition(PointF positionReal) {
        this.positionReal.set(positionReal);
    }
}

public class MarkersDataModel {
    interface IMarkersDataModelListener {
        public void onDataAdded(MarkersDataModel model, int index);
        public void onDataRemoved(MarkersDataModel model, int index, MarkerData data);
        public void onDataChanged(MarkersDataModel model, int index);
        public void onAllDataChanged(MarkersDataModel model);
        public void onDataSelected(MarkersDataModel model, int index);
    }

    private List<MarkerData> markerDataList;
    private List<IMarkersDataModelListener> listeners;
    private int selectedDataIndex = -1;

    public MarkersDataModel() {
        markerDataList = new ArrayList<MarkerData>();
        listeners = new ArrayList<IMarkersDataModelListener>();
    }

    public void selectMarkerData(int index) {
        selectedDataIndex = index;
        notifyDataSelected(index);
    }

    public int getSelectedMarkerData() {
        return selectedDataIndex;
    }

    public void setMarkerPosition(PointF position, int index) {
        MarkerData data = getMarkerDataAt(index);
        data.setPosition(position);
        notifyDataChanged(index);
    }

    public void addListener(IMarkersDataModelListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(IMarkersDataModelListener listener) {
        return listeners.remove(listener);
    }

    public boolean addMarkerData(MarkerData data) {
        int i = 0;
        for (; i < markerDataList.size(); i++) {
            MarkerData current = markerDataList.get(i);
            if (current.getRunId() > data.getRunId())
                break;
            if (current.getRunId() == data.getRunId())
                return false;
        }
        markerDataList.add(i, data);
        notifyDataAdded(i);
        return true;
    }

    public int getMarkerCount() {
        return markerDataList.size();
    }

    public MarkerData getMarkerDataAt(int index) {
        return markerDataList.get(index);
    }

    public MarkerData removeMarkerData(int index) {
        MarkerData data = markerDataList.remove(index);
        notifyDataRemoved(index, data);
        return data;
    }

    public void clear() {
        markerDataList.clear();
        notifyAllDataChanged();
    }

    public void notifyDataAdded(int index) {
        for (IMarkersDataModelListener listener : listeners)
            listener.onDataAdded(this, index);
    }

    public void notifyDataRemoved(int index, MarkerData data) {
        for (IMarkersDataModelListener listener : listeners)
            listener.onDataRemoved(this, index, data);
    }

    public void notifyDataChanged(int index) {
        for (IMarkersDataModelListener listener : listeners)
            listener.onDataChanged(this, index);
    }

    public void notifyAllDataChanged() {
        for (IMarkersDataModelListener listener : listeners)
            listener.onAllDataChanged(this);
    }

    private void notifyDataSelected(int index) {
        for (IMarkersDataModelListener listener : listeners)
            listener.onDataSelected(this, index);
    }
}