/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.microphone;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import nz.ac.auckland.lablet.R;
import nz.ac.auckland.lablet.experiment.AbstractExperimentSensor;
import nz.ac.auckland.lablet.experiment.AbstractExperimentSensorView;
import nz.ac.auckland.lablet.misc.AudioWavInputStream;
import nz.ac.auckland.lablet.views.plotview.PlotView;
import nz.ac.auckland.lablet.views.plotview.StrategyPainter;
import nz.ac.auckland.lablet.views.plotview.ThreadStrategyPainter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;


class PreviewViewState implements AbstractExperimentSensor.State {
    final protected View mainView;
    final protected MicrophoneExperimentSensor experimentSensor;

    protected PlotView amplitudePlotView;
    protected AudioAmplitudePlotDataAdapter amplitudePlotAdapter;

    protected PlotView frequencyMapPlotView;
    protected AudioFrequencyMapAdapter frequencyMapAdapter;

    private MicrophoneExperimentSensor.ISensorDataListener listener = new MicrophoneExperimentSensor.ISensorDataListener() {
        @Override
        public void onNewAmplitudeData(float[] amplitudes) {
            if (amplitudePlotAdapter.getTotalTime() >= MicrophoneExperimentSensorView.Settings.timeSpan)
                amplitudePlotAdapter.clear();
            amplitudePlotAdapter.addData(amplitudes);
        }

        @Override
        public void onNewFrequencyData(float[] frequencies) {
            int size = frequencyMapAdapter.getSize();
            if (size > 0 && frequencyMapAdapter.getX(size - 1) >= MicrophoneExperimentSensorView.Settings.timeSpan)
                frequencyMapAdapter.clear();
            frequencyMapAdapter.addData(frequencies);
        }
    };

    public PreviewViewState(View mainView, MicrophoneExperimentSensor experimentSensor) {
        this.mainView = mainView;
        this.experimentSensor = experimentSensor;

        ThreadStrategyPainter strategyPainter = new ThreadStrategyPainter();
        strategyPainter.setForceInvalidateOnRangeChanged(false);
        //StrategyPainter strategyPainter = new BufferedDirectStrategyPainter();
        amplitudePlotAdapter = new AudioAmplitudePlotDataAdapter();
        amplitudePlotView = (PlotView)mainView.findViewById(R.id.audioSignalView);
        AudioAmplitudePainter audioAmplitudePainter = new AudioAmplitudePainter(amplitudePlotAdapter);
        strategyPainter.addChild(audioAmplitudePainter);
        amplitudePlotView.addPlotPainter(strategyPainter);

        frequencyMapPlotView = (PlotView)mainView.findViewById(R.id.audioFrequencyMapPlot);
        frequencyMapAdapter = new AudioFrequencyMapAdapter(experimentSensor.getLiveStepFactor());
        strategyPainter = new ThreadStrategyPainter();
        strategyPainter.setForceInvalidateOnRangeChanged(false);
        AudioFrequencyMapConcurrentPainter audioFrequencyMapPainter
                = new AudioFrequencyMapConcurrentPainter(frequencyMapAdapter);
        strategyPainter.addChild(audioFrequencyMapPainter);
        frequencyMapPlotView.addPlotPainter(strategyPainter);

    }

    @Override
    public void start() {
        amplitudePlotAdapter.clear();
        frequencyMapAdapter.clear();

        amplitudePlotView.setXRange(0, MicrophoneExperimentSensorView.Settings.timeSpan);
        amplitudePlotView.setYRange(MicrophoneExperimentSensorView.Settings.amplitudeMin,
                MicrophoneExperimentSensorView.Settings.amplitudeMax);
        amplitudePlotView.getTitleView().setTitle("Signal Strength Vs Time");
        amplitudePlotView.getXAxisView().setUnit(experimentSensor.getTimeUnit());
        amplitudePlotView.getXAxisView().setTitle("Time");
        //amplitudePlotView.getBackgroundPainter().setShowYGrid(true);

        frequencyMapPlotView.setXRange(0, MicrophoneExperimentSensorView.Settings.timeSpan);
        frequencyMapPlotView.setYRange(1, experimentSensor.SAMPLE_RATE / 2);
        frequencyMapPlotView.setMaxXRange(0, MicrophoneExperimentSensorView.Settings.timeSpan);
        frequencyMapPlotView.setMaxYRange(1, experimentSensor.SAMPLE_RATE / 2);
        //frequencyMapPlotView.setYScale(PlotView.log10Scale());
        frequencyMapPlotView.setAutoRange(PlotView.AUTO_RANGE_DISABLED, PlotView.AUTO_RANGE_DISABLED);
        frequencyMapPlotView.setXDraggable(true);
        frequencyMapPlotView.setYDraggable(true);
        frequencyMapPlotView.setXZoomable(true);
        frequencyMapPlotView.setYZoomable(true);
        frequencyMapPlotView.getYAxisView().setUnit(experimentSensor.getFrequencyUnit());
        frequencyMapPlotView.getYAxisView().setTitle("Frequency");
        frequencyMapPlotView.getXAxisView().setUnit(experimentSensor.getTimeUnit());
        frequencyMapPlotView.getXAxisView().setTitle("Time");

        mainView.setVisibility(View.VISIBLE);
        experimentSensor.setSensorDataListener(listener);
    }

