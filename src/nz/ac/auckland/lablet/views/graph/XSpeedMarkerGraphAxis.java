/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.graph;


import nz.ac.auckland.lablet.experiment.SensorAnalysis;
import nz.ac.auckland.lablet.experiment.SensorData;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;

/**
 * Graph axis for the marker data graph adapter. Provides the x-speed.
 */
public class XSpeedMarkerGraphAxis extends MarkerGraphAxis {
    @Override
    public int size() {
        return getData().getMarkerCount() - 1;
    }

    @Override
    public Number getValue(int index) {
        SensorAnalysis sensorAnalysis = getExperimentAnalysis();
        MarkerDataModel data = getData();

        SensorData sensorData = sensorAnalysis.getSensorData();
        float deltaX = data.getCalibratedMarkerPositionAt(index + 1).x - data.getCalibratedMarkerPositionAt(index).x;
        float deltaT = sensorData.getRunValueAt(index + 1) - sensorData.getRunValueAt(index);
        if (sensorAnalysis.getSensorData().getRunValueUnitPrefix().equals("m"))
            deltaT /= 1000;
        return deltaX / deltaT;
    }

    @Override
    public String getLabel() {
        SensorAnalysis sensorAnalysis = getExperimentAnalysis();
        return "velocity [" + sensorAnalysis.getXUnit() + "/"
                + sensorAnalysis.getSensorData().getRunValueBaseUnit() + "]";
    }

    @Override
    public Number getMinRange() {
        return 3;
    }
}
