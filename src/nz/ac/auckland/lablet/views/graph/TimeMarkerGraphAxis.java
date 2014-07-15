/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.graph;

import nz.ac.auckland.lablet.experiment.SensorAnalysis;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;


/**
 * Graph axis for the marker data graph adapter. Provides the time.
 */
public class TimeMarkerGraphAxis extends MarkerGraphAxis {
    @Override
    public int size() {
        return getData().getMarkerCount();
    }

    @Override
    public Number getValue(int index) {
        MarkerDataModel markerData = getExperimentAnalysis().getTagMarkers();
        int runId = markerData.getMarkerDataAt(index).getRunId();
        return getExperimentAnalysis().getSensorData().getRunValueAt(runId);
    }

    @Override
    public String getLabel() {
        SensorAnalysis sensorAnalysis = getExperimentAnalysis();
        return "time [" + sensorAnalysis.getSensorData().getRunValueUnit() + "]";
    }

    @Override
    public Number getMinRange() {
        return -1;
    }
}
