/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.plotview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.*;
import nz.ac.auckland.lablet.views.plotview.axes.*;


public class PlotView extends ViewGroup {
    final static public int DEFAULT_PEN_COLOR = Color.WHITE;

    public static class PlotScale {
        public IScale scale;
        public LabelPartitioner labelPartitioner;
    }
    static public PlotScale log10Scale() {
        PlotScale plotScale = new PlotScale();
        plotScale.scale = new Log10Scale();
        plotScale.labelPartitioner = new LabelPartitionerLog10();
        return plotScale;
    }

    private TitleView titleView;
    private XAxisView xAxisView;
    private YAxisView yAxisView;
    private PlotPainterContainerView mainView;
    private BackgroundPainter backgroundPainter;
    private RangeInfoPainter rangeInfoPainter;

    private ScaleGestureDetector scaleGestureDetector;
    private DragDetector dragDetector = new DragDetector();

    private boolean xDraggable = false;
    private boolean yDraggable = false;
    private boolean xZoomable = false;
    private boolean yZoomable = false;

    // Float.MAX_VALUE means there there is no end range (negative or positive)
    private RectF maxRange = new RectF(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

    class DragDetector {
        private boolean isDragging = false;
        public PointF point = new PointF(-1, -1);

        public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() > 1) {
                setDragging(false);
                return false;
            }
            int action = event.getActionMasked();
            boolean handled = false;
            if (action == MotionEvent.ACTION_DOWN) {
                setDragging(true);
                point.set(event.getX(), event.getY());
            } else if (action == MotionEvent.ACTION_UP) {
                setDragging(false);
            } else if (action == MotionEvent.ACTION_MOVE && isDragging) {
                float xDragged = event.getX() - point.x;
                float yDragged = event.getY() - point.y;
                point.set(event.getX(), event.getY());

                handled = onDragged(xDragged, yDragged);
            }
            return handled;
        }

        private void setDragging(boolean dragging) {
            this.isDragging = dragging;
            setRangeIsChanging(dragging);
        }

