/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views;

import android.graphics.*;
import nz.ac.auckland.lablet.experiment.MarkerData;
import nz.ac.auckland.lablet.experiment.MarkerDataModel;

import java.util.List;


abstract class CursorMarker extends DraggableMarker {
    final private Paint cursorPaint = new Paint();
    final private Paint textPaint = new Paint();
    final private Paint textBackgroundPaint = new Paint();

    // device independent pixels
    private class Const {
        static public final float SELECT_RADIUS_DP = 30;
    }

    private float SELECT_RADIUS;

    @Override
    public void setTo(AbstractMarkerPainter painter, MarkerData markerData) {
        super.setTo(painter, markerData);

        SELECT_RADIUS = parent.toPixel(Const.SELECT_RADIUS_DP);
    }

    @Override
    public boolean isPointOnSelectArea(PointF screenPoint) {
        PointF position = getCachedScreenPosition();
        float distance = Math.abs(getDirection(screenPoint) - getDirection(position));
        return distance <= SELECT_RADIUS;
    }

    @Override
    protected boolean isPointOnDragArea(PointF screenPoint) {
        PointF position = getCachedScreenPosition();
        float distance = Math.abs(getDirection(screenPoint)- getDirection(position));
        if (distance < SELECT_RADIUS)
            return true;
        return false;
    }

    @Override
    public void onDraw(Canvas canvas, float priority) {
        PointF start = getStartPoint();
        PointF end = getEndPoint();
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(1);
        if (isSelectedForDrag())
            cursorPaint.setColor(Color.YELLOW);
        else
            cursorPaint.setColor(Color.GREEN);
        canvas.drawLine(start.x, start.y, end.x, end.y, cursorPaint);

        if (!isSelectedForDrag())
            return;

        // draw position label
        textPaint.setColor(Color.BLACK);
        textPaint.setLinearText(true);
        textBackgroundPaint.setColor(Color.argb(100, 255, 255, 255));
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        final PointF realPosition = new PointF();
        parent.getContainerView().fromScreen(getCachedScreenPosition(), realPosition);
        PointF textPosition = new PointF(start.x, start.y);
        textPosition.offset(2, -2);
        final String positionString = String.format("Position: %d", (int)getDirection(realPosition));
        final Rect textBounds = new Rect();
        textPaint.getTextBounds(positionString, 0, positionString.length(), textBounds);
        textBounds.offset((int)textPosition.x, (int)textPosition.y);
        canvas.drawRect(textBounds, textBackgroundPaint);
        canvas.drawText(positionString, textPosition.x, textPosition.y,
                textPaint);
    }

    abstract protected float getDirection(PointF point);
    abstract protected void offsetPoint(Point point, float offset);
    abstract protected PointF getStartPoint();
    abstract protected PointF getEndPoint();
}

class HCursorMarker extends CursorMarker {
    @Override
    protected float getDirection(PointF point) {
        return point.y;
    }

    @Override
    protected void offsetPoint(Point point, float offset) {
        point.y += offset;
    }

    @Override
    protected PointF getStartPoint() {
        return new PointF(0, getCachedScreenPosition().y);
    }

    @Override
    protected PointF getEndPoint() {
        return new PointF(parent.getScreenRect().width(), getCachedScreenPosition().y);
    }
}


class VCursorMarker extends CursorMarker {
    @Override
    protected float getDirection(PointF point) {
        return point.x;
    }

    @Override
    protected void offsetPoint(Point point, float offset) {
        point.x += offset;
    }

    @Override
    protected PointF getStartPoint() {
        return new PointF((int)getCachedScreenPosition().x, parent.getScreenRect().height());
    }

    @Override
    protected PointF getEndPoint() {
        return new PointF(getCachedScreenPosition().x, 0);
    }
}


abstract public class CursorDataModelPainter extends AbstractMarkerPainter {

    public CursorDataModelPainter(MarkerDataModel data) {
        super(data);

        getMarkerPainterGroup().setSelectOnDrag(true);
    }

    @Override
    public List<IMarker> getSelectableMarkerList() {
        return markerList;
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < markerList.size(); i++) {
            IMarker marker = markerList.get(i);
            marker.onDraw(canvas, 1);
        }
    }

    @Override
    public void markerMoveRequest(DraggableMarker marker, PointF newPosition, boolean isDragging) {
        super.markerMoveRequest(marker, newPosition, isDragging);

        int selectedMarkerId = -1;
        if (marker.isSelectedForDrag())
            selectedMarkerId = markerData.getMarkerDataAt(markerList.indexOf(marker)).getId();

        if (!isDragging)
            sort();

        if (selectedMarkerId >= 0) {
            for (int i = 0; i < markerData.getMarkerCount(); i++) {
                MarkerData data = markerData.getMarkerDataAt(i);
                if (data.getId() == selectedMarkerId) {
                    markerList.get(i).setSelectedForDrag(true);
                    break;
                }
            }
        }
    }

    abstract public void sort();
}


