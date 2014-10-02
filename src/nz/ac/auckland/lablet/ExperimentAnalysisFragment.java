/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet;

import android.app.Activity;
import android.os.Bundle;
import nz.ac.auckland.lablet.experiment.ExperimentData;
import nz.ac.auckland.lablet.experiment.ISensorAnalysis;


/**
 * Fragment that displays a run view container and a tag data graph/table.
 */
public class ExperimentAnalysisFragment extends android.support.v4.app.Fragment {
    protected ISensorAnalysis sensorAnalysis;
    protected ExperimentAnalysis.AnalysisRef analysisRef;

    public ExperimentAnalysisFragment() {
        super();
    }

    public ExperimentAnalysisFragment(ExperimentAnalysis.AnalysisRef ref) {
        super();

        Bundle args = new Bundle();
        args.putInt("sensorIndex", ref.sensor);
        args.putString("analysisIndex", ref.analysisId);
        setArguments(args);
    }

    private ISensorAnalysis findExperimentFromArguments(Activity activity) {
        ExperimentAnalysisActivity experimentActivity = (ExperimentAnalysisActivity)activity;
        if (!experimentActivity.ensureExperimentDataLoaded())
            return null;

        final ExperimentAnalysis experimentAnalysis = experimentActivity.getExperimentAnalysis();
        final int run = experimentAnalysis.getCurrentAnalysisRunIndex();
        final int position = getArguments().getInt("sensorIndex", 0);
        String analysis = getArguments().getString("analysisIndex", "");
        analysisRef = new ExperimentAnalysis.AnalysisRef(run, position, analysis);

        ExperimentAnalysis.AnalysisRunEntry runEntry = experimentAnalysis.getCurrentAnalysisRun();
        return runEntry.sensorList.get(position).getAnalysisEntry(analysis).analysis;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        sensorAnalysis = findExperimentFromArguments(activity);
    }

    public ExperimentData getExperimentData() {
        return ((ExperimentAnalysisActivity)getActivity()).getExperimentAnalysis().getExperimentData();
    }
}
