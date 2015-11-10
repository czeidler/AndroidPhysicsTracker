/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.accelerometer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import nz.ac.auckland.lablet.experiment.AbstractExperimentSensor;
import nz.ac.auckland.lablet.experiment.AbstractExperimentSensorView;
import nz.ac.auckland.lablet.views.plotview.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Sensor view for an accelerometer experiment.
 */
class AccelerometerExperimentView extends AbstractExperimentSensorView {
    final private AccelerometerExperimentSensor sensor;
    final private AccelerometerSensorData.IListener dataListenerStrongRef;

    final private PlotView plotView;
    final private XYDataAdapter xData;
    final private XYDataAdapter yData;
    final private XYDataAdapter zData;
    final private XYDataAdapter totalData = new XYDataAdapter();

    class PreviewViewState implements AbstractExperimentSensor.State {
        @Override
        public void start() {
            xData.setTo(null, null);
            yData.setTo(null, null);
            zData.setTo(null, null);
            totalData.clear();

            plotView.setMaxXRange(Float.MAX_VALUE, Float.MAX_VALUE);
            plotView.setMaxYRange(Float.MAX_VALUE, Float.MAX_VALUE);
            plotView.setXRange(0, 8000);
            plotView.setYRange(-0.5f, 0.5f);
            plotView.setDraggable(false);
            plotView.setZoomable(false);
            plotView.setAutoRange(PlotView.AUTO_RANGE_SCROLL, PlotView.AUTO_RANGE_ZOOM_EXTENDING);
            plotView.getXAxisView().setUnit(sensor.getXUnit());
            plotView.getXAxisView().setTitle("Time");
            plotView.getYAxisView().setUnit(sensor.getYUnit());
            plotView.getYAxisView().setTitle("Acceleration");
        }

        @Override
        public boolean stop() {
            return true;
        }
    }

    class PlaybackViewState implements AbstractExperimentSensor.State {
        @Override
        public void start() {
            AccelerometerSensorData data = (AccelerometerSensorData)sensor.getExperimentData();

            List<Number> timeCopy = new ArrayList<>(data.getTimeValues().size());
            timeCopy.addAll(data.getTimeValues());
            List<Number> xCopy = new ArrayList<>(data.getXValues().size());
            xCopy.addAll(data.getXValues());
            List<Number> yCopy = new ArrayList<>(data.getYValues().size());
            yCopy.addAll(data.getYValues());
            List<Number> zCopy = new ArrayList<>(data.getZValues().size());
            zCopy.addAll(data.getZValues());

            xData.setTo(timeCopy, xCopy);
            yData.setTo(timeCopy, yCopy);
            zData.setTo(timeCopy, zCopy);

            plotView.setAutoRange(PlotView.AUTO_RANGE_DISABLED, PlotView.AUTO_RANGE_DISABLED);

            plotView.autoZoom();
            RectF range = plotView.getRange();
            plotView.setMaxRange(range);
            plotView.setDraggable(true);
            plotView.setZoomable(true);
        }

        @Override
        public boolean stop() {
            return true;
        }
    }

    public AccelerometerExperimentView(Context context, AccelerometerExperimentSensor sensor) {
        super(context);

        this.sensor = sensor;

        this.previewState = new PreviewViewState();
        this.recordingState =  previewState;
        this.playbackState = new PlaybackViewState();

        plotView = new PlotView(context);
        plotView.getTitleView().setTitle("Accelerometer");
        plotView.getBackgroundPainter().setShowXGrid(true);
        plotView.getBackgroundPainter().setShowYGrid(true);
        addView(plotView);

        final AccelerometerSensorData data = (AccelerometerSensorData)sensor.getExperimentData();
        dataListenerStrongRef = new AccelerometerSensorData.IListener() {
            @Override
            public void onDataAdded(long time, float[] data) {
                final float x = data[0];
                final float y = data[1];
                final float z = data[2];

                xData.addData(time, x);
                yData.addData(time, y);
                zData.addData(time, z);
                totalData.addData(time, (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
            }

            @Override
            public void onDataCleared() {

            }
        };

        data.addListener(dataListenerStrongRef);

        StrategyPainter strategyPainter = new ThreadStrategyPainter();

        xData = new XYDataAdapter();
        XYConcurrentPainter xPainter = new XYConcurrentPainter(xData, getContext());
        strategyPainter.addChild(xPainter);

        yData = new XYDataAdapter();
        XYConcurrentPainter yPainter = new XYConcurrentPainter(yData, getContext());
        yPainter.setPointRenderer(new CircleRenderer());
        Paint yMarkerPaint = new Paint();
        yMarkerPaint.setColor(Color.BLUE);
        yPainter.getDrawConfig().setMarkerPaint(yMarkerPaint);
        strategyPainter.addChild(yPainter);

        zData = new XYDataAdapter();
        XYConcurrentPainter zPainter = new XYConcurrentPainter(zData, getContext());
        Paint zMarkerPaint = new Paint();
        zMarkerPaint.setColor(Color.RED);
        zPainter.getDrawConfig().setMarkerPaint(zMarkerPaint);
        zPainter.setPointRenderer(new BottomTriangleRenderer());
        strategyPainter.addChild(zPainter);

        XYConcurrentPainter totalPainter = new XYConcurrentPainter(totalData, getContext());
        Paint totalMarkerPaint = new Paint();
        totalMarkerPaint.setColor(Color.WHITE);
        totalPainter.getDrawConfig().setMarkerPaint(totalMarkerPaint);
        totalPainter.setPointRenderer(new CircleRenderer());
        strategyPainter.addChild(totalPainter);

        plotView.addPlotPainter(strategyPainter);

        LegendPainter legend = new LegendPainter();
        legend.addEntry(xPainter, "x-acceleration");
        legend.addEntry(yPainter, "y-acceleration");
        legend.addEntry(zPainter, "z-acceleration");
        legend.addEntry(totalPainter, "total-acceleration");
        plotView.addForegroundPainter(legend);
    }

    @Override
    public void onSettingsChanged() {

    }

}
