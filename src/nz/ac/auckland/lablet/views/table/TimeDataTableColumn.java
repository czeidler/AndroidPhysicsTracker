/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.views.table;


import nz.ac.auckland.lablet.experiment.Unit;

/**
 * Table column for the marker data table adapter. Provides a time column for the use in combination with an
 * position columns.
 */
public class TimeDataTableColumn extends UnitDataTableColumn {
    final private Unit tUnit;

    public TimeDataTableColumn(Unit tUnit) {
        this.tUnit = tUnit;
        listenTo(tUnit);
    }

    @Override
    public int size() {
        return dataModel.getMarkerCount();
    }

    @Override
    public Number getValue(int index) {
        int runId = dataModel.getMarkerDataAt(index).getRunId();
        return timeData.getTimeAt(runId);
    }

    @Override
    public String getStringValue(int index) {
        Number number = getValue(index);
        return String.format("%.1f", number.floatValue());
    }

    @Override
    public String getHeader() {
        return tUnit.getName() + " [" + tUnit.getUnit() + "]";
    }
}
