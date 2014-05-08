/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker.script;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import nz.ac.aucklanduni.physics.tracker.R;


/**
 * Default script fragment that contains a default frame layout with the basic interface.
 *
 * Note: there has to be a default constructor in a fragment so that the system can restart it automatically, for
 * example when the screen is rotated. Because we need to know the ScriptComponent associated with the fragment we save
 * the script component index in onSaveInstanceState. However, when loading the script the component is assigned
 * directly using setScriptComponent.
 */
public class ScriptComponentGenericFragment extends android.support.v4.app.Fragment
        implements ScriptComponentTree.IScriptComponentListener {
    protected ScriptComponentTree component;
    protected TextView titleView = null;
    protected Button finishComponentButton = null;
    protected ScrollView containerView = null;

    public void setScriptComponent(ScriptComponentTree component) {
        this.component = component;
        component.setListener(this);
    }

    private void setScriptComponent(int index) {
        ScriptRunnerActivity activity = (ScriptRunnerActivity)getActivity();

        this.component = activity.getScriptComponentTreeAt(index);
        component.setListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            return;

        setScriptComponent(savedInstanceState.getInt("componentIndex"));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ScriptRunnerActivity activity = (ScriptRunnerActivity)getActivity();
        int componentIndex = activity.getScriptComponentIndex(component);
        if (componentIndex < 0)
            return;

        outState.putInt("componentIndex", componentIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.script_component_generic_fragment, container, false);
        assert view != null;

        titleView = (TextView)view.findViewById(R.id.titleTextView);
        String title = ((ScriptComponentTreeFragmentHolder)component).getTitle();
        if (title.equals(""))
            setTitle(this.getClass().getSimpleName());
        else
            setTitle(title);

        finishComponentButton = (Button)view.findViewById(R.id.finishComponentButton);
        assert finishComponentButton != null;

        if (!component.hasChild())
            finishComponentButton.setText("Exit");
        finishComponentButton.setEnabled(false);
        finishComponentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!component.hasChild()) {
                    getActivity().finish();
                    return;
                }
                component.getScript().notifyGoToComponent(component.getNext());
            }
        });

        containerView = (ScrollView)view.findViewById(R.id.childContainer);
        assert containerView != null;

        onStateChanged(component,component.getState());

        return view;
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    protected void setState(int state) {
        component.setState(state);
    }

    protected View setChild(int layoutId) {
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(layoutId, containerView, true);
        assert view != null;

        return view;
    }

    @Override
    public void onStateChanged(ScriptComponent item, int state) {
        assert item == component;

        if (state >= 0)
            finishComponentButton.setEnabled(true);
        else
            finishComponentButton.setEnabled(false);
    }
}
