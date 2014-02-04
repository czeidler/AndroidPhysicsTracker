/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;


public class Calibration {
    interface ICalibrationListener {
        public void onCalibrationChanged();
    }

    private float xCalibration;
    private float yCalibration;

    private PointF origin = new PointF(10, 10);
    private float rotation;
    private boolean swapAxis = false;

    private List<ICalibrationListener> listeners;

    public Calibration() {
        listeners = new ArrayList<ICalibrationListener>();
        xCalibration = 1;
        yCalibration = 1;
    }

    public void addListener(ICalibrationListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(ICalibrationListener listener) {
        return listeners.remove(listener);
    }

    public float getXCalibration() {
        return xCalibration;
    }

    public float getYCalibration() {
        return yCalibration;
    }

    public PointF fromRaw(PointF raw) {
        PointF point = new PointF();
        // translation
        point.x = raw.x - origin.x;
        point.y = raw.y - origin.y;

        // rotation
        float x = point.x;
        float y = point.y;
        point.x = (float)Math.cos(rotation) * x + (float)Math.sin(rotation) * y;
        point.y = (float)Math.cos(rotation) * y - (float)Math.sin(rotation) * x;

        // scale
        point.x *= xCalibration;
        point.y *= yCalibration;
        return point;
    }

    public void setXCalibration(float xCalibration) {
        this.xCalibration = xCalibration;
        notifyCalibrationChanged();
    }

    public void setYCalibration(float yCalibration) {
        this.yCalibration = yCalibration;
        notifyCalibrationChanged();
    }

    public void setCalibration(float xCalibration, float yCalibration) {
        this.xCalibration = xCalibration;
        this.yCalibration = yCalibration;
        notifyCalibrationChanged();
    }

    public void setOrigin(PointF origin, float rotation, boolean swapAxis) {
        this.origin.set(origin);
        this.rotation = rotation;
        this.swapAxis = swapAxis;
        notifyCalibrationChanged();
    }

    public PointF getOrigin() {
        return origin;
    }

    public float getRotation() {
        return rotation;
    }

    public boolean getSwapAxis() {
        return swapAxis;
    }

    private void notifyCalibrationChanged() {
        for (ICalibrationListener listener : listeners)
            listener.onCalibrationChanged();
    }
}


class OriginCalibrationSetter implements MarkersDataModel.IMarkersDataModelListener {
    private Calibration calibration;
    private MarkersDataModel calibrationMarkers;

    public OriginCalibrationSetter(Calibration calibration, MarkersDataModel data) {
        this.calibration = calibration;
        this.calibrationMarkers = data;
        this.calibrationMarkers.addListener(this);

        calibrate();
    }

    static public float getAngle(PointF origin, PointF point) {
        PointF relative = new PointF();
        relative.x = point.x - origin.x;
        relative.y = point.y - origin.y;
        float angle = 90;
        if (relative.x != 0)
            angle = (float)Math.atan(relative.y / relative.x);
        angle = (float)Math.toDegrees((double)angle);
        // choose the right quadrant
        if (relative.x < 0)
            angle = 180 + angle;
        return angle;
    }

    public void setOrigin(PointF origin, float angle, boolean swapAxis) {
        PointF xAxis = new PointF();
        float length = 10;
        xAxis.x = (float)Math.cos(Math.toRadians(angle)) * length;
        xAxis.y = (float)Math.sin(Math.toRadians(angle)) * length;
        xAxis.x += origin.x;
        xAxis.y += origin.y;

        calibration.setOrigin(origin, angle, swapAxis);
        calibrationMarkers.setMarkerPosition(origin, 0);
        calibrationMarkers.setMarkerPosition(xAxis, 1);
    }

    private void calibrate() {
        if (calibrationMarkers.getMarkerCount() != 3)
            return;
        PointF origin = calibrationMarkers.getMarkerDataAt(0).getPosition();
        PointF axis1 = calibrationMarkers.getMarkerDataAt(1).getPosition();

        float angle = getAngle(origin, axis1);

        calibration.setOrigin(origin, angle, false);
    }

    @Override
    public void onDataAdded(MarkersDataModel model, int index) {
        calibrate();
    }

    @Override
    public void onDataRemoved(MarkersDataModel model, int index, MarkerData data) {
        calibrate();
    }

    @Override
    public void onDataChanged(MarkersDataModel model, int index, int number) {
        calibrate();
    }

    @Override
    public void onAllDataChanged(MarkersDataModel model) {
        calibrate();
    }

    @Override
    public void onDataSelected(MarkersDataModel model, int index) {

    }
}

class LengthCalibrationSetter implements MarkersDataModel.IMarkersDataModelListener {
    private Calibration calibration;
    private MarkersDataModel calibrationMarkers;

    private float calibrationValue;

    public LengthCalibrationSetter(Calibration calibration, MarkersDataModel data) {
        this.calibration = calibration;
        this.calibrationMarkers = data;
        this.calibrationMarkers.addListener(this);

        calibrationValue = 1;
        calibrate();
    }

    public void setCalibrationValue(float value) {
        calibrationValue = value;
        calibrate();
    }

    public float getCalibrationValue() {
        return calibrationValue;
    }

    public float scaleLength() {
        PointF point1 = calibrationMarkers.getMarkerDataAt(0).getPosition();
        PointF point2 = calibrationMarkers.getMarkerDataAt(1).getPosition();

        return  (float)Math.sqrt(Math.pow(point1.x - point2.x, 2) +  Math.pow(point1.y - point2.y, 2));
    }

    private void calibrate() {
        if (calibrationMarkers.getMarkerCount() != 2)
            return;
        float value = calibrationValue / scaleLength();
        calibration.setCalibration(value, value);
    }

    @Override
    public void onDataAdded(MarkersDataModel model, int index) {
        calibrate();
    }

    @Override
    public void onDataRemoved(MarkersDataModel model, int index, MarkerData data) {
        calibrate();
    }

    @Override
    public void onDataChanged(MarkersDataModel model, int index, int number) {
        calibrate();
    }

    @Override
    public void onAllDataChanged(MarkersDataModel model) {
        calibrate();
    }

    @Override
    public void onDataSelected(MarkersDataModel model, int index) {

    }
}