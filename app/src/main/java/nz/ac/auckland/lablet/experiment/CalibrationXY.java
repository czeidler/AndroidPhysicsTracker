/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.graphics.PointF;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;


interface ICalibrationXY {
    /**
     * Raw point from the screen is transformed to real coordinates.
     * @param raw point on the screen
     * @return point in real coordinates
     */
    public PointF fromRaw(PointF raw);

    /**
     * A raw length vector is scaled to real length vector. (no rotation or origin are taken into account)
     *
     * @param rawLength length vector on the screen
     * @return length vector in real coordinates.
     */
    public PointF fromRawLength(PointF rawLength);
}

/**
 * Class to map screen coordinates to real coordinates.
 * <p>
 * This includes the x and y scale, the origin and the rotation of the screen.
 * </p>
 */
public class CalibrationXY implements ICalibrationXY {
    public interface IListener {
        public void onCalibrationChanged();
    }

    private float xScale;
    private float yScale;

    private PointF origin = new PointF(5, 5);
    private PointF axis1 = new PointF(15, 5);
    private float angle;
    private boolean swapAxes = false;

    final private List<IListener> listeners = new ArrayList<>();

    public CalibrationXY() {
        xScale = 1;
        yScale = 1;
    }

    final static private String ORIGIN_X_KEY = "originX";
    final static private String ORIGIN_Y_KEY = "originY";
    final static private String AXIS1_X_KEY = "axis1x";
    final static private String AXIS1_Y_KEY = "axis1y";
    final static private String SWAP_AXES_KEY = "swapAxes";

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putFloat(ORIGIN_X_KEY, getOrigin().x);
        bundle.putFloat(ORIGIN_Y_KEY, getOrigin().y);
        bundle.putFloat(AXIS1_X_KEY, getAxis1().x);
        bundle.putFloat(AXIS1_Y_KEY, getAxis1().y);
        bundle.putBoolean(SWAP_AXES_KEY, getSwapAxes());
        return bundle;
    }

    public void fromBundle(Bundle bundle) {
        if (bundle.containsKey(ORIGIN_X_KEY))
            origin.x = bundle.getFloat(ORIGIN_X_KEY);
        if (bundle.containsKey(ORIGIN_Y_KEY))
            origin.y = bundle.getFloat(ORIGIN_Y_KEY);
        if (bundle.containsKey(AXIS1_X_KEY))
            axis1.x = bundle.getFloat(AXIS1_X_KEY);
        if (bundle.containsKey(AXIS1_Y_KEY))
            axis1.y = bundle.getFloat(AXIS1_Y_KEY);
        if (bundle.containsKey(SWAP_AXES_KEY))
            swapAxes = bundle.getBoolean(SWAP_AXES_KEY);
        notifyCalibrationChanged();
    }

    /**
     * Add listener to listen for calibration changes.
     * @param listener the interested object
     */
    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(IListener listener) {
        return listeners.remove(listener);
    }

    public float getXCalibration() {
        return xScale;
    }

    public float getYCalibration() {
        return yScale;
    }

    /**
     * Raw point from the screen is transformed to real coordinates.
     * @param raw point on the screen
     * @return point in real coordinates
     */
    @Override
    public PointF fromRaw(PointF raw) {
        PointF point = new PointF();
        // translation
        point.x = raw.x - origin.x;
        point.y = raw.y - origin.y;

        // rotation
        float x = point.x;
        float y = point.y;
        point.x = (float)Math.cos(Math.toRadians(angle)) * x + (float)Math.sin(Math.toRadians(angle)) * y;
        point.y = (float)Math.cos(Math.toRadians(angle)) * y - (float)Math.sin(Math.toRadians(angle)) * x;

        // swap axis
        if (swapAxes) {
            // mirror at x and turn 90 degree to the right
            x = -point.x;
            y = point.y;
            point.x = (float)Math.cos(Math.toRadians(90)) * x + (float)Math.sin(Math.toRadians(90)) * y;
            point.y = (float)Math.cos(Math.toRadians(90)) * y - (float)Math.sin(Math.toRadians(90)) * x;
        }

        // scale
        point.x *= xScale;
        point.y *= yScale;
        return point;
    }

    /**
     * A raw length vector is scaled to real length vector. (no rotation or origin are taken into account)
     *
     * @param rawLength length vector on the screen
     * @return length vector in real coordinates.
     */
    @Override
    public PointF fromRawLength(PointF rawLength) {
        PointF point = new PointF();
        point.set(rawLength);
        // scale
        point.x *= xScale;
        point.y *= yScale;
        return point;
    }

    /**
     * * Sets the x calibration scale factor.
     * <p>
     * A real length can be calculated from the screen length: l_{real} = scale * l_{screen}.
     * </p>
     * @param xScale x-scale
     */
    public void setXScale(float xScale) {
        this.xScale = xScale;
        notifyCalibrationChanged();
    }

    /**
     * * Sets the y calibration scale factor.
     * <p>
     * A real length can be calculated from the screen length: l_{real} = scale * l_{screen}.
     * </p>
     * @param yScale y-scale
     */
    public void setYScale(float yScale) {
        this.yScale = yScale;
        notifyCalibrationChanged();
    }

    /**
     * Sets the calibration scale factors.
     * <p>
     * A real length can be calculated from the screen length: l_{real} = scale * l_{screen}.
     * </p>
     * @param xScale x-scale
     * @param yScale y-scale
     */
    public void setScale(float xScale, float yScale) {
        this.xScale = xScale;
        this.yScale = yScale;
        notifyCalibrationChanged();
    }

    /**
     * Swap the axes of the coordinate system.
     *
     * @param swap true to swap
     */
    public void setSwapAxes(boolean swap) {
        swapAxes = swap;
        notifyCalibrationChanged();
    }

    /**
     * Set the origin and the orientation of the x-axis (in real coordinates).
     *
     * @param origin real origin of the coordinate system
     * @param axis1 orientation of the x-axis relative to the origin (length doesn't matter)
     */
    public void setOrigin(PointF origin, PointF axis1) {
        this.origin.set(origin);
        this.axis1 = axis1;
        this.angle = getAngle(origin, axis1);
        notifyCalibrationChanged();
    }

    public PointF getOrigin() {
        return origin;
    }
    public PointF getAxis1() {
        return axis1;
    }
    public boolean getSwapAxes() {
        return swapAxes;
    }

    private void notifyCalibrationChanged() {
        for (IListener listener : listeners)
            listener.onCalibrationChanged();
    }

    static public float getAngle(PointF origin, PointF point) {
        PointF relative = new PointF();
        relative.x = point.x - origin.x;
        relative.y = point.y - origin.y;
        float angle = 0;
        if (relative.x != 0) {
            angle = (float) Math.atan(relative.y / relative.x);
            angle = (float) Math.toDegrees((double) angle);
            // choose the right quadrant
            if (relative.x < 0)
                angle = 180 + angle;
        } else {
            if (relative.y < 0)
                angle = 270;
            else
                angle = 90;
        }

        return angle;
    }
}


