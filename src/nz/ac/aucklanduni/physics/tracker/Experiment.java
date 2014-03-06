/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.format.Time;

import java.io.File;


abstract public class Experiment {
    private String uid;
    private File storageDir;
    protected Context context;

    public Experiment(Context experimentContext, Bundle bundle, File storageDir) {
        init(experimentContext);

        loadExperiment(bundle, storageDir);
    }

    public Experiment(Context experimentContext) {
        init(experimentContext);

        uid = generateNewUid();
    }

    public String getXUnit() {
        return "";
    }

    public String getYUnit() {
        return "";
    }

    /** The raw data is stored in internal units starting at (0,0). These methods return the max values.
     * The max values ratio should be the same as the screen ratio.
     */
    abstract public float getMaxRawX();
    abstract public float getMaxRawY();

    protected boolean loadExperiment(Bundle bundle, File storageDir) {
        uid = bundle.getString("uid");
        this.storageDir = storageDir;
        return true;
    }

    static public File getMainExperimentDir(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        File experimentDir = new File(baseDir, "experiments");
        if (!experimentDir.exists())
            experimentDir.mkdir();
        return experimentDir;
    }

    public void setStorageDir(File dir) {
        storageDir = dir;
    }
    public File getStorageDir() {
        return storageDir;
    }

    public void onSaveAdditionalData(File dir) {}

    public Bundle experimentDataToBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("uid", uid);

        return bundle;
    }

    public String getUid() {
        return uid;
    }

    protected String getIdentifier() {
        return this.getClass().getSimpleName();
    }

    abstract public int getNumberOfRuns();
    abstract public Bundle getRunAt(int i);
    abstract public float getRunValueAt(int i);
    abstract public String getRunValueBaseUnit();
    abstract public String getRunValueUnitPrefix();
    public String getRunValueUnit() {
        return getRunValueUnitPrefix() + getRunValueBaseUnit();
    }
    abstract public String getRunValueLabel();

    protected String generateNewUid() {
        String identifier = getIdentifier();

        Time now = new Time(Time.getCurrentTimezone());
        CharSequence dateString = android.text.format.DateFormat.format("yyyy-MM-dd_hh-mm-ss", new java.util.Date());

        now.setToNow();
        String newUid = "";
        if (!identifier.equals("")) {
            newUid += identifier;
            newUid += "_";
        }
        newUid += dateString;
        return newUid;
    }

    private void init(Context experimentContext) {
        context = experimentContext;
    }
}
