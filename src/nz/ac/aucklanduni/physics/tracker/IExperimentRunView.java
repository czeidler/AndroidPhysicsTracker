package nz.ac.aucklanduni.physics.tracker;

import android.graphics.PointF;

public interface IExperimentRunView {
    public void setCurrentRun(int run);

    // convert a coordinate on the screen to the real value of the measurement
    public void fromScreen(PointF screen, PointF real);
    public void toScreen(PointF real, PointF screen);

    public float getMaxRawX();
    public float getMaxRawY();
}