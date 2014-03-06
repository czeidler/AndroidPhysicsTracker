package nz.ac.aucklanduni.physics.tracker.views;

import android.graphics.*;
import android.view.View;
import nz.ac.aucklanduni.physics.tracker.Calibration;
import nz.ac.aucklanduni.physics.tracker.IExperimentRunView;
/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
import nz.ac.aucklanduni.physics.tracker.MarkersDataModel;


public class CalibrationMarkerPainter extends AbstractMarkersPainter {
    public CalibrationMarkerPainter(View parent, IExperimentRunView runView, MarkersDataModel model) {
        super(parent, runView, model);
    }

    @Override
    protected DragableMarker createMarkerForRow(int row) {
        return new CalibrationMarker(this);
    }

    private void rotate(PointF point, PointF origin, float angleScreen) {
        float x = point.x - origin.x;
        float y = point.y - origin.y;
        point.x = (float)Math.cos(Math.toRadians(angleScreen)) * x - (float)Math.sin(Math.toRadians(angleScreen)) * y;
        point.y = (float)Math.cos(Math.toRadians(angleScreen)) * y + (float)Math.sin(Math.toRadians(angleScreen)) * x;
        point.x += origin.x;
        point.y += origin.y;
    }

    @Override
    public void draw(Canvas canvas, float priority) {
        for (IMarker marker : markerList)
            marker.onDraw(canvas, priority);

        if (markerData.getMarkerCount() != 2)
            return;

        // draw scale
        PointF screenPos1 = getScreenPos(0);
        PointF screenPos2 = getScreenPos(1);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        canvas.drawLine(screenPos1.x, screenPos1.y, screenPos2.x, screenPos2.y, paint);
        // draw ends
        final float wingLength = 8;
        float angleScreen = Calibration.getAngle(screenPos1, screenPos2);
        PointF wingTop = new PointF(screenPos1.x, screenPos1.y + wingLength / 2);
        rotate(wingTop, screenPos1, angleScreen);
        PointF wingBottom = new PointF(screenPos1.x, screenPos1.y - wingLength / 2);
        rotate(wingBottom, screenPos1, angleScreen);
        canvas.drawLine(wingTop.x, wingTop.y, wingBottom.x, wingBottom.y, paint);
        wingTop = new PointF(screenPos2.x, screenPos2.y + wingLength / 2);
        rotate(wingTop, screenPos2, angleScreen);
        wingBottom = new PointF(screenPos2.x, screenPos2.y - wingLength / 2);
        rotate(wingBottom, screenPos2, angleScreen);
        canvas.drawLine(wingTop.x, wingTop.y, wingBottom.x, wingBottom.y, paint);

        if (markerData.getSelectedMarkerData() < 0)
            return;

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(15);
        int scaleLength = (int)Math.sqrt(Math.pow(screenPos1.x - screenPos2.x, 2)
                + Math.pow(screenPos1.y - screenPos2.y, 2));
        String text = "Scale length [pixel]: ";
        text += scaleLength;
        float marginPercent = 0.02f;
        PointF textPosition = new PointF(experimentRunView.getMaxRawX() * marginPercent,
                experimentRunView.getMaxRawY() - experimentRunView.getMaxRawX() * marginPercent);
        PointF screenTextPosition = new PointF();
        experimentRunView.toScreen(textPosition, screenTextPosition);

        // draw text background box
        Rect textBound = new Rect();
        textBound.left = (int)screenTextPosition.x;
        textBound.top = (int)screenTextPosition.y + (int)Math.ceil(paint.ascent());
        textBound.right = textBound.left + (int)Math.ceil(paint.measureText(text)) + 2;
        textBound.bottom = (int)screenTextPosition.y + (int)Math.ceil(paint.descent()) + 2;
        paint.setColor(Color.argb(150, 100, 100, 100));
        canvas.drawRect(textBound, paint);

        // draw text
        paint.setColor(Color.GREEN);
        canvas.drawText(text, screenTextPosition.x, screenTextPosition.y, paint);


    }

    private PointF getScreenPos(int markerIndex) {
        return markerList.get(markerIndex).getPosition();
    }
}
