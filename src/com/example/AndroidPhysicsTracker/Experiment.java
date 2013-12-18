package com.example.AndroidPhysicsTracker;


import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;

import java.io.File;


interface IExperimentRunView {
    public void setCurrentRun(Bundle bundle);
}


abstract public class Experiment {
    private int xUnit;
    private int yUnit;

    private String uid;
    private File storageDir;
    protected Context context;

    public Experiment(Context experimentContext, Bundle bundle, File storageDir) {
        context = experimentContext;
        uid = bundle.getString("uid");
        this.storageDir = storageDir;
    }

    public Experiment(Context experimentContext) {
        context = experimentContext;

        uid = generateNewUid();
    }

    static public File getMainExperimentDir(Context context) {
        return context.getExternalFilesDir(null);
    }

    public void setStorageDir(File dir) {
        storageDir = dir;
    }

    public File getStorageDir() {
        return storageDir;
    }

    public Bundle toBundle() {
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

    protected String generateNewUid() {
        String identifier = getIdentifier();

        Time now = new Time(Time.getCurrentTimezone());
        android.text.format.DateFormat dateFormat = new android.text.format.DateFormat();
        CharSequence dateString = dateFormat.format("yyyy-MM-dd_hh:mm:ss", new java.util.Date());

        now.setToNow();
        String newUid = new String();
        if (identifier != "") {
            newUid += identifier;
            newUid += "_";
        }
        newUid += dateString;
        return newUid;
    }
}
