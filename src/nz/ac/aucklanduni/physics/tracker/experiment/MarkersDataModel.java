/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.experiment;

import android.graphics.PointF;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


public class MarkersDataModel implements Calibration.ICalibrationListener {
    public interface IMarkersDataModelListener {
        public void onDataAdded(MarkersDataModel model, int index);
        public void onDataRemoved(MarkersDataModel model, int index, MarkerData data);
        public void onDataChanged(MarkersDataModel model, int index, int number);
        public void onAllDataChanged(MarkersDataModel model);
        public void onDataSelected(MarkersDataModel model, int index);
    }

    private List<MarkerData> markerDataList;
    private List<WeakReference<IMarkersDataModelListener>> listeners;
    private int selectedDataIndex = -1;
    private Calibration calibration = null;

    public MarkersDataModel() {
        markerDataList = new ArrayList<MarkerData>();
        listeners = new ArrayList<WeakReference<IMarkersDataModelListener>>();
    }

    /**
     * If calibration is set, listeners get an onAllDataChanged notification when the calibration changed.
     * @param calibration the calibration to use in getCalibratedMarkerPositionAt
     */
    public void setCalibration(Calibration calibration) {
        if (this.calibration != null)
            this.calibration.removeListener(this);
        this.calibration = calibration;
        this.calibration.addListener(this);
        onCalibrationChanged();
    }

    @Override
    public void onCalibrationChanged() {
        notifyDataChanged(0, markerDataList.size());
    }

    public PointF getCalibratedMarkerPositionAt(int index) {
        MarkerData data = getMarkerDataAt(index);
        PointF raw = data.getPosition();
        if (calibration == null)
            return raw;
        return calibration.fromRaw(raw);
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
        notifyDataChanged(index, 1);
    }

    public void addListener(IMarkersDataModelListener listener) {
        listeners.add(new WeakReference<IMarkersDataModelListener>(listener));
    }

    public boolean removeListener(IMarkersDataModelListener listener) {
        return listeners.remove(listener);
    }

    public int addMarkerData(MarkerData data) {
        int i = 0;
        for (; i < markerDataList.size(); i++) {
            MarkerData current = markerDataList.get(i);
            if (current.getRunId() == data.getRunId())
                return -1;
            if (current.getRunId() > data.getRunId())
                break;
        }
        markerDataList.add(i, data);
        notifyDataAdded(i);
        return i;
    }

    public int getMarkerCount() {
        return markerDataList.size();
    }

    public MarkerData getMarkerDataAt(int index) {
        return markerDataList.get(index);
    }

    public int findMarkerDataByRun(int run) {
        for (int i = 0; i < getMarkerCount(); i++) {
            MarkerData data = getMarkerDataAt(i);
            if (data.getRunId() == run)
                return i;
        }
        return -1;
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
        for (ListIterator<WeakReference<IMarkersDataModelListener>> it = listeners.listIterator(); it.hasNext(); ) {
            IMarkersDataModelListener listener = it.next().get();
            if (listener != null)
                listener.onDataAdded(this, index);
            else
                it.remove();
        }
    }

    public void notifyDataRemoved(int index, MarkerData data) {
        for (ListIterator<WeakReference<IMarkersDataModelListener>> it = listeners.listIterator(); it.hasNext(); ) {
            IMarkersDataModelListener listener = it.next().get();
            if (listener != null)
                listener.onDataRemoved(this, index, data);
            else
                it.remove();
        }
    }

    public void notifyDataChanged(int index, int number) {
        for (ListIterator<WeakReference<IMarkersDataModelListener>> it = listeners.listIterator(); it.hasNext(); ) {
            IMarkersDataModelListener listener = it.next().get();
            if (listener != null)
                listener.onDataChanged(this, index, number);
            else
                it.remove();
        }
    }

    public void notifyAllDataChanged() {
        for (ListIterator<WeakReference<IMarkersDataModelListener>> it = listeners.listIterator(); it.hasNext(); ) {
            IMarkersDataModelListener listener = it.next().get();
            if (listener != null)
                listener.onAllDataChanged(this);
            else
                it.remove();
        }
    }

    private void notifyDataSelected(int index) {
        for (ListIterator<WeakReference<IMarkersDataModelListener>> it = listeners.listIterator(); it.hasNext(); ) {
            IMarkersDataModelListener listener = it.next().get();
            if (listener != null)
                listener.onDataSelected(this, index);
            else
                it.remove();
        }
    }
}