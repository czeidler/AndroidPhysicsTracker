/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.lablet.views.graph;

import nz.ac.aucklanduni.physics.lablet.experiment.ExperimentAnalysis;
import nz.ac.aucklanduni.physics.lablet.experiment.MarkerDataModel;


/**
 * Abstract base class for marker graph axes.
 */
public abstract class MarkerGraphAxis implements IGraphAdapter.IGraphAxis {
    protected MarkerGraphAdapter markerGraphAdapter;

    public void setMarkerGraphAdapter(MarkerGraphAdapter adapter) {
        markerGraphAdapter = adapter;
    }

    public MarkerDataModel getData() {
        return markerGraphAdapter.getData();
    }

    public ExperimentAnalysis getExperimentAnalysis() {
        return markerGraphAdapter.getExperimentAnalysis();
    }
}
