/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import nz.ac.auckland.lablet.experiment.MarkerData;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;
import nz.ac.auckland.lablet.views.plotview.IPlotPainter;
import nz.ac.auckland.lablet.views.plotview.PlotPainterContainerView;
import nz.ac.auckland.lablet.views.plotview.RangeDrawingView;


/**
 * Abstract base class that shares code for the start and the end marker.
 */
abstract class StartEndMarker extends DraggableMarker {
    // dimensions in density-independent  pixels
    private final float WIDTH_DP = 20;

    // dimensions in pixels, calculated in the constructor; use them for drawing
    protected float WIDTH;
    protected float HEIGHT;
    protected int TRIANGLE_HEIGHT;

    protected Paint lightColor = new Paint();
    protected Paint darkenColor = new Paint();

    public StartEndMarker() {
        lightColor.setColor(Color.rgb(97, 204, 238));
        lightColor.setAntiAlias(true);
        lightColor.setStyle(Paint.Style.FILL);

        darkenColor.setColor(Color.rgb(30, 146, 209));
        darkenColor.setAntiAlias(true);
        darkenColor.setStyle(Paint.Style.FILL);
        darkenColor.setPathEffect(new CornerPathEffect(2));
    }

    @Override
    public void setTo(AbstractMarkerPainter painter, int markerIndex) {
        super.setTo(painter, markerIndex);

        WIDTH = parent.toPixel(WIDTH_DP);
        HEIGHT = parent.toPixel(StartEndSeekBar.HEIGHT_DP);
        TRIANGLE_HEIGHT = parent.toPixel(StartEndSeekBar.HEIGHT_DP * 0.5f);
    }

    @Override
    protected boolean isPointOnSelectArea(PointF point) {
        PointF position = parent.getMarkerScreenPosition(index);
        // build a marker rect with increased width
        final float touchWidth = (float)parent.toPixel(60);
        RectF rect = new RectF();
        rect.left = position.x - touchWidth / 2;
        rect.top = position.y;
        rect.right = position.x + touchWidth / 2;
        rect.bottom = position.y + HEIGHT;

        return rect.contains(point.x, point.y);
    }
}


/**
 * Implementation of a "start" marker. (Copies the google text select markers.)
 */
class StartMarker extends StartEndMarker {

    @Override
    public void onDraw(Canvas canvas, float priority) {
        PointF position = parent.getMarkerScreenPosition(index);

        // Note: don't use drawRectangle because in some cases it does not align with drawPath for the triangle...

        // darken bottom
        Path path = new Path();
        path.moveTo(position.x, TRIANGLE_HEIGHT);
        path.lineTo(position.x, HEIGHT);
        path.lineTo(position.x - WIDTH, HEIGHT);
        path.lineTo(position.x - WIDTH, TRIANGLE_HEIGHT - 2);
        canvas.drawPath(path, darkenColor);

        // bright triangle
        path = new Path();
        path.moveTo(position.x, 0);
        path.lineTo(position.x, TRIANGLE_HEIGHT);
        path.lineTo(position.x - WIDTH, TRIANGLE_HEIGHT);
        canvas.drawPath(path, lightColor);
    }
}


/**
 * Implementation of a "end" marker. (Copies the google text select markers.)
 */
class EndMarker extends StartEndMarker {

    @Override
    public void onDraw(Canvas canvas, float priority) {
        PointF position = parent.getMarkerScreenPosition(index);

        // Note: don't use drawRectangle because in some cases it does not align with drawPath for the triangle...

        // darken bottom
        Path path = new Path();
        path.moveTo(position.x, TRIANGLE_HEIGHT);
        path.lineTo(position.x, HEIGHT);
        path.lineTo(position.x + WIDTH, HEIGHT);
        path.lineTo(position.x + WIDTH, TRIANGLE_HEIGHT - 2);
        canvas.drawPath(path, darkenColor);

        // bright triangle
        path = new Path();
        path.moveTo(position.x, 0);
        path.lineTo(position.x, TRIANGLE_HEIGHT);
        path.lineTo(position.x + WIDTH, TRIANGLE_HEIGHT);
        canvas.drawPath(path, lightColor);
    }
}


