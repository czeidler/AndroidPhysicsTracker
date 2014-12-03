/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;


abstract public class AbstractExperimentSensorView extends FrameLayout implements IExperimentSensor.IExperimentSensorListener {
    protected AbstractExperimentSensor.State previewState = null;
    protected AbstractExperimentSensor.State recordingState = null;
    protected AbstractExperimentSensor.State playbackState = null;

    public AbstractExperimentSensorView(Context context) {
        super(context);
    }

    public AbstractExperimentSensorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onStartPreview() {
        if (previewState != null)
            previewState.start();
    }

    @Override
    public void onStopPreview() {
        if (previewState != null)
            previewState.stop();
    }

    @Override
    public void onStartRecording() {
        if (recordingState != null)
            recordingState.start();
    }

    @Override
    public void onStopRecording() {
        if (recordingState != null)
            recordingState.stop();
    }

    @Override
    public void onStartPlayback() {
        if (playbackState != null)
            playbackState.start();
    }

    @Override
    public void onStopPlayback() {
        if (playbackState != null)
            playbackState.stop();
    }
}
