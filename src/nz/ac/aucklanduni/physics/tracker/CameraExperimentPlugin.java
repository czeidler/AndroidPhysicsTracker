/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.io.File;

public class CameraExperimentPlugin implements ExperimentPlugin {
    @Override
    public String getName() {
        return CameraExperiment.class.getSimpleName();
    }

    @Override
    public String toString() {
        return "Camera Experiment";
    }

    @Override
    public void startExperimentActivity(Activity parentActivity, int requestCode) {
        Intent intent = new Intent(parentActivity, CameraExperimentActivity.class);
        parentActivity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startRunSettingsActivity(Experiment experiment, Bundle analysisSpecificData, Activity parentActivity,
                                         int requestCode) {
        Intent intent = new Intent(parentActivity, CameraRunSettingsActivity.class);
        intent.putExtra("experiment_path", experiment.getStorageDir().getPath());
        if (analysisSpecificData != null)
            intent.putExtra("analysisSpecificData", analysisSpecificData);
        parentActivity.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean hasRunEditActivity(StringBuilder menuName) {
        if (menuName != null)
            menuName.append("Video Settings");
        return true;
    }

    @Override
    public Experiment loadExperiment(Context context, Bundle data, File storageDir) {
        return new CameraExperiment(context, data, storageDir);
    }

    @Override
    public ExperimentAnalysis loadExperimentAnalysis(Experiment experiment) {
        return new CameraExperimentAnalysis(experiment);
    }

    @Override
    public View createExperimentRunView(Context context, Experiment experiment) {
        return new CameraExperimentRunView(context, experiment);
    }
}