    @Override
    public boolean stop() {
        experimentSensor.setSensorDataListener(null);

        amplitudePlotAdapter.clear();
        frequencyMapAdapter.clear();

        mainView.setVisibility(View.INVISIBLE);
        return true;
    }
}

class RecordingViewState implements AbstractExperimentSensor.State {
    final protected View mainView;
    final protected MicrophoneExperimentSensor experimentSensor;

    final protected PlotView amplitudePlotView;
    final protected AudioAmplitudePlotDataAdapter amplitudePlotAdapter;

    final protected PlotView frequencyMapPlotView;
    final protected AudioFrequencyMapAdapter frequencyMapAdapter;

    public RecordingViewState(PreviewViewState previewViewState) {
        mainView = previewViewState.mainView;
        experimentSensor = previewViewState.experimentSensor;

        amplitudePlotView = previewViewState.amplitudePlotView;
        amplitudePlotAdapter = previewViewState.amplitudePlotAdapter;

        frequencyMapPlotView = previewViewState.frequencyMapPlotView;
        frequencyMapAdapter = previewViewState.frequencyMapAdapter;
    }
//TODO: discard data in adapter...
    private MicrophoneExperimentSensor.ISensorDataListener listener = new MicrophoneExperimentSensor.ISensorDataListener() {
        @Override
        public void onNewAmplitudeData(float[] amplitudes) {
            amplitudePlotAdapter.addData(amplitudes);
        }

        @Override
        public void onNewFrequencyData(float[] frequencies) {
            frequencyMapAdapter.addData(frequencies);
        }
    };

    @Override
    public void start() {
        amplitudePlotAdapter.clear();
        frequencyMapAdapter.clear();

        amplitudePlotView.setXRange(0, MicrophoneExperimentSensorView.Settings.timeSpan);
        amplitudePlotView.setYRange(MicrophoneExperimentSensorView.Settings.amplitudeMin,
                MicrophoneExperimentSensorView.Settings.amplitudeMax);
        amplitudePlotView.getTitleView().setTitle("Signal Strength Vs Time");
        amplitudePlotView.getXAxisView().setUnit(experimentSensor.getTimeUnit());
        amplitudePlotView.getXAxisView().setTitle("Time");
        //amplitudePlotView.getBackgroundPainter().setShowYGrid(true);
        amplitudePlotView.setAutoRange(PlotView.AUTO_RANGE_SCROLL, PlotView.AUTO_RANGE_DISABLED);

        frequencyMapPlotView.setXRange(0, MicrophoneExperimentSensorView.Settings.timeSpan);
        frequencyMapPlotView.setYRange(1, experimentSensor.SAMPLE_RATE / 2);
        frequencyMapPlotView.setMaxXRange(Float.MAX_VALUE, Float.MAX_VALUE);
        frequencyMapPlotView.setMaxYRange(Float.MAX_VALUE, Float.MAX_VALUE);
        frequencyMapPlotView.setXDraggable(false);
        frequencyMapPlotView.setYDraggable(false);
        frequencyMapPlotView.setXZoomable(false);
        frequencyMapPlotView.setYZoomable(false);
        frequencyMapPlotView.setAutoRange(PlotView.AUTO_RANGE_SCROLL, PlotView.AUTO_RANGE_DISABLED);
        frequencyMapPlotView.getYAxisView().setUnit(experimentSensor.getFrequencyUnit());
        frequencyMapPlotView.getYAxisView().setTitle("Frequency");
        frequencyMapPlotView.getXAxisView().setUnit(experimentSensor.getTimeUnit());
        frequencyMapPlotView.getXAxisView().setTitle("Time");


        mainView.setVisibility(View.VISIBLE);
        experimentSensor.setSensorDataListener(listener);
    }

    @Override
    public boolean stop() {
        experimentSensor.setSensorDataListener(null);

        amplitudePlotAdapter.clear();
        frequencyMapAdapter.clear();

        mainView.setVisibility(View.INVISIBLE);
        return true;
    }
}

class PlaybackViewState implements AbstractExperimentSensor.State {
    final View playbackView;
    final MicrophoneExperimentSensor experimentSensor;

    final private ToggleButton startPauseButton;
    final private SeekBar seekBar;
    final private TextView lengthTextView;

    final private PlotView frequencyMapPlotView;
    final private AudioFrequencyMapAdapter frequencyMapAdapter;

    final float DEFAULT_STEP_FACTOR = 0.5f;
    final int DEFAULT_WINDOW_SIZE = 4096;

