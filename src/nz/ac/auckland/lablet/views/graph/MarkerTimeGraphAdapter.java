/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.graph;

import nz.ac.auckland.lablet.camera.ITimeCalibration;
import nz.ac.auckland.lablet.experiment.CalibratedMarkerDataModel;


/**
 * Marker data adapter for the graphs.
 */
public class MarkerTimeGraphAdapter extends MarkerGraphAdapter {
    protected ITimeCalibration timeCalibration;

    public MarkerTimeGraphAdapter(CalibratedMarkerDataModel data, ITimeCalibration timeCalibration, String title,
                                  MarkerGraphAxis xAxis, MarkerGraphAxis yAxis) {
        super(data, title, xAxis, yAxis);

        this.timeCalibration = timeCalibration;
    }

    public void setTo(CalibratedMarkerDataModel data,  ITimeCalibration timeCalibration) {
        setTo(data);

        this.timeCalibration = timeCalibration;
    }

    public ITimeCalibration getTimeCalibration() {
        return timeCalibration;
    }

    public static MarkerTimeGraphAdapter createXSpeedAdapter(CalibratedMarkerDataModel data, ITimeCalibration timeCalibration, String title) {
        return new MarkerTimeGraphAdapter(data, timeCalibration, title, new SpeedTimeMarkerGraphAxis(),
                new XSpeedMarkerGraphAxis());
    }

    public static MarkerTimeGraphAdapter createYSpeedAdapter(CalibratedMarkerDataModel data, ITimeCalibration timeCalibration, String title) {
        return new MarkerTimeGraphAdapter(data, timeCalibration, title, new SpeedTimeMarkerGraphAxis(),
                new YSpeedMarkerGraphAxis());
    }
}
