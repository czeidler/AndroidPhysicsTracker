/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.experiment;

import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.Writer;


/**
 * Interface for a data analysis.
 */
public interface IDataAnalysis {
    String EXPERIMENT_ANALYSIS_FILE_NAME = "experiment_analysis.xml";

    String getDisplayName();
    String getIdentifier();
    boolean loadAnalysisData(Bundle bundle, File storageDir);
    ISensorData[] getData();

    Bundle exportAnalysisData(File additionalStorageDir) throws IOException;
    void exportTagMarkerCSVData(Writer writer) throws IOException;
}
