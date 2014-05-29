/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.script;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.List;


/**
 * Layout parameter for the SheetGroupLayout class.
 */
class SheetGroupLayoutParameters implements ISheetLayoutItemParameters {
    private float weight = 1.f;

    @Override
    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public float getWeight() {
        return weight;
    }
}

/**
 * Simple group layout. Supports horizontal and vertical, nested layouts.
 */
public class SheetGroupLayout extends SheetLayout {
    private List<SheetLayout> items = new ArrayList<SheetLayout>();
    protected TableRow row;
    TableLayout layout;
    int orientation;

    public SheetGroupLayout(int orientation) {
        this.orientation = orientation;
    }

    public SheetGroupLayout(SheetGroupLayoutParameters parameters, int orientation) {
        super(parameters);

        this.orientation = orientation;
    }

    /**
     * Set the orientation of the layout.
     *
     * @param orientation either LinearLayout.HORIZONTAL or LinearLayout.VERTICAL
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    private class LayoutGroupLayoutItem extends SheetLayout implements ISheetLayoutItemParameters {
        private SheetLayout layout;

        public LayoutGroupLayoutItem(SheetLayout layout) {
            this.layout = layout;
        }

        @Override
        public View buildLayout(Context context, android.support.v4.app.Fragment parentFragment) {
            return layout.buildLayout(context, parentFragment);
        }

        @Override
        public void setWeight(float weight) {
            parameters.setWeight(weight);
        }

        @Override
        public float getWeight() {
            return parameters.getWeight();
        }
    }

    private class ViewGroupLayoutItem extends SheetLayout {
        private ScriptComponentViewHolder viewHolder;

        public ViewGroupLayoutItem(ISheetLayoutItemParameters parameters, ScriptComponentViewHolder viewHolder) {
            super(parameters);
            this.viewHolder = viewHolder;
        }

        @Override
        public View buildLayout(Context context, android.support.v4.app.Fragment parentFragment) {
            return viewHolder.createView(context, parentFragment);
        }
    }

    /**
     * Adds a view holder script component to the layout.
     * <p>
     * When the layout is build the view holder's createView method is called. The holder is wrapped into a new
     * SheetLayout layout item that is then returned.
     * </p>
     *
     * @param viewHolder the new child item
     * @return the new layout item for the holder
     */
    public SheetLayout addView(ScriptComponentViewHolder viewHolder) {
        SheetLayout layoutItem = new ViewGroupLayoutItem(viewHolder, viewHolder);
        items.add(layoutItem);
        return layoutItem;
    }

    /**
     * Adds a nested layout
     *
     * @param layout the new child layout
     * @return the new layout item for the layout
     */
    public LayoutGroupLayoutItem addLayout(SheetLayout layout) {
        LayoutGroupLayoutItem layoutItem = new LayoutGroupLayoutItem(layout);
        items.add(layoutItem);
        return layoutItem;
    }

    @Override
    public View buildLayout(Context context, Fragment parentFragment) {
        layout = new TableLayout(context);
        layout.setStretchAllColumns(true);
        row = new TableRow(context);
        layout.addView(row);

        for (int i = 0; i < items.size(); i++)
            add(context, parentFragment, items.get(i), i == items.size() - 1);

        return layout;
    }

    protected void add(Context context, Fragment parentFragment, SheetLayout item, boolean isLast) {
        View view = item.buildLayout(context, parentFragment);

        // set width to zero so that the table layout stretches all columns evenly
        view.setLayoutParams(new TableRow.LayoutParams(0,
                TableRow.LayoutParams.MATCH_PARENT, item.getParameters().getWeight()));
        row.addView(view);

        int xPadding = 20;
        int yPadding = 20;
        if (orientation == LinearLayout.VERTICAL) {
            row = new TableRow(context);
            layout.addView(row);

            if (isLast)
                yPadding = 0;
        } else if (isLast)
            xPadding = 0;

        view.setPadding(0, 0, xPadding, yPadding);
    }
}