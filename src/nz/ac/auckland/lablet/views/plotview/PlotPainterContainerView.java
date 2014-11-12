/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.plotview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import org.luaj.vm2.ast.Str;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PlotPainterContainerView extends RangeDrawingView {
    final private List<IPlotPainter> backgroundPainters = new ArrayList();
    final private List<IPlotPainter> plotPainters = new ArrayList();
    final private List<IPlotPainter> foregroundPainters = new ArrayList();
    final protected JoinedList<IPlotPainter> allPainters = new JoinedList(backgroundPainters, plotPainters,
            foregroundPainters);

    public PlotPainterContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlotPainterContainerView(Context context) {
        super(context);
    }

    public void addBackgroundPainter(IPlotPainter painter) {
        backgroundPainters.add(painter);
        painter.setContainer(this);
    }

    public void addPlotPainter(IPlotPainter painter) {
        plotPainters.add(painter);
        painter.setContainer(this);
        if (getWidth() > 0 && getHeight() > 0)
            painter.onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    public void removePlotPainter(IPlotPainter painter) {
        plotPainters.remove(painter);
        invalidate();
    }

    public void addForegroundPainter(IPlotPainter painter) {
        foregroundPainters.add(painter);
        painter.setContainer(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (IPlotPainter painter : allPainters) {
            canvas.save();
            painter.onDraw(canvas);
            canvas.restore();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        for (IPlotPainter painter : allPainters) {
            if (painter.onTouchEvent(event))
                return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        for (IPlotPainter painter : allPainters)
            painter.onSizeChanged(w, h, oldw, oldh);
    }

    public List<IPlotPainter> getPlotPainters() {
        return plotPainters;
    }

    @Override
    public boolean setXRange(float left, float right, boolean keepDistance) {
        float oldLeft = getRangeLeft();
        float oldRight = getRangeRight();

        if (!super.setXRange(left, right, keepDistance))
            return false;

        for (IPlotPainter painter : allPainters)
            painter.onXRangeChanged(getRangeLeft(), getRangeRight(), oldLeft, oldRight);

        return true;
    }

    @Override
    public boolean setYRange(float bottom, float top, boolean keepDistance) {
        float oldBottom = getRangeBottom();
        float oldTop = getRangeTop();

        if (!super.setYRange(bottom, top, keepDistance))
            return false;

        for (IPlotPainter painter : allPainters)
            painter.onYRangeChanged(getRangeBottom(), getRangeTop(), oldBottom, oldTop);

        return true;
    }

    public List<AbstractXYDataAdapter> getXYDataAdapters() {
        List<AbstractXYDataAdapter> list = new ArrayList<>();
        for (IPlotPainter painter : allPainters) {
            if (!(painter instanceof StrategyPainter))
                continue;
            List<ConcurrentPainter> childs = ((StrategyPainter)painter).getChildPainters();
            for (ConcurrentPainter child : childs) {
                if (!(child instanceof  XYConcurrentPainter))
                    continue;
                XYConcurrentPainter xyConcurrentPainter = (XYConcurrentPainter)child;
                list.add((AbstractXYDataAdapter)xyConcurrentPainter.getAdapter());
            }
        }
        return list;
    }
}
