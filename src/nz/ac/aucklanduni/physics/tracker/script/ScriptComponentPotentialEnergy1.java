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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import nz.ac.aucklanduni.physics.tracker.R;

class ScriptComponentPotentialEnergy1View extends FrameLayout {
    private ScriptComponentPotentialEnergy1 component;
    private TextView heightQuestionTextView;
    private TextView energyQuestionTextView;
    private EditText heightEditText;
    private EditText energyEditText;
    private EditText pbjEditText;
    private CheckBox doneCheckBox;

    public ScriptComponentPotentialEnergy1View(Context context, ScriptComponentPotentialEnergy1 component) {
        super(context);
        this.component = component;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.script_component_potential_energy_1, null, true);
        assert view != null;
        addView(view);

        heightQuestionTextView = (TextView)view.findViewById(R.id.heightQuestionTextView);
        assert heightQuestionTextView != null;
        energyQuestionTextView = (TextView)view.findViewById(R.id.energyQuestionTextView);
        assert energyQuestionTextView != null;

        heightEditText = (EditText)view.findViewById(R.id.heightEditText);
        assert heightEditText != null;
        energyEditText = (EditText)view.findViewById(R.id.energyEditText);
        assert energyEditText != null;
        pbjEditText = (EditText)view.findViewById(R.id.pbjEditText);
        assert pbjEditText != null;

        doneCheckBox = (CheckBox)view.findViewById(R.id.doneCheckBox);
        assert doneCheckBox != null;

        heightQuestionTextView.setText(component.getHeightQuestionText());
        energyQuestionTextView.setText(component.getEnergyQuestionTextView());

        if (component.getState() == ScriptComponentTree.SCRIPT_STATE_DONE)
            doneCheckBox.setChecked(true);

        heightEditText.setText(Float.toString(component.getHeight()));
        energyEditText.setText(Float.toString(component.getEnergy()));
        pbjEditText.setText(Float.toString(component.getPbjValue()));

        heightEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    setHeight(Float.parseFloat(editable.toString()));
                } catch (NumberFormatException e) {

                }
            }
        });
        energyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    setEnergy(Float.parseFloat(editable.toString()));
                } catch (NumberFormatException e) {

                }
            }
        });
        pbjEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    setPbjValue(Float.parseFloat(editable.toString()));
                } catch (NumberFormatException e) {

                }
            }
        });
    }

    private void setHeight(float height) {
        component.setHeight(height);
        update();
    }

    private void setEnergy(float energy) {
        component.setEnergy(energy);
        update();
    }

    private void setPbjValue(float value) {
        component.setPbjValue(value);
        update();
    }

    private void update() {
        if (checkValues()) {
            component.setState(ScriptComponent.SCRIPT_STATE_DONE);
            doneCheckBox.setChecked(true);
        } else {
            component.setState(ScriptComponent.SCRIPT_STATE_ONGOING);
            doneCheckBox.setChecked(false);
        }
    }

    private boolean checkValues() {
        final float g = 9.81f;
        final float cal = 4.184f;
        final float pbjSandwichCal = 432.f;

        if (isFuzzyZero(component.getHeight()) || isFuzzyZero(component.getMass()))
            return false;

        float correctEnergy = component.getMass() * component.getHeight() * g;
        float correctPbjValue = pbjSandwichCal / (correctEnergy / cal);

        if (!isFuzzyEqual(component.getEnergy(), correctEnergy))
            return false;
        if (!isFuzzyEqual(component.getPbjValue(), correctPbjValue))
            return false;

        return true;
    }

    private boolean isFuzzyZero(float value) {
        if (Math.abs(value) < 0.0001)
            return true;
        return false;
    }

    private boolean isFuzzyEqual(float value, float correctValue) {
        if (Math.abs(value - correctValue) < correctValue * 0.05f)
            return true;
        return false;
    }
}


public class ScriptComponentPotentialEnergy1 extends ScriptComponentViewHolder {
    private String heightQuestionText = "Height of the of a mass with on 1kg.";
    private String energyQuestionTextView = "What is its energy?";

    private float mass = 1.f;
    private float height = 0.0f;
    private float energy = 0.0f;
    private float pbjValue = 0.0f;

    public ScriptComponentPotentialEnergy1() {
        setState(ScriptComponentTree.SCRIPT_STATE_ONGOING);
    }

    public String getHeightQuestionText() {
        return heightQuestionText;
    }
    public void setHeightQuestionText(String heightQuestionText) {
        this.heightQuestionText = heightQuestionText;
    }
    public String getEnergyQuestionTextView() {
        return energyQuestionTextView;
    }
    public void setEnergyQuestionTextView(String energyQuestionTextView) {
        this.energyQuestionTextView = energyQuestionTextView;
    }

    public float getMass() {
        return mass;
    }
    public void setMass(float mass) {
        this.mass = mass;
    }
    public float getHeight() {
        return height;
    }
    public void setHeight(float height) {
        this.height = height;
    }
    public float getEnergy() {
        return energy;
    }
    public void setEnergy(float energy) {
        this.energy = energy;
    }
    public float getPbjValue() {
        return pbjValue;
    }
    public void setPbjValue(float pbjValue) {
        this.pbjValue = pbjValue;
    }

    @Override
    public View createView(Context context, android.support.v4.app.Fragment parent) {
        return new ScriptComponentPotentialEnergy1View(context, this);
    }

    @Override
    public boolean initCheck() {
        return true;
    }

    @Override
    public void toBundle(Bundle bundle) {
        super.toBundle(bundle);

        bundle.putFloat("mass", mass);
        bundle.putFloat("height", height);
        bundle.putFloat("energy", energy);
        bundle.putFloat("pbjValue", pbjValue);
    }

    @Override
    public boolean fromBundle(Bundle bundle) {
        mass = bundle.getFloat("mass", 1.f);
        height = bundle.getFloat("height", 0.f);
        energy = bundle.getFloat("energy");
        pbjValue = bundle.getFloat("pbjValue");

        return super.fromBundle(bundle);
    }
}