    private MediaPlayer mediaPlayer;

    final private Runnable seekBarUpdater = new Runnable() {
        private Handler handler = new Handler();

        @Override
        public void run() {
            if (mediaPlayer == null)
                return;
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            if (!mediaPlayer.isPlaying())
                return;
            handler.postDelayed(seekBarUpdater, 100);
        }
    };

    public PlaybackViewState(View playbackView, MicrophoneExperimentSensor experimentSensor) {
        this.playbackView = playbackView;
        this.experimentSensor = experimentSensor;

        startPauseButton = (ToggleButton)playbackView.findViewById(R.id.startPauseButton);
        seekBar = (SeekBar)playbackView.findViewById(R.id.seekBar);
        lengthTextView = (TextView)playbackView.findViewById(R.id.lengthTextView);
        frequencyMapPlotView = (PlotView)playbackView.findViewById(R.id.frequencyMapPlotView);

        // frequency map
        StrategyPainter strategyPainter = new ThreadStrategyPainter();
        frequencyMapAdapter = new AudioFrequencyMapAdapter(DEFAULT_STEP_FACTOR);
        strategyPainter.addChild(new AudioFrequencyMapConcurrentPainter(frequencyMapAdapter));
        frequencyMapPlotView.addPlotPainter(strategyPainter);

        startPauseButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (mediaPlayer == null)
                    return;
                if (checked) {
                    mediaPlayer.start();
                    seekBarUpdater.run();
                } else
                    mediaPlayer.pause();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mediaPlayer.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void loadWavFileAsync() {
        frequencyMapAdapter.clear();

        final IFrequencyMapLoader frequencyMapLoader = FrequencyMapLoaderFactory.create(frequencyMapAdapter,
                experimentSensor.getAudioFile());
        final AudioWavInputStream audioWavInputStream;
        try {
            audioWavInputStream = new AudioWavInputStream(experimentSensor.getAudioFile());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final int totalTime = audioWavInputStream.getDurationMilliSeconds();

        lengthTextView.setText(Integer.toString(totalTime) + " [ms]");

        frequencyMapLoader.loadWavFile(audioWavInputStream, new Runnable() {
            @Override
            public void run() {
                frequencyMapLoader.updateFrequencies(playbackView.getContext(), frequencyMapAdapter.getStepFactor(),
                        DEFAULT_WINDOW_SIZE, new IFrequencyMapLoader.IFrequenciesUpdatedListener() {
                            @Override
                            public void onFrequenciesUpdated(boolean canceled) {
                                frequencyMapPlotView.setMaxXRange(0, totalTime);
                                frequencyMapPlotView.setMaxYRange(1, experimentSensor.SAMPLE_RATE / 2);
                                frequencyMapPlotView.setXRange(0, totalTime);
                                frequencyMapPlotView.setYRange(1, experimentSensor.SAMPLE_RATE / 2);
                                frequencyMapPlotView.setXDraggable(true);
                                frequencyMapPlotView.setYDraggable(true);
                                frequencyMapPlotView.setXZoomable(true);
                                frequencyMapPlotView.setYZoomable(true);
                            }
                        });
            }
        });
    }

    @Override
    public void start() {
        playbackView.setVisibility(View.VISIBLE);

        loadWavFileAsync();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    startPauseButton.setChecked(false);
                    mediaPlayer.seekTo(0);
                }
            });

            mediaPlayer.setDataSource(experimentSensor.getAudioFile().getPath());
            mediaPlayer.prepare();

        } catch (IOException e) {
            mediaPlayer = null;
            e.printStackTrace();
            return;
        }

        int duration = mediaPlayer.getDuration();
        seekBar.setMax(duration);
    }

    @Override
    public boolean stop() {
        playbackView.setVisibility(View.INVISIBLE);

        frequencyMapAdapter.clear();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        return false;
    }
}

class MicrophoneExperimentSensorView extends AbstractExperimentSensorView {
    static public class Settings {
        public static int timeSpan = 8 * 1000;
        public static float amplitudeMax = 0.52f;
        public static float amplitudeMin = -0.52f;
    }

    public MicrophoneExperimentSensorView(final Context context, final MicrophoneExperimentSensor experimentSensor) {
        super(context);

        final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewGroup view = (ViewGroup)inflater.inflate(R.layout.microphone_experiment_view, null, false);
        assert view != null;
        addView(view);

        View previewView = view.findViewById(R.id.previewView);
        View playbackView = view.findViewById(R.id.playbackView);

        previewView.setVisibility(View.INVISIBLE);
        playbackView.setVisibility(View.INVISIBLE);

        previewState = new PreviewViewState(previewView, experimentSensor);
        recordingState = new RecordingViewState((PreviewViewState)previewState);
        playbackState = new PlaybackViewState(playbackView, experimentSensor);
    }

    @Override
    public void onSettingsChanged() {

    }
}
