/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.accelerometer;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import nz.ac.auckland.lablet.experiment.IDataAnalysis;
import nz.ac.auckland.lablet.experiment.IExperimentData;
import nz.ac.auckland.lablet.experiment.MarkerData;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;

import java.io.File;
import java.io.IOException;
import java.io.Writer;


public class AccelerometerAnalysis implements IDataAnalysis {
    final private AccelerometerExperimentData sensorData;
    final private DisplaySettings displaySettings = new DisplaySettings();

    final private MarkerDataModel baseLineMarker = new MarkerDataModel();
    final private MarkerDataModel rangeMarkers = new MarkerDataModel();

    public class DisplaySettings {
        final RectF range = new RectF();

        public void setRange(RectF range) {
            this.range.set(range);
        }

        public RectF getRange() {
            return range;
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putFloat("rangeLeft", range.left);
            bundle.putFloat("rangeTop", range.top);
            bundle.putFloat("rangeRight", range.right);
            bundle.putFloat("rangeBottom", range.bottom);
            return bundle;
        }

        public void fromBundle(Bundle bundle) {
            range.left = bundle.getFloat("rangeLeft", range.left);
            range.top = bundle.getFloat("rangeTop", range.top);
            range.right = bundle.getFloat("rangeRight", range.right);
            range.bottom = bundle.getFloat("rangeBottom", range.bottom);
        }
    }

    public AccelerometerAnalysis(AccelerometerExperimentData sensorData) {
        this.sensorData = sensorData;

        MarkerData markerData = new MarkerData(0);
        markerData.setPosition(new PointF(0f, 9.81f));
        baseLineMarker.addMarkerData(markerData);

        markerData = new MarkerData(0);
        markerData.setPosition(new PointF(0f, 0f));
        rangeMarkers.addMarkerData(markerData);
        markerData = new MarkerData(1);
        markerData.setPosition(new PointF(0f, 0f));
        rangeMarkers.addMarkerData(markerData);
    }

    public MarkerDataModel getBaseLineMarker() {
        return baseLineMarker;
    }

    public MarkerDataModel getRangeMarkers() {
        return rangeMarkers;
    }

    @Override
    public String getDisplayName() {
        return "Accelerometer Analysis";
    }

    @Override
    public String getIdentifier() {
        return "Vector4DAnalysis";
    }

    @Override
    public IExperimentData getData() {
        return sensorData;
    }

    @Override
    public boolean loadAnalysisData(Bundle bundle, File storageDir) {
        Bundle displaySettingsBundle = bundle.getBundle("displaySettingsBundle");
        if (displaySettingsBundle != null)
            displaySettings.fromBundle(displaySettingsBundle);

        Bundle baseLineMarkerBundle = bundle.getBundle("baseLineMarker");
        if (baseLineMarkerBundle != null)
            baseLineMarker.fromBundle(baseLineMarkerBundle);
        Bundle rangeMarkersBundle = bundle.getBundle("rangeMarkers");
        if (rangeMarkersBundle != null)
            rangeMarkers.fromBundle(rangeMarkersBundle);

        return true;
    }

    @Override
    public Bundle exportAnalysisData(File additionalStorageDir) throws IOException {
        Bundle bundle = new Bundle();
        bundle.putBundle("displaySettingsBundle", getDisplaySettings().toBundle());

        bundle.putBundle("baseLineMarker", baseLineMarker.toBundle());
        bundle.putBundle("rangeMarkers", rangeMarkers.toBundle());

        return bundle;
    }

    @Override
    public void exportTagMarkerCSVData(Writer writer) throws IOException {

    }

    public DisplaySettings getDisplaySettings() {
        return displaySettings;
    }
}
