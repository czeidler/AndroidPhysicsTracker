/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import nz.ac.auckland.lablet.camera.CameraExperimentData;
import nz.ac.auckland.lablet.experiment.ExperimentPluginFactory;
import nz.ac.auckland.lablet.experiment.IExperiment;
import nz.ac.auckland.lablet.experiment.IExperimentPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ExperimentActivity extends Activity {
    final private List<IExperiment> experiments = new ArrayList<>();
    private IExperiment currentExperiment;
    private File experimentBaseDir;

    private FrameLayout centerView = null;
    private ImageButton startButton = null;
    private ImageButton stopButton = null;
    private ImageButton newButton = null;
    private MenuItem analyseMenuItem = null;
    private MenuItem settingsMenu = null;

    private AbstractViewState state = null;

    private boolean unsavedExperimentData = false;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.perform_experiment_activity_actions, menu);

        MenuItem backItem = menu.findItem(R.id.action_back);
        assert backItem != null;
        backItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                onBackPressed();
                return false;
            }
        });
        analyseMenuItem = menu.findItem(R.id.action_analyse);
        assert analyseMenuItem != null;
        analyseMenuItem.setEnabled(false);
        analyseMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                finishExperiment(true);
                return true;
            }
        });
        Intent intent = getIntent();
        if (intent != null) {
            Bundle options = intent.getExtras();
            if (options != null) {
                boolean showAnalyseMenu = options.getBoolean("show_analyse_menu", true);
                analyseMenuItem.setVisible(showAnalyseMenu);
            }
        }

        settingsMenu = menu.findItem(R.id.action_settings);
        assert settingsMenu != null;
        if (currentExperiment != null) {
            currentExperiment.onPrepareOptionsMenu(settingsMenu, new IExperiment.IExperimentParent() {
                private AbstractViewState previousState;

                @Override
                public void startEditingSettings() {
                    previousState = state;
                    setState(null);
                }

                @Override
                public void finishEditingSettings() {
                    setState(previousState);
                }
            });
        }

        // set states after menu has been init
        if (!unsavedExperimentData) {
            setState(new PreviewState());
        } else {
            // we have unsaved experiment data means we are in the PlaybackState state
            setState(new PlaybackState());
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.experiment_recording);

        centerView = (FrameLayout)findViewById(R.id.centerLayout);

        startButton = (ImageButton)findViewById(R.id.recordButton);
        stopButton = (ImageButton)findViewById(R.id.stopButton);
        newButton = (ImageButton)findViewById(R.id.newButton);

        setState(null);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (state != null)
                    state.onRecordClicked();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (state != null)
                    state.onStopClicked();
            }
        });

        newButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (state != null)
                    state.onNewClicked();
            }
        });

        IExperimentPlugin plugin = ExperimentPluginFactory.getFactory().findExperimentPlugin(CameraExperimentData.class.getSimpleName());
        experimentBaseDir = new File(getExternalFilesDir(null), "experiments");
        IExperiment experiment = plugin.createExperiment(this, getIntent(), experimentBaseDir);
        addExperiment(experiment);
        centerView.addView(experiment.createExperimentView(this));
    }

    private void addExperiment(IExperiment experiment) {
        experiments.add(experiment);
        currentExperiment = experiment;
        invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (int i = 0; i < experiments.size(); i++) {
            IExperiment experiment = experiments.get(i);

            String experimentId = "";
            experimentId += i;
            Bundle experimentState = new Bundle();
            experiment.onSaveInstanceState(experimentState);

            outState.putBundle(experimentId, experimentState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        for (int i = 0; i < experiments.size(); i++) {
            IExperiment experiment = experiments.get(i);

            String experimentId = "";
            experimentId += i;
            Bundle experimentState = savedInstanceState.getBundle(experimentId);
            experiment.onRestoreInstanceState(experimentState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (IExperiment experiment : experiments)
            experiment.init(this, getIntent(), experimentBaseDir);
    }

    @Override
    public void onPause() {
        setState(null);

        for (IExperiment experiment : experiments)
            experiment.destroy();

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (unsavedExperimentData) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Experiment is not saved");
            builder.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finishExperiment(false);
                }
            });
            builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    for (IExperiment experiment : experiments)
                        try {
                            experiment.finish(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    unsavedExperimentData = false;
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

            builder.create().show();
        } else {
            for (IExperiment experiment : experiments)
                try {
                    experiment.finish(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void startRecording() {
        try {
            for (IExperiment experiment : experiments)
                experiment.startRecording();

        } catch (Exception e) {
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Unable to start recording!");
            builder.setNeutralButton("Ok", null);
            builder.create().show();
            setState(null);
            // in case some experiment are running stop them
            stopRecording();
            return;
        }
    }

    private void stopRecording() {
        for (IExperiment experiment : experiments)
            experiment.stopRecording();
    }

    private void finishExperiment(boolean startAnalysis) {
        unsavedExperimentData = false;

        try {
            for (IExperiment experiment : experiments)
                experiment.finish(false);

            Intent data = new Intent();
            //TODO: FIX
            //File outputDir = experimentData.getStorageDir();
            //data.putExtra("experiment_path", outputDir.getPath());
            data.putExtra("start_analysis", startAnalysis);
            setResult(RESULT_OK, data);
        } catch (IOException e) {
            e.printStackTrace();
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    abstract class AbstractViewState {
        abstract public void enterState();
        abstract public void leaveState();
        public void onRecordClicked() {}
        public void onStopClicked() {}
        public void onNewClicked() {}
    }

    private void setState(AbstractViewState newState) {
        if (state != null)
            state.leaveState();
        state = newState;
        if (state == null) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            newButton.setVisibility(View.INVISIBLE);
        } else
            state.enterState();
    }

    class PreviewState extends AbstractViewState {
        public void enterState() {
            settingsMenu.setVisible(true);

            unsavedExperimentData = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            newButton.setVisibility(View.INVISIBLE);

            analyseMenuItem.setEnabled(false);

            for (IExperiment experiment : experiments)
                experiment.startPreview();
        }

        public void leaveState() {
            settingsMenu.setVisible(false);

            for (IExperiment experiment : experiments)
                experiment.stopPreview();
        }

        @Override
        public void onRecordClicked() {
            setState(new RecordState());
        }
    }

    /**
     * Lock the screen to the current orientation.
     * @return the previous orientation settings
     */
    private int lockScreenOrientation() {
        int initialRequestedOrientation = getRequestedOrientation();

        // Note: a surface rotation of 90 degrees means a physical device rotation of -90 degrees.
        int orientation = getResources().getConfiguration().orientation;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_90:
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
        return initialRequestedOrientation;
    }

    class RecordState extends AbstractViewState {
        private boolean isRecording = false;
        private int initialRequestedOrientation;
        public void enterState() {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            newButton.setVisibility(View.INVISIBLE);

            analyseMenuItem.setEnabled(false);

            // disable screen rotation during recording
            initialRequestedOrientation = lockScreenOrientation();

            startRecording();
            isRecording = true;

            // don't fall asleep!
            centerView.setKeepScreenOn(true);
        }

        public void leaveState() {
            if (isRecording) {
                stopRecording();
                isRecording = false;
            }
            unsavedExperimentData = true;

            setRequestedOrientation(initialRequestedOrientation);

            // sleep if tired
            centerView.setKeepScreenOn(false);
        }

        @Override
        public void onStopClicked() {
            stopRecording();
            isRecording = false;
            setState(new PlaybackState());
        }
    }

    class PlaybackState extends AbstractViewState {
        public void enterState() {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            newButton.setVisibility(View.VISIBLE);

            for (IExperiment experiment : experiments)
                experiment.startPlayback();

            analyseMenuItem.setEnabled(true);
        }

        public void leaveState() {
            for (IExperiment experiment : experiments)
                experiment.stopPlayback();
        }

        @Override
        public void onNewClicked() {
            setState(new PreviewState());
        }
    }
}
