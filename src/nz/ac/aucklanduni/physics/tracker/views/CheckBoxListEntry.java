/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.views;


public class CheckBoxListEntry {
    private boolean selected = false;
    private String name;
    private OnCheckBoxListEntryListener listener = null;

    public interface OnCheckBoxListEntryListener {
        public void onSelected(CheckBoxListEntry entry);
    }

    public CheckBoxListEntry(String name, OnCheckBoxListEntryListener listener) {
        this.name = name;
        this.listener = listener;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (listener != null)
            listener.onSelected(this);
    }

    public boolean getSelected() {
        return selected;
    }
}