        private boolean onDragged(float x, float y) {
            if (yDraggable) {
                float realDelta = mainView.fromScreenY(y) - mainView.fromScreenY(0);
                if (mainView.getRangeBottom() < mainView.getRangeTop())
                    realDelta *= -1;

                offsetYRange(realDelta);
                return true;
            }
            if (xDraggable) {
                float realDelta = mainView.fromScreenX(x) - mainView.fromScreenX(0);
                if (mainView.getRangeLeft() < mainView.getRangeRight())
                    realDelta *= -1;

                offsetXRange(realDelta);
                return true;
            }
            return false;
        }
    }

    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                if (yZoomable) {
                    setRangeIsChanging(true);

                    float focusPoint = scaleGestureDetector.getFocusY();
                    float focusPointRatio = focusPoint / mainView.getHeight();

                    float zoom = scaleGestureDetector.getPreviousSpanY() - scaleGestureDetector.getCurrentSpanY();
                    zoom /= getHeight();
                    float currentRange = Math.abs(mainView.getRangeTop() - mainView.getRangeBottom());
                    float zoomValue = zoom * currentRange;

                    float newBottom = mainView.getRangeBottom() - zoomValue * (1 - focusPointRatio);
                    float newTop = mainView.getRangeTop() + zoomValue * focusPointRatio;
                    setYRange(newBottom, newTop);

                    return true;
                }

                if (xZoomable) {
                    setRangeIsChanging(true);

                    float focusPoint = scaleGestureDetector.getFocusX();
                    float focusPointRatio = focusPoint / mainView.getWidth();

                    float zoom = scaleGestureDetector.getPreviousSpanX() - scaleGestureDetector.getCurrentSpanX();
                    zoom /= getWidth();
                    float currentRange = Math.abs(mainView.getRangeLeft() - mainView.getRangeRight());
                    float zoomValue = zoom * currentRange;

                    float newLeft = mainView.getRangeLeft() - zoomValue * focusPointRatio;
                    float newRight = mainView.getRangeRight() + zoomValue * (1 - focusPointRatio);
                    setXRange(newLeft, newRight);

                    return true;
                }
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                setRangeIsChanging(false);
            }
        });

        titleView = new TitleView(context);
        addView(titleView);

        xAxisView = new XAxisView(context);
        addView(xAxisView);

        yAxisView = new YAxisView(context);
        addView(yAxisView);

        this.mainView = new PlotPainterContainerView(context);
        addView(mainView);

        backgroundPainter = new BackgroundPainter(xAxisView, yAxisView);
        mainView.addBackgroundPainter(backgroundPainter);

        rangeInfoPainter = new RangeInfoPainter(this);
        mainView.addForegroundPainter(rangeInfoPainter);
    }

    public void addPlotPainter(IPlotPainter painter) {
        mainView.addPlotPainter(painter);
    }

    public void setMaxXRange(float left, float right) {
        maxRange.left = left;
        maxRange.right = right;

        // reset range
        setXRange(mainView.getRangeLeft(), mainView.getRangeRight());
    }

    private void setRangeIsChanging(boolean changing) {
        
    }

    public void setMaxYRange(float bottom, float top) {
        maxRange.bottom = bottom;
        maxRange.top = top;

        // reset range
        setYRange(mainView.getRangeBottom(), mainView.getRangeTop());
    }

    public RectF getMaxRange() {
        return new RectF(maxRange);
    }

    static class RangeF {
        public RangeF(float start, float end) {
            this.start = start;
            this.end = end;
        }

        public float start;
        public float end;
    }

    private void validateXRange(RangeF range) {
        if (maxRange.left != Float.MAX_VALUE) {
            if (range.end > range.start) {
                if (range.start < maxRange.left)
                    range.start = maxRange.left;
            } else {
                if (range.start > maxRange.left)
                    range.start = maxRange.left;
            }
        }
        if (maxRange.right != Float.MAX_VALUE) {
            if (range.end > range.start) {
                if (range.end > maxRange.right)
                    range.end = maxRange.right;
            } else {
                if (range.end < maxRange.right)
                    range.end = maxRange.right;
            }
        }
    }

    private void validateYRange(RangeF range) {
        if (maxRange.bottom != Float.MAX_VALUE) {
            if (range.end > range.start) {
                if (range.start < maxRange.bottom)
                    range.start = maxRange.bottom;
            } else {
                if (range.start > maxRange.bottom)
                    range.start = maxRange.bottom;
            }
        }
        if (maxRange.top != Float.MAX_VALUE) {
            if (range.end > range.start) {
                if (range.end > maxRange.top)
                    range.end = maxRange.top;
            } else {
                if (range.end < maxRange.top)
                    range.end = maxRange.top;
            }
        }
    }

    public void setYScale(PlotScale plotScale) {
        if (yAxisView != null)
            yAxisView.setLabelPartitioner(plotScale.labelPartitioner);
        for (IPlotPainter painter : mainView.getPlotPainters())
            painter.setYScale(plotScale.scale);
    }

    public BackgroundPainter getBackgroundPainter() {
        return backgroundPainter;
    }

    private boolean fuzzyEquals(float value1, float value2) {
        return Math.abs(value1 - value2) < 0.00001;
    }

    private void setYRange(float bottom, float top, boolean keepDistance) {
        RangeF range = new RangeF(bottom, top);
        validateYRange(range);
        if (keepDistance && !fuzzyEquals(bottom - top, range.start - range.end)) {
            if (bottom != range.start)
                range.end = range.start + (top - bottom);
            else if (top != range.end)
                range.start = range.end - (top - bottom);
        }
        bottom = range.start;
        top = range.end;

        if (hasYAxis())
            yAxisView.setDataRange(bottom, top);
        mainView.setRangeY(bottom, top);

        invalidate();
    }

    public void setXRange(float left, float right, boolean keepDistance) {
        RangeF range = new RangeF(left, right);
        validateXRange(range);
        if (keepDistance && !fuzzyEquals(left - right, range.start - range.end)) {
            if (left != range.start)
                range.end = range.start + (right - left);
            else if (right != range.end)
                range.start = range.end - (right - left);
        }
        left = range.start;
        right = range.end;

        if (hasXAxis())
            xAxisView.setDataRange(left, right);
        mainView.setRangeX(left, right);

        invalidate();
    }

    public void setXRange(float left, float right) {
        setXRange(left, right, false);
    }

    public void setYRange(float bottom, float top) {
        setYRange(bottom, top, false);
    }

    public void offsetXRange(float offset) {
        setXRange(mainView.getRangeLeft() + offset, mainView.getRangeRight() + offset, true);
    }

    public void offsetYRange(float offset) {
        setYRange(mainView.getRangeBottom() + offset, mainView.getRangeTop() + offset, true);
    }

    public boolean isXDragable() {
        return xDraggable;
    }

    public void setXDragable(boolean xDragable) {
        this.xDraggable = xDragable;
    }

    public boolean isYDragable() {
        return yDraggable;
    }

    public void setYDragable(boolean yDragable) {
        this.yDraggable = yDragable;
    }

    public boolean isXZoomable() {
        return xZoomable;
    }

    public void setXZoomable(boolean xZoomable) {
        this.xZoomable = xZoomable;
    }

    public boolean isYZoomable() {
        return yZoomable;
    }

    public void setYZoomable(boolean yZoomable) {
        this.yZoomable = yZoomable;
    }

    public void invalidate() {
        mainView.invalidate();
        if (xAxisView != null)
            xAxisView.invalidate();
        if (yAxisView != null)
            yAxisView.invalidate();
    }

    public TitleView getTitleView() {
        return titleView;
    }

    public YAxisView getYAxisView() {
        return yAxisView;
    }

    public XAxisView getXAxisView() {
        return xAxisView;
    }

    private boolean hasTitle() {
        return titleView != null && titleView.getVisibility() == View.VISIBLE;
    }

    private boolean hasXAxis() {
        return xAxisView != null && xAxisView.getVisibility() == View.VISIBLE;
    }

    private boolean hasYAxis() {
        return yAxisView != null && yAxisView.getVisibility() == View.VISIBLE;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = right - left;
        final int height = bottom - top;
        int titleBottom = 0;
        if (hasTitle())
            titleBottom = (int)titleView.getPreferredHeight();
        final int titleHeight = titleBottom;

        int xAxisTop = height;
        int xAxisLeftOffset = 0;
        int xAxisRightOffset = 0;
        if (hasXAxis()) {
            xAxisTop -= (int)xAxisView.optimalHeight();
            xAxisRightOffset = (int)xAxisView.getAxisRightOffset();
            xAxisLeftOffset = (int)xAxisView.getAxisLeftOffset();
        }

        int yAxisRight = 0;
        int yAxisTopOffset = 0;
        int yAxisBottomOffset = 0;
        if (hasYAxis()) {
            final int mainAreaHeight = xAxisTop - (int)Math.max(yAxisView.getAxisTopOffset(), titleHeight);
            yAxisRight = (int) yAxisView.optimalWidthForHeight(mainAreaHeight);
            yAxisTopOffset = (int) yAxisView.getAxisTopOffset();
            yAxisBottomOffset = (int)yAxisView.getAxisBottomOffset();
        }

        final Rect titleRect = new Rect(yAxisRight, 0, width, titleBottom);
        final Rect xAxisRect = new Rect(yAxisRight - xAxisLeftOffset, xAxisTop, width, height);
        final Rect yAxisRect = new Rect(0, Math.max(0, titleBottom - yAxisTopOffset), yAxisRight,
                xAxisTop + yAxisBottomOffset);
        final Rect mainViewRect = new Rect(yAxisRight, Math.max(titleBottom, yAxisTopOffset), width - xAxisRightOffset,
                xAxisTop);

        if (hasTitle()) {
            titleView.measure(MeasureSpec.makeMeasureSpec(titleRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(titleRect.height(), MeasureSpec.EXACTLY));
            titleView.layout(titleRect.left, titleRect.top, titleRect.right, titleRect.bottom);
        }

        if (hasXAxis()) {
            xAxisView.measure(MeasureSpec.makeMeasureSpec(xAxisRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(xAxisRect.height(), MeasureSpec.EXACTLY));
            xAxisView.layout(xAxisRect.left, xAxisRect.top, xAxisRect.right, xAxisRect.bottom);
        }

        if (hasYAxis()) {
            yAxisView.measure(MeasureSpec.makeMeasureSpec(yAxisRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(yAxisRect.height(), MeasureSpec.EXACTLY));
            yAxisView.layout(yAxisRect.left, yAxisRect.top, yAxisRect.right, yAxisRect.bottom);
        }

        if (mainView != null) {
            mainView.measure(MeasureSpec.makeMeasureSpec(mainViewRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mainViewRect.height(), MeasureSpec.EXACTLY));
            mainView.layout(mainViewRect.left, mainViewRect.top, mainViewRect.right, mainViewRect.bottom);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        dragDetector.onTouchEvent(event);

        if (scaleGestureDetector.onTouchEvent(event))
            return true;

        return super.onTouchEvent(event);
    }
}
