/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.graphics.PointF;

/**
 * Two dimensional data that is associated with an integer run id.
 * <p>
 * The run id is associated with a run of an experiment. The run id is needed because not every run may have a marker.
 * For example, if only every second run is tagged the marker run id is used to map the marker data to the sensors.
 * </p>
 */
public class MarkerData {
    final private int id;
    private PointF positionReal;

    public MarkerData(int id) {
        this.id = id;
        positionReal = new PointF();
    }

    public int getId() {
        return id;
    }

    public PointF getPosition() {
        return positionReal;
    }

    public void setPosition(PointF positionReal) {
        this.positionReal.set(positionReal);
    }
}
