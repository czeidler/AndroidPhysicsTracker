package com.example.AndroidPhysicsTracker;


import android.graphics.PointF;
import android.os.Bundle;

import java.io.File;

public class ExperimentAnalysis {
    private Experiment experiment;

    private RunDataModel runDataModel;
    private Calibration calibration;

    private MarkersDataModel tagMarkers;
    private MarkersDataModel xyCalibrationMarkers;
    private XYCalibrationSetter xyCalibrationSetter;

    private Bundle experimentSpecificData = null;

    public ExperimentAnalysis(Experiment experiment) {
        this.experiment = experiment;

        runDataModel = new RunDataModel();
        runDataModel.setNumberOfRuns(experiment.getNumberOfRuns());

        calibration = new Calibration();

        tagMarkers = new MarkersDataModel();
        tagMarkers.setCalibration(calibration);

        xyCalibrationMarkers = new MarkersDataModel();
        MarkerData point1 = new MarkerData(-1);
        point1.setPosition(new PointF(10, 10));
        xyCalibrationMarkers.addMarkerData(point1);
        MarkerData point2 = new MarkerData(-2);
        point2.setPosition(new PointF(30, 10));
        xyCalibrationMarkers.addMarkerData(point2);
        xyCalibrationSetter = new XYCalibrationSetter(calibration, xyCalibrationMarkers);
    }

    public Experiment getExperiment() { return  experiment; }
    public RunDataModel getRunDataModel() {
        return runDataModel;
    }
    public Calibration getCalibration() {
        return calibration;
    }
    public MarkersDataModel getTagMarkers() {
        return tagMarkers;
    }
    public MarkersDataModel getXYCalibrationMarkers() { return xyCalibrationMarkers; }
    public Bundle getExperimentSpecificData() { return experimentSpecificData; }
    public void setExperimentSpecificData(Bundle data) {
        experimentSpecificData = data;
        onRunSpecificDataChanged();
    }

    public Bundle analysisDataToBundle() {
        Bundle analysisDataBundle = new Bundle();

        analysisDataBundle.putInt("currentRun", runDataModel.getCurrentRun());

        if (tagMarkers.getMarkerCount() > 0) {
            Bundle tagMarkerBundle = new Bundle();
            int[] runIds = new int[tagMarkers.getMarkerCount()];
            float[] xPositions = new float[tagMarkers.getMarkerCount()];
            float[] yPositions = new float[tagMarkers.getMarkerCount()];
            for (int i = 0; i < tagMarkers.getMarkerCount(); i++) {
                MarkerData data = tagMarkers.getMarkerDataAt(i);
                runIds[i] = data.getRunId();
                xPositions[i] = data.getPosition().x;
                yPositions[i] = data.getPosition().y;
            }
            tagMarkerBundle.putIntArray("runIds", runIds);
            tagMarkerBundle.putFloatArray("xPositions", xPositions);
            tagMarkerBundle.putFloatArray("yPositions", yPositions);

            analysisDataBundle.putBundle("tagMarkers", tagMarkerBundle);
        }

        if (experimentSpecificData != null)
            analysisDataBundle.putBundle("experiment_specific_data", experimentSpecificData);
        return analysisDataBundle;
    }

    protected boolean loadAnalysisData(Bundle bundle, File storageDir) {
        tagMarkers.clear();

        setExperimentSpecificData(bundle.getBundle("experiment_specific_data"));
        runDataModel.setCurrentRun(bundle.getInt("currentRun"));

        Bundle tagMarkerBundle = bundle.getBundle("tagMarkers");
        if (tagMarkerBundle != null) {
            tagMarkers.clear();
            int[] runIds = tagMarkerBundle.getIntArray("runIds");
            float[] xPositions = tagMarkerBundle.getFloatArray("xPositions");
            float[] yPositions = tagMarkerBundle.getFloatArray("yPositions");

            if (runIds != null && xPositions != null && yPositions != null && runIds.length == xPositions.length
                && xPositions.length == yPositions.length) {
                for (int i = 0; i < runIds.length; i++) {
                    MarkerData data = new MarkerData(runIds[i]);
                    data.getPosition().set(xPositions[i], yPositions[i]);
                    tagMarkers.addMarkerData(data);
                }
            }
        }

        return true;
    }

    protected void onRunSpecificDataChanged() {}
}
