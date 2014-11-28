/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import nz.ac.auckland.lablet.views.plotview.*;

import java.util.Arrays;


public class AudioFrequencyMapConcurrentPainter extends ArrayConcurrentPainter {
    final private int[] heatMap = new int[512];

    public AudioFrequencyMapConcurrentPainter(CloneablePlotDataAdapter dataAdapter) {
        super(dataAdapter);

        setMaxDirtyRanges(-1);

        preCalculateHeatMap();
    }

    private void preCalculateHeatMap() {
        final int[] colors = {
                Color.rgb(191, 191, 191), // light gray
                Color.rgb(77, 153, 255), // light blue
                Color.rgb(230, 26, 230), // violet
                Color.RED,
                Color.BLACK,
                Color.GREEN
        };

        // last color is for overflow
        final int nColors = colors.length - 1;
        for (int i = 0; i < heatMap.length - 1; i++) {
            final float value = ((float)i) / (heatMap.length - 1);
            final int index = (int)(value * (nColors - 1)) + 1;

            int red = (int)((1.f - value) * Color.red(colors[index - 1]) + value * Color.red(colors[index]));
            int green = (int)((1.f - value) * Color.green(colors[index - 1]) + value * Color.green(colors[index]));
            int blue = (int)((1.f - value) * Color.blue(colors[index - 1]) + value * Color.blue(colors[index]));

            heatMap[i] = Color.rgb(red, green, blue);
        }

        heatMap[heatMap.length - 1] = colors[nColors];
    }

    private int heatMap(double value) {
        if (value >= 1)
            return heatMap[heatMap.length - 1];
        if (value < 0)
            return heatMap[0];

        return heatMap[(int)(value * heatMap.length)];
    }

    @Override
    protected RectF getRealDataRect(int startIndex, int lastIndex) {
        AudioFrequencyMapAdapter audioAmplitudePlotDataAdapter = (AudioFrequencyMapAdapter)dataAdapter;
        RectF realDataRect = parent.getContainerView().getRange();
        if (audioAmplitudePlotDataAdapter.getSize() > 0) {
            realDataRect.left = audioAmplitudePlotDataAdapter.getX(startIndex);
            realDataRect.right = audioAmplitudePlotDataAdapter.getX(lastIndex);
        }
        return realDataRect;
    }

    @Override
    protected boolean geometryInfoNeededForRendering() {
        return true;
    }

    @Override
    protected void drawRange(Canvas bitmapCanvas, ArrayRenderPayload payload, Range range) {
        AudioFrequencyMapAdapter adapter = (AudioFrequencyMapAdapter)payload.getAdapter();
        Matrix rangeMatrix = payload.getRangeMatrix();

        int start = range.min;
        int count = range.max - range.min + 1;
        if (start < 0)
            start = 0;

        final int dataSize = adapter.getSize();
        if (dataSize == 0)
            return;
        if (start + count > dataSize)
            count = dataSize - start;

        final float[] startLeftTop = mapPoint(rangeMatrix, payload.getRealDataRect().left, payload.getRealDataRect().top);
        final int xStartPixel = (int)startLeftTop[0];

        final RectF screenRect = payload.getScreenRect();
        final int screenRectWidth = (int)Math.ceil(screenRect.width());
        final int screenRectHeight = (int)Math.ceil(screenRect.height());
        final int[] colors = new int[screenRectHeight];
        final int[] bitmapData = new int[screenRectWidth * screenRectHeight];
        // the default int value of 0 is transparent
        //Arrays.fill(bitmapData, Color.TRANSPARENT);

        for (int index = start; index < start + count; index++) {

            final float[] screenLeftTop = mapPoint(rangeMatrix, adapter.getX(index), payload.getRealDataRect().top);
            int timePixel = (int)screenLeftTop[0] - xStartPixel;

            if (timePixel < 0)
                continue;

            final int lastTimePixel = timePixel;

            final float[] frequencies = adapter.getY(index);
            getColors(colors, frequencies, payload);

            // advance till the next pixel
            for (; index < start + count; index++) {
                final float[] temp = mapPoint(rangeMatrix, adapter.getX(index), payload.getRealDataRect().top);
                timePixel = (int)temp[0] - xStartPixel;
                if (lastTimePixel != timePixel) {
                    // go back again
                    timePixel --;
                    index --;
                    break;
                }
            }

            for (int column = lastTimePixel; column <= timePixel; column++) {
                if (column >= screenRect.width())
                    break;
                for (int row = 0; row < colors.length; row++)
                    bitmapData[column + row * screenRectWidth] = colors[row];
            }

            if (timePixel >= screenRect.width())
                break;
        }
        bitmapCanvas.drawBitmap(bitmapData, 0, screenRectWidth, startLeftTop[0], startLeftTop[1],
                screenRectWidth, screenRectHeight, true, null);
    }

