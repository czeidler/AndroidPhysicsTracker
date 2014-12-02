/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.content.Context;
import android.os.Bundle;
import nz.ac.auckland.lablet.accelerometer.AccelerometerSensorData;
import nz.ac.auckland.lablet.camera.CameraSensorData;
import nz.ac.auckland.lablet.microphone.MicrophoneSensorData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ExperimentData {
    public static class RunEntry {
        public ExperimentRunData runData;
        public List<ISensorData> sensorDataList = new ArrayList<>();
    }

    private File storageDir;
    private String loadError = "";
    private List<RunEntry> runs = new ArrayList<>();

    public File getStorageDir() {
        return storageDir;
    }

    public List<RunEntry> getRuns() {
        return runs;
    }

    public String getLoadError() {
        return loadError;
    }

    private ISensorData loadSensorData(Context context, File sensorDirectory) {
        Bundle bundle;

        File file = new File(sensorDirectory, ISensorData.EXPERIMENT_DATA_FILE_NAME);
        bundle = ExperimentHelper.loadBundleFromFile(file);

        if (bundle == null) {
            loadError = "can't read experiment file";
            return null;
        }

        Bundle experimentData = bundle.getBundle("data");
        if (experimentData == null) {
            loadError = "failed to load experiment data";
            return null;
        }

        String experimentIdentifier = bundle.getString("sensor_name", "");

        ExperimentPluginFactory factory = ExperimentPluginFactory.getFactory();
        ISensorPlugin plugin = factory.findSensorPlugin(experimentIdentifier);
        if (plugin == null) {
            // fallback: try to find analysis for the data type
            if (!experimentData.containsKey(AbstractSensorData.DATA_TYPE_KEY)) {
                loadError = "experiment data type information is missing";
                return null;
            }
            String dataType = experimentData.getString(AbstractSensorData.DATA_TYPE_KEY);
            ISensorData sensorData = getSensorDataForType(dataType, context, experimentData, sensorDirectory);
            if (sensorData == null)
                loadError = "unknown experiment type";
            return sensorData;
        }


        ISensorData sensorData = plugin.loadSensorData(context, experimentData, sensorDirectory);
        if (sensorData == null) {
            loadError = "can't load experiment";
            return null;
        }

        return sensorData;
    }

    private ISensorData getSensorDataForType(String dataType, Context context, Bundle data, File dir) {
        ISensorData sensorData = null;
        switch (dataType) {
            case MicrophoneSensorData.DATA_TYPE:
                sensorData = new MicrophoneSensorData(context);
                break;
            case CameraSensorData.DATA_TYPE:
                sensorData = new CameraSensorData(context);
                break;
            case AccelerometerSensorData.DATA_TYPE:
                sensorData = new AccelerometerSensorData(context);
                break;
        }
        if (sensorData != null) {
            try {
                sensorData.loadExperimentData(data, dir);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return sensorData;
    }

    private ExperimentData.RunEntry loadRunData(Context context, File runDir) {
        ExperimentRunData runData = new ExperimentRunData();
        File runDataFile = new File(runDir, ExperimentRun.EXPERIMENT_RUN_FILE_NAME);
        try {
            runData.loadFromFile(runDataFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ExperimentData.RunEntry runEntry = new ExperimentData.RunEntry();
        runEntry.runData = runData;

        for (File runDirectory : runDir.listFiles()) {
            if (!runDirectory.isDirectory())
                continue;
            ISensorData sensorData = loadSensorData(context, runDirectory);
            if (sensorData == null)
                return null;
            runEntry.sensorDataList.add(sensorData);
        }
        return runEntry;
    }

    public boolean load(Context context, File storageDir) {
        if (storageDir == null || !storageDir.exists())
            return false;

        this.storageDir = storageDir;

        for (File groupDir : storageDir.listFiles()) {
            if (!groupDir.isDirectory())
                continue;

            ExperimentData.RunEntry runEntry = loadRunData(context, groupDir);
            if (runEntry == null)
                return false;
            runs.add(runEntry);
        }

        return true;
    }
}
