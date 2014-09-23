/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.microphone;

import android.os.Bundle;
import nz.ac.auckland.lablet.experiment.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


public class FrequencyAnalysis implements ISensorAnalysis {
    final private MicrophoneSensorData sensorData;

    final private MarkerDataModel hCursorMarkerModel;
    final private MarkerDataModel vCursorMarkerModel;
    final private Unit xUnit = new Unit("s");
    final private Unit yUnit = new Unit("Hz");
    final private FreqMapDisplaySettings freqMapDisplaySettings = new FreqMapDisplaySettings();

    public class FreqMapDisplaySettings {
        private int windowSize = 4096;
        private float stepFactor = 0.5f;
        private int contrast = 127;
        private int brightness = 127;

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }

        public float getStepFactor() {
            return stepFactor;
        }

        public void setStepFactor(float stepFactor) {
            this.stepFactor = stepFactor;
        }

        public int getContrast() {
            return contrast;
        }

        public void setContrast(int contrast) {
            this.contrast = contrast;
        }

        public int getBrightness() {
            return brightness;
        }

        public void setBrightness(int brightness) {
            this.brightness = brightness;
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("windowSize", windowSize);
            bundle.putFloat("stepFactor", stepFactor);
            bundle.putInt("contrast", contrast);
            bundle.putInt("brightness", brightness);
            return bundle;
        }

        public void fromBundle(Bundle bundle) {
            windowSize = bundle.getInt("windowSize", windowSize);
            stepFactor = bundle.getFloat("stepFactor", stepFactor);
            contrast = bundle.getInt("contrast", contrast);
            brightness = bundle.getInt("brightness", brightness);
        }
    }

    public FrequencyAnalysis(MicrophoneSensorData sensorData) {
        this.sensorData = sensorData;

        xUnit.setName("time");
        yUnit.setName("frequency");
        hCursorMarkerModel = new MarkerDataModel();
        vCursorMarkerModel = new MarkerDataModel();
    }

    public Unit getXUnit() {
        return xUnit;
    }

    public Unit getYUnit() {
        return yUnit;
    }

    public FreqMapDisplaySettings getFreqMapDisplaySettings() {
        return freqMapDisplaySettings;
    }

    @Override
    public String getIdentifier() {
        return getClass().getSimpleName();
    }

    @Override
    public ISensorData getData() {
        return sensorData;
    }

    @Override
    public boolean loadAnalysisData(Bundle bundle, File storageDir) {
        Bundle hCursorsBundle = bundle.getBundle("hCursors");
        if (hCursorsBundle != null)
            hCursorMarkerModel.fromBundle(hCursorsBundle);
        Bundle vCursorsBundle = bundle.getBundle("vCursors");
        if (vCursorsBundle != null)
            vCursorMarkerModel.fromBundle(vCursorsBundle);

        Bundle freqMapDisplaySettingsBundle = bundle.getBundle("freqMapDisplaySettings");
        if (freqMapDisplaySettingsBundle != null)
            freqMapDisplaySettings.fromBundle(freqMapDisplaySettingsBundle);

        return true;
    }

    @Override
    public Bundle exportAnalysisData(File additionalStorageDir) throws IOException {
        Bundle analysisDataBundle = new Bundle();

        if (hCursorMarkerModel.getMarkerCount() > 0)
            analysisDataBundle.putBundle("hCursors", hCursorMarkerModel.toBundle());
        if (vCursorMarkerModel.getMarkerCount() > 0)
            analysisDataBundle.putBundle("vCursors", vCursorMarkerModel.toBundle());

        analysisDataBundle.putBundle("freqMapDisplaySettings", getFreqMapDisplaySettings().toBundle());
        return analysisDataBundle;
    }

    @Override
    public void exportTagMarkerCSVData(OutputStream outputStream) throws IOException {

    }

    public MarkerDataModel getHCursorMarkerModel() {
        return hCursorMarkerModel;
    }

    public MarkerDataModel getVCursorMarkerModel() {
        return vCursorMarkerModel;
    }
}
