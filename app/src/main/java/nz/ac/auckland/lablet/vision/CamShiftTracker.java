package nz.ac.auckland.lablet.vision;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nz.ac.auckland.lablet.camera.MotionAnalysis;
import nz.ac.auckland.lablet.camera.VideoData;
import nz.ac.auckland.lablet.data.PointData;
import nz.ac.auckland.lablet.data.PointDataList;
import nz.ac.auckland.lablet.data.RectData;
import nz.ac.auckland.lablet.data.RectDataList;
import nz.ac.auckland.lablet.data.RoiData;
import nz.ac.auckland.lablet.data.RoiDataList;
import nz.ac.auckland.lablet.misc.WeakListenable;

/**
 * Created by Jamie on 27/07/2015.
 */

public class CamShiftTracker extends AsyncTask<Object, Double, SparseArray<Rect>> {

    public interface IListener {
        void onTrackingFinished(SparseArray<Rect> results);

        void onTrackingUpdate(Double percentDone);
    }

    private static String TAG = CamShiftTracker.class.getName();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialisation failed");
        } else {
            Log.i(TAG, "OpenCV initialisation succeeded");
        }
    }

    private TermCriteria termCriteria = new TermCriteria(TermCriteria.EPS | TermCriteria.COUNT, 10, 1);
    private MatOfInt histSize = new MatOfInt(16);
    private MatOfFloat ranges = new MatOfFloat(0, 180);
    private Rect trackWindow;
    private Scalar hsvMin;
    private Scalar hsvMax;
    private int colourRange = 9;
    private WeakListenable<CamShiftTracker.IListener> weakListenable;
    private MotionAnalysis motionAnalysis;
    private boolean debuggingEnabled = false;
    private boolean isTracking = false;

    Mat hsv, hue, mask, hist, backproj, bgr;// = Mat::zeros(200, 320, CV_8UC3), backproj;
    Size size;

    public CamShiftTracker(MotionAnalysis motionAnalysis) {
        weakListenable = new WeakListenable<>();
        this.motionAnalysis = motionAnalysis;
    }

    public void stopTracking() {
        isTracking = false;
    }

    @Override
    protected SparseArray<Rect> doInBackground(Object[] objects) {
        isTracking = true;
        int startFrame = (int) objects[0];
        int endFrame = (int) objects[1];
        RoiDataList roiDataList = (RoiDataList) objects[2];
        VideoData videodata = (VideoData) objects[3];

        RoiData currentRoi = null;
        SparseArray<Rect> results = new SparseArray<>();

        for (int i = startFrame; i <= endFrame && isTracking; i++) {
            RoiData last = getLastRoi(roiDataList, i);

            if (last != null) {
                if (last != currentRoi) {
                    currentRoi = last;
                    Bitmap roiBmp = videodata.getFrame(i);

                    if (roiBmp != null) {
                        PointF topLeft = videodata.toVideoPoint(currentRoi.getTopLeft().getPosition());
                        PointF btmRight = videodata.toVideoPoint(currentRoi.getBtmRight().getPosition());

                        int x = (int) topLeft.x;
                        int y = (int) topLeft.y;
                        int width = (int) (btmRight.x - topLeft.x);
                        int height = (int) (btmRight.y - topLeft.y);
                        setRegionOfInterest(roiBmp, x, y, width, height);
                    } else {
                        Log.d(TAG, "Region of interest BMP is null");
                        return null;
                    }
                }

                if (currentRoi.getFrameId() != i) {
                    long frameTimeMicroseconds = (long) motionAnalysis.getTimeData().getTimeAt(i) * 1000;
                    Bitmap curFrameBmp = videodata.getFrame(frameTimeMicroseconds);

                    if (curFrameBmp != null) {
                        Rect result = getObjectLocation(curFrameBmp);

                        if (result != null) {
                            results.put(i, result);
                        }
                    } else {
                        Log.d(TAG, "Current frame BMP is null: " + i);
                    }
                }
            } else {
                Log.d(TAG, "No region of interests are set");
                return null;
            }

            publishProgress(((double) i + 1) / endFrame);
        }

        return results;
    }

    /**
     * Sets the region of interest for the CamShiftTracker.
     */

    private RoiData getLastRoi(RoiDataList roiDataList, int currentFrame) {
        RoiData data = null;

        for (int i = currentFrame; i >= 0; i--) {
            int roiIndex = roiDataList.getIndexByFrameId(i);

            if (roiIndex != -1) {
                data = roiDataList.getDataAt(roiIndex);
                break;
            }
        }

        return data;
    }

    /*
    *
     */

    public void trackObjects(int startFrame, int endFrame) {

        if (motionAnalysis.getRoiDataList().size() > 0) {
            Object[] objects = new Object[4];
            objects[0] = startFrame; //TODO, add second method and set default start and end. Check that start < end
            objects[1] = endFrame;
            objects[2] = motionAnalysis.getRoiDataList();
            objects[3] = motionAnalysis.getVideoData();
            this.execute(objects);
        } else {
            Log.e(TAG, "Please add a region of interest before calling trackObjects");
        }
    }

    /**
     * Finds an object in a bitmap frameId. The object being searched for is detected based on
     * a pre-specified region of interest (setRegionOfInterest).
     * <p/>
     * Returns a RotatedRect that specifies the location of the object.
     * <p/>
     * Important: setRegionOfInterest must be called before this method is used, otherwise an IllegalStateException
     * will be thrown.
     */

    private Rect getObjectLocation(Bitmap bmp) {
        Mat image = new Mat();

        try {
            Utils.bitmapToMat(bmp, image);
        } catch (Exception e) {
            Log.e(TAG, "getObjectLocation bitmapToMat failed: ", e);
            return null;
        }

        toHsv(image, hsvMin, hsvMax);

        ArrayList<Mat> hsvs = new ArrayList<>();
        hsvs.add(hsv);

        Imgproc.calcBackProject(hsvs, new MatOfInt(0), hist, backproj, ranges, 1);
        //this.saveFrame(backproj, "backproj");
        Core.bitwise_and(backproj, mask, backproj);
        //this.saveFrame(backproj, "backproj_bitwise_and");

        try {
            RotatedRect result = Video.CamShift(backproj, trackWindow, termCriteria);

            if (result.size.equals(new Size(0, 0)) && result.angle == 0 && result.center.equals(new Point(0, 0))) {
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Shit went down: ", e);
            return null;
        }

        return trackWindow.clone();
    }

    private void setRegionOfInterest(Bitmap bmp, int x, int y, int width, int height) {
        size = new Size(bmp.getWidth(), bmp.getHeight());
        hsv = new Mat(size, CvType.CV_8UC3);
        hue = new Mat(size, CvType.CV_8UC3);
        mask = new Mat(size, CvType.CV_8UC3);
        hist = new Mat(size, CvType.CV_8UC3);
        bgr = new Mat();
        backproj = new Mat();

        Mat image = new Mat();

        try {
            Utils.bitmapToMat(bmp, image);
        } catch (Exception e) {
            Log.e(TAG, "getObjectLocation bitmapToMat failed: ", e);
        }
        //this.saveFrame(image, "roi_raw_frame");

        trackWindow = new Rect(x, y, width, height);

        Mat bgrRoi = image.submat(trackWindow);

        Pair<Scalar, Scalar> minMaxHsv = getMinMaxHsv(bgrRoi, 2);
        hsvMin = minMaxHsv.first;
        hsvMax = minMaxHsv.second;

        toHsv(image, hsvMin, hsvMax);

        Mat hsvRoi = hsv.submat(trackWindow);
        //this.saveFrame(hsvRoi, "roi_hue_submat");

        Mat maskRoi = mask.submat(trackWindow);
        //this.saveFrame(maskRoi, "roi_mask_submat");

        ArrayList<Mat> hsvRois = new ArrayList<>();
        hsvRois.add(hsvRoi); //List<Mat> images, MatOfInt channels, Mat mask, Mat hist, MatOfInt histSize, MatOfFloat ranges, boolean accumulate
        Imgproc.calcHist(hsvRois, new MatOfInt(0), maskRoi, hist, histSize, ranges);
        //this.saveFrame(hist, "roi_hist");
        Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX);
        //hist.reshape(-1);
        //this.saveFrame(hist, "roi_hist_norm");
    }

    private Pair<Scalar, Scalar> getMinMaxHsv(Mat bgr, int k) {
        //Convert to HSV
        Mat input = new Mat();
//        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV, 3);
        Imgproc.cvtColor(bgr, input, Imgproc.COLOR_BGR2BGRA, 3);
        //Resize

        //Image quantization using k-means: https://github.com/abidrahmank/OpenCV2-Python-Tutorials/blob/master/source/py_tutorials/py_ml/py_kmeans/py_kmeans_opencv/py_kmeans_opencv.rst
        Mat clusterData = new Mat();

        Mat reshaped = input.reshape(1, bgr.rows() * bgr.cols());
        reshaped.convertTo(clusterData, CvType.CV_32F, 1.0 / 255.0);
        Mat labels = new Mat();
        Mat centres = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
        Core.kmeans(clusterData, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centres);

        //Get num hits for each category
        int[] counts = new int[k];

        for (int i = 0; i < labels.rows(); i++) {
            int label = (int) labels.get(i, 0)[0];
            counts[label] += 1;
        }

        //Get cluster index with maximum number of members
        int maxCluster = 0;
        int index = -1;

        for (int i = 0; i < counts.length; i++) {
            int value = counts[i];

            if (value > maxCluster) {
                maxCluster = value;
                index = i;
            }
        }

        //Get cluster centre point hsv
        int r = (int) (centres.get(index, 2)[0] * 255.0);
        int g = (int) (centres.get(index, 1)[0] * 255.0);
        int b = (int) (centres.get(index, 0)[0] * 255.0);

        Log.d(TAG, "r=" + r + " g=" + g + " b=" + b);

        for (int i = 0; i < centres.rows(); i++) {
            for (int j = 0; j < centres.cols(); j++) {
                double[] centreData = centres.get(i, j);
                Log.d(TAG, "i=" + i + " j=" + j + " centres=" + centreData[0] * 255);
            }
        }

        int sum = (r + g + b) / 3;

        Scalar min;
        Scalar max;

        int rg = Math.abs(r - g);
        int gb = Math.abs(g - b);
        int rb = Math.abs(r - b);
        int maxDiff = Math.max(Math.max(rg, gb), rb);

        if (maxDiff < 35 && sum > 120) //white
        {
            min = new Scalar(0, 0, 0);
            max = new Scalar(180, 40, 255);
        } else if (sum < 50 && maxDiff < 35) //black
        {
            min = new Scalar(0, 0, 0);
            max = new Scalar(180, 255, 40);
        } else {
            Mat bgrColour = new Mat(1, 1, CvType.CV_8UC3, new Scalar(r, g, b));
            Mat hsvColour = new Mat();

            for (int i = 0; i < bgrColour.rows(); i++) {
                for (int j = 0; j < bgrColour.cols(); j++) {
                    double[] data = bgrColour.get(i, j);
                    Log.d(TAG, "MAT: i=" + i + " j=" + j + " bgrColour=" + data[0]);
                }
            }

            Imgproc.cvtColor(bgrColour, hsvColour, Imgproc.COLOR_BGR2HSV, 3);
            double[] hsv = hsvColour.get(0, 0);

            int addition = 0;
            int minHue = (int) hsv[0] - colourRange;
            if (minHue < 0) {
                addition = Math.abs(minHue);
            }

            int maxHue = (int) hsv[0] + colourRange;

            min = new Scalar(Math.max(minHue, 0), 60, Math.max(35, hsv[2] - 30));
            max = new Scalar(Math.min(maxHue + addition, 180), 255, 255);
        }

        return new Pair<>(min, max);
    }

    private void toHsv(Mat image, Scalar hsvMin, Scalar hsvMax) {
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV, 3);

        Core.inRange(hsv, hsvMin, hsvMax, mask);

        Mat output = new Mat();
        Imgproc.cvtColor(mask, output, Imgproc.COLOR_GRAY2BGR, 0);
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2RGBA, 0);
    }

    public void updateMarkers(SparseArray<Rect> results) {
        //Delete all items from arrays
        PointDataList pointDataList = motionAnalysis.getPointDataList();
        RectDataList rectDataList = motionAnalysis.getRectDataList();
        pointDataList.clear();
        rectDataList.clear();

        VideoData videoData = motionAnalysis.getVideoData();

        for (int i = 0; i < results.size(); i++) {
            int frameId = results.keyAt(i);
            Rect result = results.get(frameId);

            float centreX = result.x + result.width / 2;
            float centreY = result.y + result.height / 2;

            //Add point marker
            PointF centre = videoData.toMarkerPoint(new PointF(centreX, centreY));
            PointData centreTag = new PointData(frameId);
            centreTag.setPosition(centre);
            pointDataList.addData(centreTag);

            //Add debugging rectangle
            RectData data = new RectData(frameId);
            data.setCentre(centre);
            data.setAngle(0);
            PointF size = videoData.toMarkerPoint(new PointF(result.width, result.height));
            data.setWidth(size.x);
            data.setHeight(size.y);
            rectDataList.addData(data);
        }
    }

    public void addRegionOfInterest(int frameId) {
        PointDataList pointDataList = motionAnalysis.getPointDataList();
        RoiDataList roiDataList = motionAnalysis.getRoiDataList();

        RoiData data = new RoiData(frameId);
        PointF centre = new PointF(motionAnalysis.getVideoData().getMaxRawX() / 2, motionAnalysis.getVideoData().getMaxRawY() / 2);
        int width = 5;
        int height = 5;
        data.setTopLeft(new PointF(centre.x - width, centre.y + height));
        data.setTopRight(new PointF(centre.x + width, centre.y + height));
        data.setBtmRight(new PointF(centre.x + width, centre.y - height));
        data.setBtmLeft(new PointF(centre.x - width, centre.y - height));
        data.setCentre(centre);
        roiDataList.addData(data);
        pointDataList.removeData(frameId);
    }

    public List<CamShiftTracker.IListener> getListeners() {
        return weakListenable.getListeners();
    }

    public void addListener(CamShiftTracker.IListener listener) {
        weakListenable.addListener(listener);
    }

    public boolean removeListener(CamShiftTracker.IListener listener) {
        return weakListenable.removeListener(listener);
    }

    public boolean hasListener(CamShiftTracker.IListener listener) {
        return weakListenable.hasListener(listener);
    }

    @Override
    protected void onPostExecute(SparseArray<Rect> results) {
        super.onPostExecute(results);

        this.updateMarkers(results);

        for (IListener listener : weakListenable.getListeners())
            listener.onTrackingFinished(results);
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);

        for (IListener listener : weakListenable.getListeners())
            listener.onTrackingUpdate(values[0]);
    }

    public boolean isDebuggingEnabled() {
        return debuggingEnabled;
    }

    public void setDebuggingEnabled(boolean debuggingEnabled) {
        this.debuggingEnabled = debuggingEnabled;
        motionAnalysis.getRectDataList().setVisibility(debuggingEnabled);
    }

    public void saveFrame(Mat mat, String name) {
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);// .createBitmap();
        Utils.matToBitmap(mat, bmp);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream("/sdcard/" + name + ".png");
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
}
