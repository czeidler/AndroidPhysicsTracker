/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;


import android.content.Intent;
import android.os.Bundle;


/**
 * Abstract base class for experiment plugins.
 */
abstract public class AbstractExperimentPlugin implements IExperimentPlugin {
    /**
     * Helper method to pack the option bundle correctly.
     *
     * @param intent the Intent where the data should be packed to
     * @param options the options for the activity
     */
    static public void packStartExperimentIntent(Intent intent, Bundle options) {
        if (options != null)
            intent.putExtra("options", options);
    }

    /**
     * Helper method to pack the analysis specific data and the options bundles correctly.
     *
     * @param intent the Intent where the data should be packed to
     * @param sensorData the parent activity
     * @param analysisSpecificData analysis specific data bundle
     * @param options the options for the activity
     */
    static public void packStartRunSettingsIntent(Intent intent, SensorData sensorData,
                                                  Bundle analysisSpecificData, Bundle options) {
        intent.putExtra("experiment_path", sensorData.getStorageDir().getPath());
        if (analysisSpecificData != null)
            intent.putExtra("analysisSpecificData", analysisSpecificData);
        if (options != null)
            intent.putExtra("options", options);
    }
}
