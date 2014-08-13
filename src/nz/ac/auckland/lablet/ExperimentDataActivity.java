/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import nz.ac.auckland.lablet.experiment.ExperimentData;
import nz.ac.auckland.lablet.experiment.ExperimentLoader;
import nz.ac.auckland.lablet.experiment.SensorAnalysis;
import nz.ac.auckland.lablet.experiment.IExperimentPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract base class for activities that analyze an experiment.
 */
abstract public class ExperimentDataActivity extends FragmentActivity {
    protected class AnalysisEntry {
        final public SensorAnalysis analysis;
        final public IExperimentPlugin plugin;

        public AnalysisEntry(ExperimentData.SensorEntry sensorEntry) {
            this.plugin = sensorEntry.plugin;
            this.analysis = ExperimentLoader.getSensorAnalysis(sensorEntry);
        }
    }

    protected ExperimentData experimentData = null;

    final protected List<List<AnalysisEntry>> analysisRuns = new ArrayList<>();
    protected List<AnalysisEntry> currentAnalysisRun;
    protected AnalysisEntry currentAnalysisSensor;

    protected void setCurrentAnalysisRun(int index) {
        currentAnalysisRun = analysisRuns.get(index);
        setCurrentAnalysisSensor(0);
    }

    protected void setCurrentAnalysisSensor(int index) {
        currentAnalysisSensor = currentAnalysisRun.get(index);
    }

    public List<AnalysisEntry> getCurrentAnalysisRun() {
        return currentAnalysisRun;
    }

    private void setExperimentData(ExperimentData experimentData) {
        this.experimentData = experimentData;

        for (ExperimentData.RunEntry runEntry : experimentData.getRuns()) {
            List<AnalysisEntry> analysisEntryList = new ArrayList<>();
            for (ExperimentData.SensorEntry sensor : runEntry.sensors) {
                AnalysisEntry entry = new AnalysisEntry(sensor);
                if (entry.analysis == null)
                    continue;
                analysisEntryList.add(entry);
            }
            analysisRuns.add(analysisEntryList);
        }

        if (analysisRuns.size() == 0 || analysisRuns.get(0).size() == 0) {
            showErrorAndFinish("No experiment found.");
            return;
        }

        setCurrentAnalysisRun(0);
        setCurrentAnalysisSensor(0);
    }

    public ExperimentData getExperimentData() {
        return experimentData;
    }

    protected boolean loadExperiment(Intent intent) {
        if (intent == null) {
            showErrorAndFinish("can't load experiment (Intent is null)");
            return false;
        }

        String experimentPath = intent.getStringExtra("experiment_path");
        int runId = intent.getIntExtra("run_id", 0);
        int sensorId = intent.getIntExtra("sensor_id", 0);

        ExperimentData experimentData = new ExperimentData();
        if (!experimentData.load(this, experimentPath)) {
            showErrorAndFinish(experimentData.getLoadError());
            return false;
        }
        setExperimentData(experimentData);

        setCurrentAnalysisRun(runId);
        setCurrentAnalysisSensor(sensorId);
        return true;
    }

    protected void showErrorAndFinish(String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(error);
        builder.setNeutralButton("Ok", null);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
    }

    static public File getDefaultExperimentBaseDir(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        return new File(baseDir, "experiments");
    }
}
