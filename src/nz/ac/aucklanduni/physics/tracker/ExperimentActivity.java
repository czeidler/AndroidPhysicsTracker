/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.io.*;

abstract public class ExperimentActivity extends FragmentActivity {
    protected Experiment experiment = null;
    protected ExperimentPlugin plugin = null;

    private File baseDirectory = null;

    protected void setExperiment(Experiment experiment) {
        this.experiment = experiment;

        baseDirectory = getDefaultExperimentBaseDir(this);

        // set experiment storage dir
        if (experiment.getStorageDir() != null)
            return;

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey("experiment_base_directory"))
                    baseDirectory = new File(extras.getString("experiment_base_directory"));
            }
        }

        experiment.setStorageDir(getExperimentStorageDir());

    }

    public Experiment getExperiment() {
        return experiment;
    }
    public ExperimentPlugin getExperimentPlugin() {
        return plugin;
    }

    protected boolean loadExperiment(Intent intent) {
        if (intent == null) {
            showErrorAndFinish("can't load experiment (Intent is null)");
            return false;
        }

        String experimentPath = intent.getStringExtra("experiment_path");

        ExperimentLoader.Result result = new ExperimentLoader.Result();
        if (!ExperimentLoader.loadExperiment(this, experimentPath, result)) {
            showErrorAndFinish(result.loadError);
            return false;
        }

        plugin = result.plugin;
        experiment = result.experiment;

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
        File experimentDir = new File(baseDir, "experiments");
        if (!experimentDir.exists())
            experimentDir.mkdir();
        return experimentDir;
    }

    private File getExperimentStorageDir() {
        String directoryName = getExperiment().getUid();
        File dir = new File(baseDirectory, directoryName);
        if (!dir.exists())
            dir.mkdir();
        return dir;
    }

    protected boolean deleteStorageDir() {
        File file = getExperimentStorageDir();
        return StorageLib.recursiveDeleteFile(file);
    }
}
