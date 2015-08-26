/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

import org.opencv.core.RotatedRect;

import nz.ac.auckland.lablet.R;
//import nz.ac.auckland.lablet.accelerometer.AccelerometerSensorData;
import nz.ac.auckland.lablet.data.Data;
import nz.ac.auckland.lablet.data.FrameDataList;
import nz.ac.auckland.lablet.data.PointData;
import nz.ac.auckland.lablet.data.PointDataList;
import nz.ac.auckland.lablet.data.RectData;
import nz.ac.auckland.lablet.misc.Unit;
import nz.ac.auckland.lablet.misc.WeakListenable;
import nz.ac.auckland.lablet.views.FrameDataSeekBar;
import nz.ac.auckland.lablet.views.painters.RoiDataListPainter;
import nz.ac.auckland.lablet.views.graph.*;
import nz.ac.auckland.lablet.views.plotview.LinearFitPainter;
import nz.ac.auckland.lablet.views.table.*;
import nz.ac.auckland.lablet.vision_algorithms.CamShiftTracker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class MotionAnalysisSideBar extends WeakListenable<MotionAnalysisSideBar.IListener> {
    public interface IListener {
        void onOpened();
        void onClosed();
    }

    final private FrameLayout sideBar;
    private boolean open = false;
    private AnimatorSet animator;
    final private View parent;
    final int animationDuration = 300;

    public MotionAnalysisSideBar(View parent, View content) {
        this.parent = parent;
        sideBar = (FrameLayout)parent.findViewById(R.id.sideBarFrame);

        sideBar.addView(content);
    }

    public float getWidth() {
        return sideBar.getWidth();
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        if (open)
            return;
        open = true;
        notifyOnOpened();

        if (animator != null)
            animator.cancel();

        animator = new AnimatorSet();
        int width = parent.getWidth();
        animator.play(ObjectAnimator.ofFloat(sideBar, View.X, width, width - sideBar.getWidth()));
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                sideBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }
        });
        animator.start();
    }

    public void close() {
        if (!open)
            return;
        open = false;
        notifyOnClosed();

        if (animator != null)
            animator.cancel();

        animator = new AnimatorSet();
        int width = parent.getWidth();
        animator.play(ObjectAnimator.ofFloat(sideBar, View.X, width - sideBar.getWidth(), width));
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
                sideBar.setVisibility(View.INVISIBLE);
            }
        });
        animator.start();
    }

    private void notifyOnOpened() {
        for (IListener listener : getListeners())
            listener.onOpened();
    }

    private void notifyOnClosed() {
        for (IListener listener : getListeners())
            listener.onClosed();
    }
}

class MotionAnalysisFragmentView extends FrameLayout {
    private MarkerDataTableAdapter markerDataTableAdapter;
    final private FrameContainerView runContainerView;
    final private GraphView2D graphView;
    final private Spinner graphSpinner;
    final private ViewGroup sideBarView;
    final private TableView tableView;
    final private MotionAnalysisSideBar sideBar;
    final private FrameDataSeekBar frameDataSeekBar;
    final private List<GraphSpinnerEntry> graphSpinnerEntryList = new ArrayList<>();
    private boolean releaseAdaptersWhenDrawerClosed = false;
    final private FrameDataList.IListener dataListenerStrongRef;
    //final private PointDataList.IListener markerListenerStrongRef;
    private boolean changingFrames = false;
    int curMarker;
    private PointF nextPos;
    private CamShiftTracker tracker;// = new CamShiftTracker();
    private int previousFrame = -1;

    private class GraphSpinnerEntry {
        private String name;
        private MarkerTimeGraphAdapter markerGraphAdapter;
        private boolean fit = false;

        public GraphSpinnerEntry(String name, MarkerTimeGraphAdapter adapter) {
            this.name = name;
            this.markerGraphAdapter = adapter;
        }

        public GraphSpinnerEntry(String name, MarkerTimeGraphAdapter adapter, boolean fit) {
            this.name = name;
            this.markerGraphAdapter = adapter;
            this.fit = fit;
        }