    private float[] mapPoint(Matrix matrix, float x, float y) {
        final float[] screenLeftTop = new float[2];
        screenLeftTop[0] = x;
        screenLeftTop[1] = y;
        matrix.mapPoints(screenLeftTop);
        return screenLeftTop;
    }

    final float frequencyRang = 22050;
    private float getRealValue(int index, int arraySize) {
        return (float)index / arraySize * frequencyRang;
    }

    private double getFrequencyAmp(float frequencyAmpRaw, float frequencyAmpMax) {
        final float maxDB = -60;
        return 1d - 10d * Math.log10(frequencyAmpRaw / frequencyAmpMax) / maxDB;

        //return Math.log10(Math.abs(frequencyAmpRaw)) / Math.log10(frequencyAmpMax);
        //return Math.abs(frequencyAmpRaw) / frequencyAmpMax;
    }


    private int toPixel(float scaledValue, float scaledBottom, float scaledTop, int screenRectHeight) {
        return (int)((scaledValue - scaledBottom) / (scaledTop - scaledBottom) * screenRectHeight);
    }

    private int[] getColors(int[] colors, final float[] frequencies, final ArrayRenderPayload payload) {
        IScale yScale = parent.getYScale();

        final float scaledBottom = yScale.scale(payload.getRealDataRect().bottom);
        final float scaledTop = yScale.scale(payload.getRealDataRect().top);
        final int screenRectHeight = (int)Math.ceil(payload.getScreenRect().height());

        float maxFreqAmplitude = 32768 * frequencies.length * 2;

        Arrays.fill(colors, Color.TRANSPARENT);

        float frequencyAmpSum = 0;
        int lastPixel = -1;
        int perPixelCount = 0;
        for (int i = 0; i < frequencies.length; i++) {
            float frequencyAmp = frequencies[i];

            float frequency = getRealValue(i, frequencies.length);
            int pixel = toPixel(yScale.scale(frequency), scaledBottom, scaledTop, screenRectHeight);
            if (pixel < 0)
                continue;
            if (pixel >= colors.length)
                break;
            if (lastPixel == -1)
                lastPixel = pixel;

            if (pixel == lastPixel) {
                frequencyAmpSum += frequencyAmp;
                perPixelCount++;
            } else {
                float frequencyAmpAverage = frequencyAmpSum / perPixelCount;
                double freqAmplitude = getFrequencyAmp(frequencyAmpAverage, maxFreqAmplitude);
                drawFrequencyPixels(colors, freqAmplitude, lastPixel, pixel);

                frequencyAmpSum = frequencyAmp;
                lastPixel = pixel;
                perPixelCount = 1;
            }
        }
        if (lastPixel >= 0) {
            float frequencyAmpAverage = frequencyAmpSum / perPixelCount;
            double freqAmplitude = getFrequencyAmp(frequencyAmpAverage, maxFreqAmplitude);
            drawFrequencyPixels(colors, freqAmplitude, lastPixel, colors.length - 1);
        }
        return colors;
    }

    private void drawFrequencyPixels(int[] colors, double frequencyAmp, int startPixel, int endPixel) {
        int colorValue = heatMap(frequencyAmp);
        for (int a = startPixel; a < endPixel; a++) {
            int colorIndex = colors.length - 1 - a;
            colors[colorIndex] = colorValue;
        }
    }
}
