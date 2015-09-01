package nz.ac.auckland.lablet.views.markers;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

import nz.ac.auckland.lablet.data.Data;
import nz.ac.auckland.lablet.data.DataList;
import nz.ac.auckland.lablet.data.RoiData;
import nz.ac.auckland.lablet.misc.DeviceIndependentPixel;
import nz.ac.auckland.lablet.views.plotview.AbstractPlotPainter;

/**
 * Created by Jamie on 26/08/2015.
 */
public abstract class MarkerList<D extends DataList> extends AbstractPlotPainter
{
    private DataList.IListener<D> dataListener = new DataList.IListener<D>() {

        @Override
        public void onDataAdded(D list, int index) {
            addMarker(index);
            containerView.invalidate();
        }

        @Override
        public void onDataRemoved(D list, int index, Data data) {
            removeMarker(index);
            containerView.invalidate();
        }

        @Override
        public void onDataChanged(D list, int index, int number) {
            rebuildPainterList(); //TODO: bug, if I don't call this then view doesn't update when setMarkerPosition called.
            containerView.invalidate();
        }

        @Override
        public void onAllDataChanged(D list) {
            rebuildPainterList();
            containerView.invalidate();
        }

        @Override
        public void onDataSelected(D list, int index) {
            containerView.invalidate();
        }

    };

    protected D dataList = null;
    final protected Rect frame = new Rect();
    final protected List<IMarker> painterList = new ArrayList<>();

    public MarkerList(D list) {
        dataList = list;
        dataList.addListener(dataListener);
    }

    @Override
    protected void onAttachedToView() {
        super.onAttachedToView();

        rebuildPainterList();
    }

    public D getDataList() {
        return dataList;
    }

    public void release() {
        if (dataList != null) {
            dataList.removeListener(dataListener);
            dataList = null;
        }
    }

    public void setCurrentFrame(int frame, @Nullable PointF insertHint) {

        // check if we have the run in the data list
        Data data = null;
        int index = dataList.getIndexByFrameId(frame);
        dataList.selectData(index);
    }

    public List<IMarker> getSelectableMarkerList() {
        return painterList;
    }

    public RectF getScreenRect() {
        return containerView.getScreenRect();
    }

    @Override
    public void onSizeChanged(int width, int height, int oldw, int oldh) {
        frame.set(0, 0, width, height);
        rebuildPainterList();
    }

    protected void rebuildPainterList() {
        painterList.clear();
        for (int i = 0; i < dataList.getDataCount(); i++)
            addMarker(i);
    }

    @Override
    public void invalidate() {

    }

    public IMarker getMarkerForFrame(int frameId) {
        if (frameId < 0 || frameId >= painterList.size())
            return null;
        return painterList.get(frameId);
    }



    abstract protected IMarker createMarkerForFrame(int frameId);

    public void addMarker(int frameId) {
        IMarker marker = createMarkerForFrame(frameId);
        marker.setTo(this, dataList.getDataAt(frameId));
        painterList.add(frameId, marker);
    }

    public int markerIndexOf(IMarker marker) {
        return painterList.indexOf(marker);
    }

    public void removeMarker(int frameId) {
        painterList.remove(frameId);
        if (frameId == dataList.getSelectedData())
            dataList.selectData(-1);
    }

    @Override
    public void onRangeChanged(RectF range, RectF oldRange, boolean keepDistance) {
        super.onRangeChanged(range, oldRange, keepDistance);

        invalidateMarker();
    }

    private void invalidateMarker() {
        for (IMarker marker : painterList)
            marker.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        List<IMarker> selectableMarkers = getSelectableMarkerList();
        int action = event.getActionMasked();
        boolean handled = false;
        if (action == MotionEvent.ACTION_DOWN) {
            for (IMarker marker : selectableMarkers) {
                if (marker.handleActionDown(event)) {
                    handled = true;
                    break;
                }
            }
            if (handled) {
                ViewParent parent = containerView.getParent();
                if (parent != null)
                    parent.requestDisallowInterceptTouchEvent(true);
            }

        } else if (action == MotionEvent.ACTION_UP) {
            for (IMarker marker : selectableMarkers) {
                if (marker.handleActionUp(event)) {
                    handled = true;
                    break;
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (IMarker marker : selectableMarkers) {
                if (marker.handleActionMove(event)) {
                    handled = true;
                    break;
                }
            }
        }
        if (handled)
            containerView.invalidate();

        return handled;
    }

}
