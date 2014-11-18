/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.plotview;

import android.graphics.*;

import java.util.ArrayList;
import java.util.List;


public class BufferedStrategyPainter extends StrategyPainter {
    private Bitmap offScreenBitmap;
    private Canvas offScreenCanvas;

    private RectF dirtyRect = null;
    private boolean invalidated = false;
    private Point moveBitmap = new Point();

    @Override
    public boolean hasThreads() {
        return false;
    }

    @Override
    public boolean hasFreeRenderingPipe() {
        return true;
    }

    @Override
    public void onSizeChanged(int width, int height, int oldw, int oldh) {
        offScreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        offScreenCanvas = new Canvas(offScreenBitmap);
    }

    private List<RenderPayload> collectAllRenderPayloads(boolean geometryInfoNeeded, RectF requestedRealRect) {
        List<RenderPayload> payloadList = new ArrayList<>();
        for (ConcurrentPainter painter : childPainters)
            payloadList.addAll(painter.collectRenderPayloads(geometryInfoNeeded, requestedRealRect));

        return payloadList;
    }

    private boolean isCompleteRedraw(List<RenderPayload> payloadList) {
        for (RenderPayload payload : payloadList) {
            if (payload.isCompleteRedraw())
                return true;
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (offScreenBitmap == null)
            return;

        RectF range = null;
        if (dirtyRect != null)
            range = dirtyRect;
        dirtyRect = null;

        List<RenderPayload> payloadList = collectAllRenderPayloads(false, range);
        if (isCompleteRedraw(payloadList) || invalidated) {
            offScreenBitmap.eraseColor(Color.TRANSPARENT);
            invalidated = false;
        }

        if ((moveBitmap.x != 0 && Math.abs(moveBitmap.x) < containerView.getWidth()
                || (moveBitmap.y != 0 && Math.abs(moveBitmap.y) < containerView.getHeight()))) {
            Bitmap newBitmap = Bitmap.createBitmap(offScreenBitmap.getWidth(), offScreenBitmap.getHeight(),
                    offScreenBitmap.getConfig());
            Canvas newCanvas = new Canvas(newBitmap);
            newCanvas.drawBitmap(offScreenBitmap, moveBitmap.x, moveBitmap.y, null);
            offScreenBitmap = newBitmap;
            offScreenCanvas = newCanvas;
            moveBitmap.set(0, 0);
        }

        for (RenderPayload payload : payloadList) {
            ConcurrentPainter painter = payload.getPainter();
            painter.render(offScreenCanvas, payload);
        }

        canvas.drawBitmap(offScreenBitmap, 0, 0, null);
    }

    @Override
    protected void onNewDirtyRegions() {
        containerView.invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();

        dirtyRect = new RectF(containerView.getRange());
        invalidated = true;
    }

    private RectF getDirtyRect(RectF rangeOrg, RectF oldRangeOrg, boolean keepDistance) {
        NormRectF dirt = new NormRectF(new RectF(rangeOrg));
        if (!keepDistance)
            return dirt.get();

        NormRectF range = new NormRectF(rangeOrg);
        NormRectF oldRange = new NormRectF(oldRangeOrg);
        if (range.getTop() == oldRange.getTop() && range.getBottom() == oldRange.getBottom()) {
            if (oldRange.getLeft() > range.getLeft() && oldRange.getLeft() < range.getRight())
                dirt.setRight(oldRange.getLeft());
            else if (oldRange.getRight() > range.getLeft() && oldRange.getRight() < range.getRight())
                dirt.setLeft(oldRange.getRight());
        }

        if (range.getLeft() == oldRange.getLeft() && range.getRight() == oldRange.getRight()) {
            if (oldRange.getTop() > range.getTop() && oldRange.getTop() < range.getBottom())
                dirt.setBottom(oldRange.getTop());
            else if (oldRange.getBottom() > range.getTop() && oldRange.getBottom() < range.getBottom())
                dirt.setTop(oldRange.getBottom());
        }

        return dirt.get();
    }

    @Override
    public void onRangeChanged(RectF range, RectF oldRange, boolean keepDistance) {
        if (keepDistance) {
            dirtyRect = getDirtyRect(range, oldRange, keepDistance);

            Rect oldScreen = containerView.toScreen(oldRange);
            Rect screen = containerView.toScreen(range);
            moveBitmap.offset(oldScreen.left - screen.left, oldScreen.top - screen.top);
            containerView.invalidate();
        } else
            invalidate();
    }
}