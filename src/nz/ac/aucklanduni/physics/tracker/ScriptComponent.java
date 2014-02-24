/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;


import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;

import java.io.File;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


class Hash {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        // from: http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String sha1Hex(String data) {
        String sha1 = "";
        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(data.getBytes("UTF-8"));
            sha1 = bytesToHex(crypt.digest());
        } catch(Exception e) {
            e.printStackTrace();
        }
        return sha1;
    }
}

public class ScriptComponent implements Iterable<ScriptComponent> {
    final static public int SCRIPT_STATE_INACTIVE = -2;
    final static public int SCRIPT_STATE_ONGOING = -1;
    final static public int SCRIPT_STATE_DONE = 0;

    private Bundle stateData = null;
    private ScriptComponent parent = null;
    private int state = SCRIPT_STATE_INACTIVE;
    private Map<Integer, ScriptComponent> connections = new HashMap<Integer, ScriptComponent>();

    @Override
    public java.util.Iterator<ScriptComponent> iterator() {
        return new Iterator(this);
    }

    static private class Iterator implements java.util.Iterator<ScriptComponent> {
        private ScriptComponent currentComponent;
        private java.util.Iterator<ScriptComponent> currentComponentIterator;
        private java.util.Iterator<ScriptComponent> childIterator;

        Iterator(ScriptComponent root) {
            currentComponent = root;
            currentComponentIterator = currentComponent.connections.values().iterator();
        }

        @Override
        public ScriptComponent next() {
            if (childIterator == null) {
                ScriptComponent child = currentComponentIterator.next();
                childIterator = child.iterator();
                return child;
            } else {
                ScriptComponent child = childIterator.next();
                if (!childIterator.hasNext())
                    childIterator = null;

                return child;
            }
        }

        @Override
        public void remove() {

        }

        @Override
        public boolean hasNext() {
            if (childIterator != null && childIterator.hasNext())
                return true;
            return currentComponentIterator.hasNext();
        }
    }

    static String getChainHash(ScriptComponent component) {
        String hashData = component.getName();
        java.util.Iterator<ScriptComponent> iterator = component.connections.values().iterator();
        int childId = -1;
        while (true) {
            if (!iterator.hasNext())
                break;
            ScriptComponent child = iterator.next();
            childId++;

            hashData += childId;
            hashData += getChainHash(child);
        }

        return Hash.sha1Hex(hashData);
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void toBundle(Bundle bundle) {
        bundle.putInt("state", state);
    }

    public boolean fromBundle(Bundle bundle) {
        if (!bundle.containsKey("state"))
            return false;
        state = bundle.getInt("state");
        return true;
    }

    public void setNextComponent(int state, ScriptComponent component) {
        connections.put(state, component);
        component.setParent(this);
    }

    public ScriptComponent getNext() {
        if (state < 0)
            return null;
        return connections.get(state);
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setState(int state, Bundle stateData) {
        this.state = state;
        this.stateData = stateData;
        onStateChanged(state);
    }

    protected void onStateChanged(int state) {

    }

    public int getState() {
        return state;
    }

    public void setStateData(Bundle data) {
        stateData = data;
    }

    public Bundle getStateData() {
        return stateData;
    }

    public ScriptComponent getParent() {
        return parent;
    }

    public int getStepsToRoot() {
        int stepsToRoot = 1;
        ScriptComponent currentParent = parent;
        while (currentParent != null) {
            stepsToRoot++;
            currentParent = currentParent.getParent();
        }
        return stepsToRoot;
    }

    private void setParent(ScriptComponent parent) {
        this.parent = parent;
    }
}

class Script {
    public interface IScriptListener {
        public void onCurrentComponentChanged(ScriptComponent current);
    }

    private ScriptComponent root = null;
    private ScriptComponent currentComponent = null;
    private IScriptListener listener = null;

    public void setListener(IScriptListener listener) {
        this.listener = listener;
    }

    public void setRoot(ScriptComponent component) {
        root = component;
    }

    public ScriptComponent getRoot() {
        return root;
    }

    static public File getScriptDirectory(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        File scriptDir = new File(baseDir, "scripts");
        if (!scriptDir.exists())
            scriptDir.mkdir();
        return scriptDir;
    }

    static public File getScriptUserDataDir(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        File scriptDir = new File(baseDir, "script_user_data");
        if (!scriptDir.exists())
            scriptDir.mkdir();
        return scriptDir;
    }

    static public String generateScriptUid(String scriptName) {
        Time now = new Time(Time.getCurrentTimezone());
        CharSequence dateString = android.text.format.DateFormat.format("yyyy-MM-dd_hh-mm-ss", new java.util.Date());

        now.setToNow();
        String newUid = "";
        if (!scriptName.equals("")) {
            newUid += scriptName;
            newUid += "_";
        }
        newUid += dateString;
        return newUid;
    }

    public boolean start() {
        // already started?
        if (currentComponent != null)
            return false;

        if (root == null)
            return false;

        setCurrentComponent(root);
        return true;
    }

    public boolean saveScript(Bundle bundle) {
        bundle.putString("scriptId", ScriptComponent.getChainHash(root));

        int componentId = -1;
        java.util.Iterator<ScriptComponent> iterator = root.iterator();
        while (true) {
            if (!iterator.hasNext())
                break;
            ScriptComponent component = iterator.next();
            componentId++;

            Bundle componentBundle = new Bundle();
            component.toBundle(componentBundle);

            String bundleKey = Integer.toString(componentId);
            bundle.putBundle(bundleKey, componentBundle);
        }
        return true;
    }

    public boolean loadScript(Bundle bundle) {
        String scriptId = ScriptComponent.getChainHash(root);
        if (!bundle.get("scriptId").equals(scriptId)) {
            return false;
        }

        int componentId = -1;
        java.util.Iterator<ScriptComponent> iterator = root.iterator();
        while (true) {
            if (!iterator.hasNext())
                break;
            ScriptComponent component = iterator.next();
            componentId++;

            String bundleKey = Integer.toString(componentId);
            if (!bundle.containsKey(bundleKey))
                return false;
            if (!component.fromBundle(bundle.getBundle(bundleKey)))
                return false;
        }

        return true;
    }

    public ScriptComponent getCurrentComponent() {
        return currentComponent;
    }

    public void setCurrentComponent(ScriptComponent component) {
        currentComponent = component;
        currentComponent.setState(ScriptComponent.SCRIPT_STATE_ONGOING);

        if (listener != null)
            listener.onCurrentComponentChanged(currentComponent);
    }

    public boolean cancelCurrent() {
        if (currentComponent == null)
            return false;

        ScriptComponent parent = currentComponent.getParent();
        if (parent == null)
            return false;

        currentComponent.setStateData(null);
        setCurrentComponent(parent);
        return true;
    }

    public boolean backToParent() {
        if (currentComponent == null)
            return false;

        ScriptComponent parent = currentComponent.getParent();
        if (parent == null)
            return false;

        setCurrentComponent(parent);
        return true;
    }

    public boolean next() {
        if (currentComponent == null)
            return false;

        ScriptComponent next = currentComponent.getNext();
        if (next == null)
            return false;

        setCurrentComponent(next);
        return true;
    }
}

