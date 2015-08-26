/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.graphics.PointF;
import android.os.Bundle;

import nz.ac.auckland.lablet.data.Data;
import nz.ac.auckland.lablet.data.PointDataList;


/**
 * Class to manage the length calibration scale.
 * <p>
 * <ol>
 * <li>
 * Monitors the scale marker model and re-calibrates when there are changes. For example, when the user moved the scale.
 * </li>
 * <li>
 * One can set the length of the calibration scale and LengthCalibrationSetter automatically sets the x and y scale
 * values of the {@link CalibrationXY} class. For example, the length has been
 * updated in the calibration dialog.
 * </li>
 * </ol>
 * </p>
 */
public class LengthCalibrationSetter {
    private CalibrationXY calibrationXY;
    private PointDataList calibrationMarkers;

    private float calibrationValue;

    private PointDataList.IListener dataListener = new PointDataList.IListener<PointDataList>() {
        @Override
        public void onDataAdded(PointDataList model, int index) {
            calibrate();
        }

        @Override
        public void onDataRemoved(PointDataList model, int index, Data data) {
            calibrate();
        }

        @Override
        public void onDataChanged(PointDataList model, int index, int number) {
            calibrate();
        }

        @Override
        public void onAllDataChanged(PointDataList model) {
            calibrate();
        }

        @Override
        public void onDataSelected(PointDataList model, int index) {

        }
    };

    public LengthCalibrationSetter(PointDataList lengthCalibrationMarkers, CalibrationXY calibrationXY) {
        this.calibrationMarkers = lengthCalibrationMarkers;
        this.calibrationMarkers.addListener(dataListener);
        this.calibrationXY = calibrationXY;

        calibrationValue = 1;
        calibrate();
    }

    public void setCalibrationValue(float value) {
        calibrationValue = value;
        calibrate();
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        PointF point1 = calibrationMarkers.getDataAt(0).getPosition();
        PointF point2 = calibrationMarkers.getDataAt(1).getPosition();
        bundle.putFloat("lengthCalibrationPoint1x", point1.x);
        bundle.putFloat("lengthCalibrationPoint1y", point1.y);
        bundle.putFloat("lengthCalibrationPoint2x", point2.x);
        bundle.putFloat("lengthCalibrationPoint2y", point2.y);
        bundle.putFloat("lengthCalibrationValue", getCalibrationValue());

        return bundle;
    }

    public void fromBundle(Bundle bundle) {
        PointF point1 = calibrationMarkers.getDataAt(0).getPosition();
        PointF point2 = calibrationMarkers.getDataAt(1).getPosition();
        if (bundle.containsKey("lengthCalibrationPoint1x"))
            point1.x = bundle.getFloat("lengthCalibrationPoint1x");
        if (bundle.containsKey("lengthCalibrationPoint1y"))
            point1.y = bundle.getFloat("lengthCalibrationPoint1y");
        if (bundle.containsKey("lengthCalibrationPoint2x"))
            point2.x = bundle.getFloat("lengthCalibrationPoint2x");
        if (bundle.containsKey("lengthCalibrationPoint2y"))
            point2.y = bundle.getFloat("lengthCalibrationPoint2y");
        if (bundle.containsKey("lengthCalibrationValue"))
            calibrationValue = bundle.getFloat("lengthCalibrationValue");

        calibrate();
    }

    public float getCalibrationValue() {
        return calibrationValue;
    }

    public CalibrationXY getCalibrationXY() {
        return calibrationXY;
    }

    public float scaleLength() {
        PointF point1 = calibrationMarkers.getDataAt(0).getPosition();
        PointF point2 = calibrationMarkers.getDataAt(1).getPosition();

        return  (float)Math.sqrt(Math.pow(point1.x - point2.x, 2) +  Math.pow(point1.y - point2.y, 2));
    }

    private void calibrate() {
        if (calibrationMarkers.getDataCount() != 2)
            return;
        float value = calibrationValue / scaleLength();
        calibrationXY.setScale(value, value);
    }
}
