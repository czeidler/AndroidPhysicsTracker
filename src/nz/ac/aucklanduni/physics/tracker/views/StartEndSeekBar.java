/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import nz.ac.aucklanduni.physics.tracker.IExperimentRunView;
import nz.ac.aucklanduni.physics.tracker.MarkerData;
import nz.ac.aucklanduni.physics.tracker.MarkersDataModel;


abstract class StartEndMarker extends DragableMarker {
    // dimensions in density-independent  pixels
    private final float WIDTH_DP = 20;

    // dimensions in pixels, calculated in the constructor; use them for drawing
    protected float WIDTH;
    protected float HEIGHT;
    protected int TRIANGLE_HEIGHT;

    protected Paint lightColor = new Paint();
    protected Paint darkenColor = new Paint();

    public StartEndMarker(AbstractMarkersPainter parentContainer) {
        super(parentContainer);

        lightColor.setColor(Color.rgb(97, 204, 238));
        lightColor.setAntiAlias(true);
        lightColor.setStyle(Paint.Style.FILL);

        darkenColor.setColor(Color.rgb(30, 146, 209));
        darkenColor.setAntiAlias(true);
        darkenColor.setStyle(Paint.Style.FILL);
        darkenColor.setPathEffect(new CornerPathEffect(2));

        WIDTH = parent.toPixel(WIDTH_DP);
        HEIGHT = parent.toPixel(StartEndSeekBar.HEIGHT_DP);
        TRIANGLE_HEIGHT = parent.toPixel(StartEndSeekBar.HEIGHT_DP * 0.5f);
    }

    @Override
    protected boolean isPointOnSelectArea(PointF point) {
        // build a marker rect with increased width
        final float touchWidth = (float)parent.toPixel(60);
        RectF rect = new RectF();
        rect.left = position.x - touchWidth / 2;
        rect.top = position.y;
        rect.right = position.x + touchWidth / 2;
        rect.bottom = position.y + HEIGHT;

        return rect.contains(point.x, point.y);
    }

    protected void onDraggedTo(PointF point) {
        parent.markerMoveRequest(this, point);
    }
}

class StartMarker extends StartEndMarker {

    public StartMarker(AbstractMarkersPainter parentContainer) {
        super(parentContainer);
    }

    @Override
    public void onDraw(Canvas canvas, float priority) {
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

class EndMarker extends StartEndMarker {

    public EndMarker(AbstractMarkersPainter parentContainer) {
        super(parentContainer);
    }

    @Override
    public void onDraw(Canvas canvas, float priority) {
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


class StartEndPainter extends AbstractMarkersPainter {
    int numberOfSteps = 10;

    public StartEndPainter(View parent, IExperimentRunView runView, MarkersDataModel data) {
        super(parent, runView, data);
    }

    @Override
    protected DragableMarker createMarkerForRow(int row) {
        if (row == 0)
            return new StartMarker(this);
        else
            return new EndMarker(this);
    }

    @Override
    public void draw(Canvas canvas, float priority) {
        for (IMarker marker : markerList)
            marker.onDraw(canvas, priority);
    }

    @Override
    public void markerMoveRequest(DragableMarker marker, PointF newPosition) {
        int row = markerList.lastIndexOf(marker);
        if (row < 0)
            return;

        PointF newReal = new PointF();
        sanitizeScreenPoint(newPosition);
        experimentRunView.fromScreen(newPosition, newReal);
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
        float stepSize = 1.0f / (numberOfSteps - 1);
        int stepPosition = Math.round(floatPosition / stepSize);
        return stepSize * stepPosition;
    }
}


public class StartEndSeekBar extends MarkerView implements IExperimentRunView {
    private MarkersDataModel markersDataModel;
    private StartEndPainter startEndPainter;

    // dimensions in density-independent  pixels
    public static final float HEIGHT_DP = 35;

    public StartEndSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        markersDataModel = new MarkersDataModel();
        markersDataModel.addMarkerData(new MarkerData(0));
        markersDataModel.addMarkerData(new MarkerData(1));

        startEndPainter = new StartEndPainter(this, this, markersDataModel);
        addMarkerPainter(startEndPainter);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (IMarkerDataModelPainter markerPainter : markerPainterList)
            markerPainter.draw(canvas, 1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LayoutParams params = getLayoutParams();
        assert params != null;
        setMeasuredDimension(params.width, startEndPainter.toPixel(HEIGHT_DP));
    }

    public MarkersDataModel getMarkersDataModel() {
        return markersDataModel;
    }

    @Override
    public void setCurrentRun(int run) {

    }

    @Override
    public void fromScreen(PointF screen, PointF real) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int barWidth = viewFrame.width() - paddingLeft - paddingRight;

        if (screen.x < paddingLeft)
            screen.x = paddingLeft;
        if (screen.x > viewFrame.width() - paddingRight)
            screen.x = viewFrame.width() - paddingRight;

        screen.x -= paddingLeft;

        real.x = screen.x / barWidth;
        real.y = 0;
    }

    @Override
    public void toScreen(PointF real, PointF screen) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int barWidth = viewFrame.width() - paddingLeft - paddingRight;

        screen.x = paddingLeft + real.x * barWidth;
        screen.y = 0;
    }

    @Override
    public float getMaxRawX() {
        return 100;
    }

    @Override
    public float getMaxRawY() {
        return 100;
    }

    public void setMax(int max) {
        startEndPainter.setNumberOfSteps(max + 1);
    }
}