/**
 * Painter for the start and end marker.
 * <p>
 * The used data model should contain exactly two data points.
 * </p>
 */
class StartEndPainter extends AbstractMarkerPainter {
    int numberOfSteps = 10;

    /**
     * Constructor.
     *
     * @param data should contain exactly two data points, one for the start and one for the end marker
     */
    public StartEndPainter(MarkerDataModel data) {
        super(data);
    }

    @Override
    protected DraggableMarker createMarkerForRow(int row) {
        if (row == 0)
            return new StartMarker();
        else
            return new EndMarker();
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (IMarker marker : markerList)
            marker.onDraw(canvas, 1);
    }

    @Override
    public void markerMoveRequest(DraggableMarker marker, PointF newPosition, boolean isDragging) {
        int row = markerList.lastIndexOf(marker);
        if (row < 0)
            return;

        PointF newReal = new PointF();
        sanitizeScreenPoint(newPosition);
        containerView.fromScreen(newPosition, newReal);
        newReal.x = toStepPosition(newReal.x);

        if (row == 0) {
            MarkerData marker2 = markerData.getMarkerDataAt(1);
            if (newReal.x > marker2.getPosition().x)
                markerData.setMarkerPosition(newReal, 1);
        } else {
            MarkerData marker1 = markerData.getMarkerDataAt(0);
            if (newReal.x < marker1.getPosition().x)
                markerData.setMarkerPosition(newReal, 0);
        }
        markerData.setMarkerPosition(newReal, row);
    }

    public void setNumberOfSteps(int steps) {
        numberOfSteps = steps;

        MarkerData marker1 = markerData.getMarkerDataAt(0);
        marker1.getPosition().x = toStepPosition(marker1.getPosition().x);
        markerData.setMarkerPosition(marker1.getPosition(), 0);

        MarkerData marker2 = markerData.getMarkerDataAt(1);
        marker2.getPosition().x = toStepPosition(marker2.getPosition().x);
        markerData.setMarkerPosition(marker2.getPosition(), 1);
    }

    private float toStepPosition(float floatPosition) {
        if (numberOfSteps <= 1)
            return 0.5f;
        float stepSize = 1.0f / numberOfSteps;
        int stepPosition = Math.round(floatPosition / stepSize);
        return stepSize * stepPosition;
    }
}


/**
 * A seek bar with a start and an end marker. For example, used to select video start and end point.
 */
public class StartEndSeekBar extends PlotPainterContainerView {
    private MarkerDataModel markerDataModel;
    private StartEndPainter startEndPainter;

    // dimensions in density-independent  pixels
    public static final float HEIGHT_DP = 35;

    public StartEndSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        markerDataModel = new MarkerDataModel();
        markerDataModel.addMarkerData(new MarkerData(0));
        markerDataModel.addMarkerData(new MarkerData(1));

        startEndPainter = new StartEndPainter(markerDataModel);
        addPlotPainter(startEndPainter);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (IPlotPainter markerPainter : allPainters)
            markerPainter.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LayoutParams params = getLayoutParams();
        assert params != null;
        setMeasuredDimension(params.width, startEndPainter.toPixel(HEIGHT_DP));
    }

    public MarkerDataModel getMarkerDataModel() {
        return markerDataModel;
    }

    @Override
    public float toScreenX(float real) {
        float paddingLeft = getPaddingLeft();
        float paddingRight = getPaddingRight();
        float width = viewWidth - paddingLeft - paddingRight;
        return paddingLeft + (real - rangeRect.left) / (rangeRect.right - rangeRect.left) * width;
    }

    @Override
    public float toScreenY(float real) {
        float paddingTop = getPaddingTop();
        float paddingBottom = getPaddingBottom();
        float height = viewHeight - paddingTop - paddingBottom;
        return paddingTop + (1.f - (real - rangeRect.bottom) / (rangeRect.top - rangeRect.bottom)) * height;
    }

    /**
     * Set range from 0 to max.
     *
     * @param max the end of the range
     */
    public void setMax(int max) {
        startEndPainter.setNumberOfSteps(max);
    }
}