        public void release() {
            markerGraphAdapter.release();
        }

        public String toString() {
            return name;
        }

        public MarkerTimeGraphAdapter getMarkerGraphAdapter() {
            return markerGraphAdapter;
        }

        public boolean getFit() {
            return fit;
        }
    }

    private SideBarState sideBarState = new ClosedSideBarState();

    private void setSideBarState(SideBarState state) {
        if (sideBarState != null)
            sideBarState.leaveState();
        sideBarState = state;
        if (sideBarState != null)
            sideBarState.enterState();
    }

    abstract class SideBarState {
        abstract void nextState();
        void enterState() {}
        void leaveState() {}
        abstract int getIcon();
    }

    class ClosedSideBarState extends SideBarState {
        @Override
        void nextState() {
            setSideBarState(new HalfOpenedSideBarState());
        }

        @Override
        int getIcon() {
            return R.drawable.ic_chart_line;
        }
    }

    class HalfOpenedSideBarState extends SideBarState {
        @Override
        void nextState() {
            setSideBarState(new OpenedSideBarState());
        }

        @Override
        void enterState() {
            sideBarView.setAlpha(0.4f);
            graphView.setClickable(false);

            tableView.setVisibility(GONE);
            ViewGroup.LayoutParams params = sideBarView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            sideBarView.setLayoutParams(params);
            sideBar.open();
        }

        @Override
        void leaveState() {
            graphView.setClickable(true);
            sideBarView.setAlpha(0.8f);

            ViewGroup.LayoutParams params = sideBarView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            sideBarView.setLayoutParams(params);
            tableView.setVisibility(VISIBLE);
        }

        @Override
        int getIcon() {
            return R.drawable.ic_chart_line_plus;
        }
    }

    class OpenedSideBarState extends SideBarState {
        @Override
        void nextState() {
            setSideBarState(new ClosedSideBarState());
        }

        @Override
        void leaveState() {
            sideBar.close();
        }

        @Override
        int getIcon() {
            return R.drawable.ic_chart_line_less;
        }
    }

    // keep hard reference to the listener!
    private MotionAnalysisSideBar.IListener sideBarListener = new MotionAnalysisSideBar.IListener() {
        @Override
        public void onOpened() {
            if (releaseAdaptersWhenDrawerClosed) {
                selectGraphAdapter(graphSpinner.getSelectedItemPosition());
                tableView.setAdapter(markerDataTableAdapter);
            }
        }

        @Override
        public void onClosed() {
            if (releaseAdaptersWhenDrawerClosed) {
                tableView.setAdapter((ITableAdapter) null);
                selectGraphAdapter(-1);
            }
        }
    };

