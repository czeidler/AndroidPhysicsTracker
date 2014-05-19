/*
 * Copyright 2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.lablet.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;


/**
 * Load a lua script file from disk.
 */
public class LuaScriptLoader {
    /**
     * Simple script builder. Only supports components with one child.
     */
    private class ScriptBuilder {
        private IScriptComponentFactory factory;
        private Script script = new Script();
        private ScriptComponentTree lastComponent;

        public ScriptBuilder(IScriptComponentFactory factory) {
            this.factory = factory;
        }

        /**
         * Adds the component at the SCRIPT_STATE_DONE slot.
         *
         * @param component to be added
         */
        public void add(ScriptComponentTree component) {
            add(ScriptComponentTree.SCRIPT_STATE_DONE, component);
        }

        /**
         * Adds a component to the state slot and make it the current component (Successive components will be added to
         * the new component).
         *
         * @param state the slot where the component will be inserted
         * @param component to be added
         */
        public void add(int state, ScriptComponentTree component) {
            if (lastComponent == null) {
                script.setRoot(component);
                lastComponent = component;
            } else {
                lastComponent.setChildComponent(state, component);
                lastComponent = component;
            }
        }

        public Script getScript() {
            return script;
        }

        public ScriptComponentTree create(String componentName) {
            return factory.create(componentName, script);
        }
    }

    private String lastError = "";
    private ScriptBuilder builder;

    public LuaScriptLoader(IScriptComponentFactory factory) {
        builder = new ScriptBuilder(factory);
    }

    // this is basically a copy from the luaj code but it uses a BufferedInputStream
    private LuaValue loadfile(Globals globals, String filename) {
        try {
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filename));
            return globals.load(inputStream, "@" + filename, "bt", globals);
        } catch (Exception e) {
            return LuaValue.error("load " + filename + ": " + e);
        }
    }

    /**
     * Opens a script file and load it into a {@link Script}.
     *
     * @param scriptFile the script location
     * @return a new script or null on failure
     */
    public Script load(File scriptFile) {
        if (!scriptFile.exists()) {
            lastError = "Script file does not exist!";
            return null;
        }

        try {
            Globals globals = JsePlatform.standardGlobals();
            //LuaValue chunk = globals.loadfile(scriptFile.getPath());
            LuaValue chunk = loadfile(globals, scriptFile.getPath());
            chunk.call();

            LuaValue hookFunction = globals.get("onBuildExperimentScript");
            LuaValue arg = CoerceJavaToLua.coerce(builder);
            hookFunction.call(arg);
        } catch (LuaError e) {
            lastError = e.getMessage();
            return null;
        }

        Script script = builder.getScript();
        if (!script.initCheck()) {
            lastError = script.getLastError();
            return null;
        }
        return script;
    }

    /**
     * Returns the error message in case an error occurred in load.
     * @return error message string
     */
    public String getLastError() {
        return lastError;
    }
}
