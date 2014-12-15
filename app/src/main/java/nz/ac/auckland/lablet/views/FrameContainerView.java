/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import nz.ac.auckland.lablet.camera.MotionAnalysis;
import nz.ac.auckland.lablet.experiment.CalibrationXY;
import nz.ac.auckland.lablet.experiment.FrameDataModel;
import nz.ac.auckland.lablet.experiment.MarkerData;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;


/**
 * Container for the {@link IExperimentFrameView} and a marker view overlay.
 * <p>
 * The resize behaviour of the run view is copied and the marker view is put exactly on top of the run view. In this way
 * the screen coordinates of the run view and the marker view are the same.
 * </p>
 */
public class FrameContainerView extends RelativeLayout {
    private View sensorAnalysisView = null;
    private MarkerView markerView = null;
    private TagMarkerDataModelPainter painter = null;
    private FrameDataModel frameDataModel = null;
    private MotionAnalysis sensorAnalysis = null;
    private OriginMarkerPainter originMarkerPainter = null;

    private GestureDetector gestureDetector;

    private FrameDataModel.IFrameDataModelListener frameDataModelListener = new FrameDataModel.IFrameDataModelListener() {
        @Override
        public void onFrameChanged(int newFrame) {
            ((IExperimentFrameView) sensorAnalysisView).setCurrentFrame(newFrame);
            markerView.setCurrentFrame(newFrame);
            markerView.invalidate();
        }

        @Override
        public void onNumberOfFramesChanged() {

        }
    };

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (painter == null)
                return super.onSingleTapUp(e);

            IMarker currentMarker = painter.getMarkerForRow(painter.getMarkerModel().getSelectedMarkerData());
            if (currentMarker != null && currentMarker.isSelectedForDrag())
                return super.onSingleTapUp(e);

            IMarker tappedMarker = painter.getMarkerAtScreenPosition(new PointF(e.getX(), e.getY()));
            if (tappedMarker == null)
                return super.onSingleTapUp(e);
            int tappedMarkerIndex = painter.markerIndexOf(tappedMarker);

            MarkerDataModel markerDataModel = painter.getMarkerModel();
            frameDataModel.setCurrentFrame(markerDataModel.getMarkerDataAt(tappedMarkerIndex).getRunId());
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            int currentFrame = frameDataModel.getCurrentFrame();
            if (frameDataModel.getCurrentFrame() < frameDataModel.getNumberOfFrames() - 1)
                frameDataModel.setCurrentFrame(currentFrame + 1);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    private MotionAnalysis.IListener motionAnalysisListener = new MotionAnalysis.IListener() {
        @Override
        public void onShowCoordinateSystem(boolean show) {
            if (show)
                markerView.addPlotPainter(originMarkerPainter);
            else
                markerView.removePlotPainter(originMarkerPainter);
        }
    };

    public FrameContainerView(Context context) {
        super(context);
    }

    public FrameContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void finalize() {
        release();

        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void setTo(View runView, MotionAnalysis analysis) {
        if (sensorAnalysis != null)
            sensorAnalysis.removeListener(motionAnalysisListener);
        sensorAnalysis = analysis;
        sensorAnalysis.addListener(motionAnalysisListener);

        if (frameDataModel != null)
            frameDataModel.removeListener(frameDataModelListener);
        frameDataModel = sensorAnalysis.getFrameDataModel();
        frameDataModel.addListener(frameDataModelListener);

        sensorAnalysisView = runView;

        sensorAnalysisView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                int parentWidth = sensorAnalysisView.getMeasuredWidth();
                int parentHeight = sensorAnalysisView.getMeasuredHeight();
                markerView.setSize(parentWidth, parentHeight);
            }
        });

        // run view
        RelativeLayout.LayoutParams runViewParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        addView(sensorAnalysisView, runViewParams);

        // marker view
        RelativeLayout.LayoutParams makerViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        makerViewParams.addRule(RelativeLayout.ALIGN_LEFT, sensorAnalysisView.getId());
        makerViewParams.addRule(RelativeLayout.ALIGN_TOP, sensorAnalysisView.getId());
        makerViewParams.addRule(RelativeLayout.ALIGN_RIGHT, sensorAnalysisView.getId());
        makerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, sensorAnalysisView.getId());

        markerView = new MarkerView(getContext());
        addView(markerView, makerViewParams);

        RectF range = ((IExperimentFrameView) sensorAnalysisView).getDataRange();
        markerView.setRange(range);
        markerView.setMaxRange(range);

        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        markerView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });
    }

    public void addTagMarkerData(MarkerDataModel data) {
        painter = new TagMarkerDataModelPainter(data);
        markerView.addPlotPainter(painter);

        frameDataModelListener.onFrameChanged(frameDataModel.getCurrentFrame());
    }

    public void addXYCalibrationData(MarkerDataModel data) {
        CalibrationMarkerPainter painter = new CalibrationMarkerPainter(data);
        markerView.addPlotPainter(painter);
    }

    public void removeOriginData() {
        markerView.removePlotPainter(originMarkerPainter);
    }

    public void addOriginData(MarkerDataModel data, CalibrationXY calibrationXY) {
        originMarkerPainter = new OriginMarkerPainter(data, calibrationXY);
        if (sensorAnalysis.getShowCoordinateSystem())
            markerView.addPlotPainter(originMarkerPainter);
    }

    public void release() {
        if (markerView != null)
            markerView.release();
        if (sensorAnalysis != null)
            sensorAnalysis.removeListener(motionAnalysisListener);
        if (frameDataModel != null)
            frameDataModel.removeListener(frameDataModelListener);
    }

    /**
     * Copy resize behaviour of the sensorAnalysisView.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (sensorAnalysis == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int specWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int specHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        sensorAnalysisView.measure(widthMeasureSpec, heightMeasureSpec);

        int width = sensorAnalysisView.getMeasuredWidth();
        int height = sensorAnalysisView.getMeasuredHeight();

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, specWidthMode),
                MeasureSpec.makeMeasureSpec(height, specHeightMode));
    }
}
