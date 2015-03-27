/*
 * Copyright 2013-2015.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet.script;

import nz.ac.auckland.lablet.ScriptHomeActivity;
import nz.ac.auckland.lablet.misc.StorageLib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class ScriptMetaData {
    final public File file;
    final public float interfaceVersion;

    public ScriptMetaData(File file, float interfaceVersion) {
        this.file = file;
        this.interfaceVersion = interfaceVersion;
    }

    public String getScriptFileName() {
        return StorageLib.removeExtension(file.getName());
    }

    public String getLabel() {
        return getScriptFileName();
    }

    public String toString() {
        return getLabel();
    }

    public String readRemote() {
        File parent = file.getParentFile();
        File remoteFile = new File(parent, getScriptFileName() + "." + ScriptHomeActivity.REMOTE_TYPE);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(remoteFile));
            return reader.readLine();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}
