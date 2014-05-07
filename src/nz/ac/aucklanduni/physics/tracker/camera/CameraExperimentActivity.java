/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.camera;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import nz.ac.aucklanduni.physics.tracker.ExperimentActivity;
import nz.ac.aucklanduni.physics.tracker.R;
import nz.ac.aucklanduni.physics.tracker.StorageLib;
import nz.ac.aucklanduni.physics.tracker.views.RatioSurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraExperimentActivity extends ExperimentActivity {
    private RatioSurfaceView preview = null;
    private VideoView videoView = null;
    private Button startButton = null;
    private Button stopButton = null;
    private Button newButton = null;

    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private Camera.Size videoSize = null;
    private MediaRecorder recorder = null;
    private AbstractViewState state = null;

    private MenuItem analyseMenuItem = null;
    private int cameraId = 0;
    private int rotationDegree = 0;

    private File videoFile = null;
    private boolean unsavedExperimentData = false;

    static final int CAMERA_FACE = Camera.CameraInfo.CAMERA_FACING_BACK;

    static final String videoFileName = "video.mp4";

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
                boolean showAnalyseMenu = options.getBoolean("showAnalyseMenu", true);
                analyseMenuItem.setVisible(showAnalyseMenu);
            }
        }

        // set states after menu has been init
        if (previewHolder.getSurface() != null) {
            if (!unsavedExperimentData) {
                setState(new PreviewState());
            } else {
                // we have unsaved experiment data means we are in the PlaybackState state
                setState(new PlaybackState());
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setExperiment(new CameraExperiment(this));

        setContentView(R.layout.perform_camera_experiment);

        preview = (RatioSurfaceView)findViewById(R.id.surfaceView);
        previewHolder = preview.getHolder();
        assert previewHolder != null;
        previewHolder.addCallback(surfaceCallback);

        videoView = (VideoView)findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setKeepScreenOn(true);
        videoView.setMediaController(mediaController);

        recorder = new MediaRecorder();

        startButton = (Button) findViewById(R.id.recordButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        newButton = (Button) findViewById(R.id.newButton);

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
    }

    @Override
    public void onDestroy() {
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!unsavedExperimentData)
            return;

        outState.putString("unsaved_recording", videoFile.getPath());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (!savedInstanceState.containsKey("unsaved_recording"))
            return;

        String filePath = savedInstanceState.getString("unsaved_recording");
        videoFile = new File(filePath);

        // setting unsavedExperimentData here; from this information the correct state is set in onResume (after
        // everything has been init)
        unsavedExperimentData = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (camera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);

                if (info.facing == CAMERA_FACE) {
                    cameraId = i;
                    camera = Camera.open(i);
                }
            }
        }
        if (camera == null)
            camera = Camera.open();

        Camera.Parameters parameters = camera.getParameters();
        videoSize = getOptimalVideoSize(camera);
        assert videoSize != null;

        parameters.setPreviewSize(videoSize.width, videoSize.height);
        camera.setParameters(parameters);

        setCameraDisplayOrientation(cameraId, camera);

        float ratio;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                ratio = (float)videoSize.height / videoSize.width;
                break;
            default:
                ratio = (float)videoSize.width / videoSize.height;
        }
        preview.setRatio(ratio);
    }

    @Override
    public void onPause() {
        setState(null);

        camera.release();
        camera = null;
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
                    deleteTempFiles();
                    unsavedExperimentData = false;
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

            builder.create().show();
        } else {
            deleteTempFiles();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private boolean deleteTempFiles() {
        if (videoFile.exists())
            return videoFile.delete();
        return true;
    }

    // copied from android dev page
    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        rotationDegree = 0;
        switch (rotation) {
            case Surface.ROTATION_0: rotationDegree = 0; break;
            case Surface.ROTATION_90: rotationDegree = 90; break;
            case Surface.ROTATION_180: rotationDegree = 180; break;
            case Surface.ROTATION_270: rotationDegree = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + rotationDegree) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - rotationDegree + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void startRecording() {
        try {
            camera.unlock();
            recorder.setCamera(camera);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            // 90 degrees surface is -90 device...
            int hintRotation = rotationDegree;
            if (hintRotation == 90)
                hintRotation = 270;
            else if (hintRotation == 270)
                hintRotation = 90;
            recorder.setOrientationHint(hintRotation);

            CamcorderProfile profile = getOptimalCamcorderProfile(cameraId);
            if (profile == null)
                throw new Exception("no camcorder profile!");
            recorder.setVideoSize(videoSize.width, videoSize.height);
            recorder.setVideoFrameRate(profile.videoFrameRate);
            recorder.setVideoEncodingBitRate(profile.videoBitRate);

            File outputDir = getCacheDir();
            videoFile = new File(outputDir, getVideoFileName());
            if (!videoFile.exists()) {
                if (!videoFile.createNewFile())
                    return;
            }

            recorder.setOutputFile(videoFile.getPath());
            recorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Unable to start recording!");
            builder.setNeutralButton("Ok", null);
            builder.create().show();
            setState(null);
            return;
        }
        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.reset();

        camera.lock();
    }

    private String getVideoFileName() {
        return videoFileName;
    }

    private boolean moveTempFilesToExperimentDir() {
        File target = new File(experiment.getStorageDir(), getVideoFileName());
        return StorageLib.moveFile(videoFile, target);
    }

    private void finishExperiment(boolean startAnalysis) {
        unsavedExperimentData = false;

        try {
            if (!moveTempFilesToExperimentDir())
                throw new IOException();
            ((CameraExperiment)experiment).setVideoFileName(getVideoFileName());
            experiment.saveExperimentDataToFile();
            Intent data = new Intent();
            File outputDir = experiment.getStorageDir();
            data.putExtra("experiment_path", outputDir.getPath());
            data.putExtra("start_analysis", startAnalysis);
            setResult(RESULT_OK, data);
        } catch (IOException e) {
            e.printStackTrace();
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    // Use the actual camera preview and video sizes to find the largest matching size for preview and recording.
    private Camera.Size getOptimalVideoSize(Camera camera) {
        List<Camera.Size> videoSizes = camera.getParameters().getSupportedVideoSizes();
        assert videoSizes != null;
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        assert previewSizes != null;

        Collections.sort(previewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size size, Camera.Size size2) {
                Integer area1 = size.height * size.width;
                Integer area2 = size2.height * size2.width;
                return area2.compareTo(area1);
            }
        });

        for (Camera.Size previewSize : previewSizes) {
            for (Camera.Size VideoSize : videoSizes) {
                if (previewSize.equals(VideoSize))
                    return previewSize;
            }
        }
        return null;
    }

    // find a supported camcorder profile with best quality
    private CamcorderProfile getOptimalCamcorderProfile(int cameraId) {
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF))
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QCIF);
        return null;
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                camera.setPreviewDisplay(previewHolder);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    abstract class AbstractViewState {
        abstract public void enterState();
        abstract public void leaveState();
        public void onRecordClicked() {}
        public void onStopClicked() {}
        public void onNewClicked() {}
    }

    void setState(AbstractViewState newState) {
        if (state != null)
            state.leaveState();
        state = newState;
        if (state == null) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            newButton.setVisibility(View.INVISIBLE);

            preview.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.INVISIBLE);
        } else
            state.enterState();
    }

    class PreviewState extends AbstractViewState {
        public void enterState() {
            unsavedExperimentData = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            newButton.setVisibility(View.INVISIBLE);

            analyseMenuItem.setEnabled(false);

            preview.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.INVISIBLE);

            camera.startPreview();
        }

        public void leaveState() {
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
        final int orientation = getResources().getConfiguration().orientation;
        final int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                break;
            case Surface.ROTATION_90:
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                break;
            case Surface.ROTATION_180:
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                break;
            case Surface.ROTATION_270:
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
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

            preview.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.INVISIBLE);

            startRecording();
            isRecording = true;

            // disable screen rotation during recording
            initialRequestedOrientation = lockScreenOrientation();
        }

        public void leaveState() {
            if (isRecording) {
                stopRecording();
                isRecording = false;
            }
            unsavedExperimentData = true;
            camera.stopPreview();

            setRequestedOrientation(initialRequestedOrientation);
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

            analyseMenuItem.setEnabled(true);

            preview.setVisibility(View.INVISIBLE);
            videoView.setVisibility(View.VISIBLE);

            if (videoFile == null)
                return;
            videoView.setVideoPath(videoFile.getPath());
            videoView.requestFocus();
            videoView.start();
        }

        public void leaveState() {
            videoView.stopPlayback();
        }

        @Override
        public void onNewClicked() {
            setState(new PreviewState());
        }
    }
}
