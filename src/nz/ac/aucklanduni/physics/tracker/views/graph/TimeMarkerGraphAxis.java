/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.views.graph;


import nz.ac.aucklanduni.physics.tracker.ExperimentAnalysis;
import nz.ac.aucklanduni.physics.tracker.MarkersDataModel;

public class TimeMarkerGraphAxis extends MarkerGraphAxis {
    @Override
    public int size() {
        return getData().getMarkerCount();
    }

    @Override
    public Number getValue(int index) {
        MarkersDataModel markerData = getExperimentAnalysis().getTagMarkers();
        int runId = markerData.getMarkerDataAt(index).getRunId();
        return getExperimentAnalysis().getExperiment().getRunValueAt(runId);
    }

    @Override
    public String getLabel() {
        ExperimentAnalysis experimentAnalysis = getExperimentAnalysis();
        return "time [" + experimentAnalysis.getExperiment().getRunValueUnit() + "]";
    }

    @Override
    public Number getMinRange() {
        return -1;
    }
}