    public MotionAnalysisFragmentView(Context context, final MotionAnalysis sensorAnalysis) {
        super(context);

        final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mainView = inflater.inflate(R.layout.motion_analysis, this, true);
        assert mainView != null;

        frameDataSeekBar = (FrameDataSeekBar)mainView.findViewById(R.id.frameDataSeekBar);

        sideBarView = (ViewGroup)inflater.inflate(R.layout.motion_analysis_data_side_bar, null, false);
        sideBar = new MotionAnalysisSideBar(mainView, sideBarView);

        runContainerView = (FrameContainerView)mainView.findViewById(R.id.frameContainerView);

        graphSpinner = (Spinner)sideBarView.findViewById(R.id.graphSpinner);

        // marker graph view
        graphView = (GraphView2D)sideBarView.findViewById(R.id.tagMarkerGraphView);
        assert graphView != null;

        tableView = (TableView)sideBarView.findViewById(R.id.tableView);
        assert tableView != null;
        tableView.setColumnWeights(1f, 1.8f, 1.4f, 1.4f);
        tableView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    return;
                // ignore header row
                int frameId = sensorAnalysis.getTagMarkers().getDataAt(i - 1).getFrameId();
                sensorAnalysis.getFrameDataList().setCurrentFrame(frameId);
            }
        });

        final CameraExperimentFrameView sensorAnalysisView = new CameraExperimentFrameView(context, sensorAnalysis);
        frameDataSeekBar.setTo(sensorAnalysis.getFrameDataList(), sensorAnalysis.getTimeData());
        tracker = new CamShiftTracker();

        dataListenerStrongRef = new FrameDataList.IListener() {

            @Override
            public void onFrameChanged(int newFrame) {

                if(tracker.isROISet() && sensorAnalysis.getFrameDataList().getROIFrame() == newFrame) {
                    sensorAnalysis.getRectMarkers().setVisibility(true);
                }
                else if(tracker.isROISet() && sensorAnalysis.getFrameDataList().getROIFrame() != newFrame)
                {
                    sensorAnalysis.getRectMarkers().setVisibility(false);
                }

                if(tracker.isROISet() && !sensorAnalysis.getFrameDataList().isObjectPicked(newFrame))
                {
                    long startFrameTime = (long)sensorAnalysis.getTimeData().getTimeAt(newFrame-1) * 1000;
                    long endFrameTime = (long)sensorAnalysis.getTimeData().getTimeAt(newFrame) * 1000;


                    RotatedRect result = new RotatedRect();
                    int frameRate = sensorAnalysis.getVideoData().getVideoFrameRate();
                    long increment = 1000*1000/frameRate;

                    for(long i = startFrameTime; i <= endFrameTime; i+=increment) {
                        Bitmap bmp = sensorAnalysis.getVideoData().getFrame(i);
                        tracker.findObject(newFrame, bmp);
                        result = tracker.getOutput(newFrame);
                    }
                    //sensorAnalysis.getFrameDataList().g
                    sensorAnalysis.getFrameDataList().setObjectPicked(newFrame, true);

                    //Visualise detected rectangle (debug mode)


//                    int currentMarker = sensorAnalysis.getTagMarkers().getSelectedData();
//
//                    if(curPos != null) {
//                        sensorAnalysis.getTagMarkers().setMarkerPosition(curPos, currentMarker);
//                    }
                    PointF nextPos = sensorAnalysis.getVideoData().toMarkerPoint(new Point((int) result.center.x, (int) result.center.y));
                    PointData nextTag = new PointData(newFrame);
                    nextTag.setPosition(nextPos);
                    sensorAnalysis.getTagMarkers().addData(nextTag); //Have to add marker here rather than edit its position as TagDataListPainter.setCurrentFrame (which adds markers) is called after this method

                    //Add debugging data
                    RectData data = new RectData(newFrame);
                    data.setCentre(sensorAnalysis.getVideoData().toMarkerPoint(new Point((int)result.center.x, (int)result.center.y)));
                    data.setAngle((float)result.angle);
                    data.setHeight((float)result.size.height); //TODO: scale
                    data.setHeight((float)result.size.width);
                    sensorAnalysis.getRectDataList().addData(data);
                    //

                            //.setMarkerPosition(nextPos, -1);
                    //changingFrames = true;

                    //changingFrames = true;
                    //sensorAnalysis.getTagMarkers().setMarkerPosition(newPos, currentMarker);
                    //PointDataList model = sensorAnalysis.getTagMarkers().addListener();

                    //sensorAnalysis.addListener();
                    int i = 0;
                    //Set marker position in new window. TODO: hacky for now as doesn't take rotation into account
                    //Data topLeft = sensorAnalysis.getRectMarkers() .getRectMarkers().getDataAt(1);
                    //Data btmRight = sensorAnalysis.getRectMarkers().getDataAt(0);

                    //int width = (int)result.size.width;
                    //int height = (int)result.size.height;
                    //int x = (int)result.center.x - width/2;
                    //int y = (int)result.center.y - height/2;

                    //topLeft.setPosition();
                    //btmRight.setPosition(sensorAnalysis.getVideoData().toMarkerPoint(new Point(result.width, result.height)));
                }
            }

            @Override
            public void onNumberOfFramesChanged() {

            }
        };

        sensorAnalysis.getFrameDataList().addListener(dataListenerStrongRef);


