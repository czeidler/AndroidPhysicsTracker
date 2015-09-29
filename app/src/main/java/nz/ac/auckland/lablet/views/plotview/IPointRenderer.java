/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.plotview;

import android.graphics.Canvas;
import android.graphics.PointF;


/**
 * Interface to render a shape at a certain position.
 */
public interface IPointRenderer {
    void drawPoint(Canvas canvas, PointF position, DrawConfig config);
}
