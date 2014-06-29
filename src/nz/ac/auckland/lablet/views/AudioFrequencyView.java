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
import android.view.ViewGroup;


public class AudioFrequencyView extends ViewGroup {
    final private float BORDER = 0.0f;
    final private int BACKGROUND_COLOR = Color.argb(255, 80, 80, 80);
    final private Paint penPaint = new Paint();

    private Rect viewRect = new Rect();

    private Bitmap bitmap = null;
    private Canvas bitmapCanvas = null;

    private int position = 0;
    private float valueMax = 200;

    private double[] frequencies = null;

    public AudioFrequencyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWillNotDraw(false);

        penPaint.setColor(Color.GREEN);
        penPaint.setStrokeWidth(1);
        penPaint.setStyle(Paint.Style.FILL);
    }

    public void addData(double[] frequencies) {
        this.frequencies = frequencies;
        invalidate();
    }

    private float toScreenY(float y) {
        return getAmpBaseLine() - (( viewRect.height() * (1.f - BORDER)) * y / valueMax);
    }

    private void drawFrequencies(int position, float frequency) {
        float binWidth = 2.f;
        float binPostition = binWidth * position;
        bitmapCanvas.drawRect(binPostition, toScreenY(frequency), binPostition + binWidth, toScreenY(0), penPaint);
    }

    private float getAmpBaseLine() {
        return  viewRect.height();
    }

    private void clearBitmap() {
        bitmap.eraseColor(BACKGROUND_COLOR);
    }

    @Override
    protected void onLayout(boolean b, int i, int i2, int i3, int i4) {

    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (w <= 0 || h <= 0)
            return;

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);

        viewRect.set(0, 0, w, h);

        clearBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (frequencies != null && frequencies.length > 0) {
            clearBitmap();
            position = 0;

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < frequencies.length; i++) {
                float frequencyValue = (float) Math.abs(frequencies[i]) / frequencies.length;

                min = Math.min(frequencyValue, min);
                max = Math.max(frequencyValue, max);

                drawFrequencies(position, frequencyValue);
                position++;
            }

            frequencies = null;
        }

        if (bitmap != null)
            canvas.drawBitmap(bitmap, 0, 0, null);
    }
}