//        markerListenerStrongRef = new PointDataList.IListener() {
//
//            @Override
//            public void onDataAdded(PointDataList model, int index) {
//
//                if(changingFrames && nextPos != null)
//                {
//                    //sensorAnalysis.getTagMarkers().setMarkerPosition(nextPos, -1);
//                    //changingFrames = false;
//                }
//            }
//
//            @Override
//            public void onDataRemoved(PointDataList model, int index, Data data) {
//                int i = 0;
//                int j = 1;
//            }
//
//            @Override
//            public void onDataChanged(PointDataList model, int index, int number) {
//                int i = 0;
//                int j = 1;
//            }
//
//            @Override
//            public void onAllDataChanged(PointDataList model) {
//                int i = 0;
//                int j = 1;
//            }
//
//            @Override
//            public void onDataSelected(PointDataList model, int index) {
//                int i = 0;
//                int j = 1;
//            }
//        };
//
//        sensorAnalysis.getTagMarkers().addListener(markerListenerStrongRef);

        runContainerView.setTo(sensorAnalysisView, frameDataSeekBar, sensorAnalysis);

        final Unit xUnit = sensorAnalysis.getXUnit();
        final Unit yUnit = sensorAnalysis.getYUnit();
        final Unit tUnit = sensorAnalysis.getTUnit();

        // marker table view
        final ITimeData timeData = sensorAnalysis.getTimeData();
        markerDataTableAdapter = new MarkerDataTableAdapter(sensorAnalysis.getTagMarkers());
        markerDataTableAdapter.addColumn(new RunIdDataTableColumn("frame"));
        markerDataTableAdapter.addColumn(new TimeDataTableColumn(tUnit, timeData));
        markerDataTableAdapter.addColumn(new XPositionDataTableColumn(xUnit));
        markerDataTableAdapter.addColumn(new YPositionDataTableColumn(yUnit));

        PointDataList markerDataModel = sensorAnalysis.getTagMarkers();
        ITimeData timeCalibration = sensorAnalysis.getTimeData();
        if (timeCalibration.getSize() > 400)
            releaseAdaptersWhenDrawerClosed = true;

        // graph spinner
        graphSpinnerEntryList.add(new GraphSpinnerEntry("Position Data", new MarkerTimeGraphAdapter(markerDataModel,
                timeCalibration, "Position Data",
                new XPositionMarkerGraphAxis(xUnit, sensorAnalysis.getXMinRangeGetter()),
                new YPositionMarkerGraphAxis(yUnit, sensorAnalysis.getYMinRangeGetter()))));
        graphSpinnerEntryList.add(new GraphSpinnerEntry("x-Velocity", new MarkerTimeGraphAdapter(markerDataModel,
                timeCalibration, "x-Velocity", new TimeMarkerGraphAxis(tUnit),
                new XSpeedMarkerGraphAxis(xUnit, tUnit)), true));
        graphSpinnerEntryList.add(new GraphSpinnerEntry("y-Velocity", new MarkerTimeGraphAdapter(markerDataModel,
                timeCalibration, "y-Velocity", new TimeMarkerGraphAxis(tUnit),
                new YSpeedMarkerGraphAxis(yUnit, tUnit)), true));
        graphSpinnerEntryList.add(new GraphSpinnerEntry("Time vs x-Position", new MarkerTimeGraphAdapter(markerDataModel,
                timeCalibration, "Time vs x-Position", new TimeMarkerGraphAxis(tUnit),
                new XPositionMarkerGraphAxis(xUnit, sensorAnalysis.getXMinRangeGetter()))));
        graphSpinnerEntryList.add(new GraphSpinnerEntry("Time vs y-Position", new MarkerTimeGraphAdapter(markerDataModel,
                timeCalibration, "Time vs y-Position", new TimeMarkerGraphAxis(tUnit),
                new YPositionMarkerGraphAxis(yUnit, sensorAnalysis.getYMinRangeGetter()))));

        graphSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                graphSpinnerEntryList));
        graphSpinner.setSelection(0);
        graphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectGraphAdapter(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                graphView.setAdapter(null);
            }
        });

        // setup the load/ unloading of the view data adapters
        sideBar.addListener(sideBarListener);

        if (!releaseAdaptersWhenDrawerClosed) {
            tableView.setAdapter(markerDataTableAdapter);
            selectGraphAdapter(graphSpinner.getSelectedItemPosition());
        }
    }

    public void showRegionOfInterest(MotionAnalysis sensorAnalysis)
    {
        int roiFrame = sensorAnalysis.getFrameDataList().getROIFrame();
        sensorAnalysis.getFrameDataList().setCurrentFrame(roiFrame);
        //sensorAnalysis.getRectMarkers().setVisibility(true);
    }

    public void setRegionOfInterest(MotionAnalysis sensorAnalysis)
    {
        int currentFrame = sensorAnalysis.getFrameDataList().getCurrentFrame();
        float startFrameTime =  sensorAnalysis.getTimeData().getTimeAt(currentFrame);
        Bitmap bmp = sensorAnalysis.getVideoData().getFrame((long) (startFrameTime * 1000.0));
        //this.saveFrame(bmp);

        PointF tLeftM = sensorAnalysis.getRectMarkers().getDataAt(RoiDataListPainter.TOP_LEFT).getPosition();
        PointF bRightM = sensorAnalysis.getRectMarkers().getDataAt(RoiDataListPainter.BTM_RIGHT).getPosition();

        Point topLeft = sensorAnalysis.getVideoData().toVideoPoint(tLeftM);
        Point btmRight = sensorAnalysis.getVideoData().toVideoPoint(bRightM);

        int x = topLeft.x;
        int y = topLeft.y;
        int width = btmRight.x - topLeft.x;
        int height = btmRight.y - topLeft.y;

        tracker.setROI(currentFrame, bmp, x, y, width, height);

        //Set frame target marker to middle of rectangle
        int currentMarker = sensorAnalysis.getTagMarkers().getSelectedData();
        PointF newPos = sensorAnalysis.getVideoData().toMarkerPoint(new Point(x + width / 2, y + height / 2));
        sensorAnalysis.getTagMarkers().setMarkerPosition(newPos, currentMarker);


        sensorAnalysis.getFrameDataList().setObjectPicked(currentFrame, true);
        sensorAnalysis.getFrameDataList().setROIFrame(currentFrame);
    }

    public void saveFrame(Bitmap bmp)
    {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("/sdcard/screen.png");
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @return an icon id for the new state
     */
    public void onToggleSidebar() {
        sideBarState.nextState();
    }

    public int getSideBarStatusIcon() {
        return sideBarState.getIcon();
    }

    private void selectGraphAdapter(int i) {
        if (i < 0) {
            graphView.setFitPainter(null);
            graphView.setAdapter(null);
            return;
        }
        if (releaseAdaptersWhenDrawerClosed && !sideBar.isOpen())
            return;
        GraphSpinnerEntry entry = graphSpinnerEntryList.get(i);
        MarkerGraphAdapter adapter = entry.getMarkerGraphAdapter();
        LinearFitPainter fitPainter = null;
        if (entry.getFit()) {
            fitPainter = new LinearFitPainter();
            fitPainter.setDataAdapter(adapter);
        }
        graphView.setAdapter(adapter);
        graphView.setFitPainter(fitPainter);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    public void release() {
        markerDataTableAdapter.release();
        runContainerView.release();
        tableView.setAdapter((ITableAdapter)null);
        graphView.setAdapter(null);
        graphSpinner.setAdapter(null);
        for (GraphSpinnerEntry entry : graphSpinnerEntryList)
            entry.release();
        graphSpinnerEntryList.clear();
    }

    public void onPause() {
        runContainerView.onPause();
    }

    public void onResume() {
        runContainerView.onResume();
    }



}
