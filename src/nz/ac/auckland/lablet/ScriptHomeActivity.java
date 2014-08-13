/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.auckland.lablet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import nz.ac.auckland.lablet.misc.StorageLib;
import nz.ac.auckland.lablet.views.CheckBoxAdapter;
import nz.ac.auckland.lablet.script.Script;
import nz.ac.auckland.lablet.script.ScriptRunnerActivity;
import nz.ac.auckland.lablet.views.CheckBoxListEntry;
import nz.ac.auckland.lablet.views.InfoBarBackgroundDrawable;
import nz.ac.auckland.lablet.views.InfoSideBar;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Main or home activity to manage scripts (lab activities).
 * <p>
 * The user is able to start a new script and resume or delete existing scripts.
 * </p>
 */
public class ScriptHomeActivity extends Activity {
    private List<String> scriptList = null;
    private ArrayAdapter<String> scriptListAdaptor = null;
    private ArrayList<CheckBoxListEntry> existingScriptList = null;
    private CheckBoxListEntry.OnCheckBoxListEntryListener checkBoxListEntryListener;
    private CheckBoxAdapter existingScriptListAdaptor = null;
    private MenuItem deleteItem = null;
    private MenuItem exportItem = null;
    private AlertDialog deleteScriptDataAlertBox = null;
    private AlertDialog infoAlertBox = null;
    private CheckBox selectAllCheckBox = null;

    final int START_SCRIPT = 1;
    final String PREFERENCES_NAME = "lablet_preferences";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.script_activity_actions, menu);

        // to stand alone experiment screen
        MenuItem standAlone = menu.findItem(R.id.action_stand_alone);
        assert(standAlone != null);
        standAlone.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                startStandAloneExperimentActivity();
                return true;
            }
        });

        // info item
        MenuItem infoItem = menu.findItem(R.id.action_info);
        assert(infoItem != null);
        String versionString = ExperimentHomeActivity.getVersionString(this);
        infoItem.setTitle(versionString);
        infoAlertBox = ExperimentHomeActivity.createAlertInfoBox(this);
        infoItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                infoAlertBox.show();
                return true;
            }
        });

        // delete item
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setTitle("Really delete the selected script data?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteSelectedExistingScript();
            }
        });

        deleteScriptDataAlertBox = builder.create();

        deleteItem = menu.findItem(R.id.action_delete);
        assert deleteItem != null;
        deleteItem.setVisible(false);
        deleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (!isAtLeastOneExistingScriptSelected())
                    return false;
                deleteScriptDataAlertBox.show();
                return true;
            }
        });

        exportItem = menu.findItem(R.id.action_mail);
        assert exportItem != null;
        exportItem.setVisible(false);
        exportItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (!isAtLeastOneExistingScriptSelected())
                    return false;
                exportSelection();
                return true;
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean atLeastOneSelected = isAtLeastOneExistingScriptSelected();
        deleteItem.setVisible(atLeastOneSelected);
        exportItem.setVisible(atLeastOneSelected);

        return super.onPrepareOptionsMenu(menu);
    }

    private void exportSelection() {
        List<File> exportList = new ArrayList<>();

        File scriptBaseDir = getScriptUserDataDir(this);
        for (CheckBoxListEntry entry : existingScriptList) {
            if (!entry.getSelected())
                continue;
            File experimentDir = new File(scriptBaseDir, entry.getName());
            exportList.add(experimentDir);
        }

        File[] fileArray = new File[exportList.size()];
        for (int i = 0; i < exportList.size(); i++)
            fileArray[i] = exportList.get(i);

        ExportDirDialog dirDialog = new ExportDirDialog(this, fileArray);
        dirDialog.show();
    }

    private void startStandAloneExperimentActivity() {
        Intent intent = new Intent(this, ExperimentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.script_home);

        int grey = 70;
        int listBackgroundColor = Color.rgb(grey, grey, grey);

        // info side bar
        InfoSideBar infoSideBar = (InfoSideBar)findViewById(R.id.infoSideBar);
        assert infoSideBar != null;
        infoSideBar.setIcon(R.drawable.ic_console);
        infoSideBar.setInfoText("Lab Activities");
        infoSideBar.setBackground(new InfoBarBackgroundDrawable(Color.argb(255, 22, 115, 155)));

        // experiment list
        ListView scriptListView = (ListView)findViewById(R.id.scriptList);
        assert(scriptListView != null);
        scriptListView.setBackgroundColor(listBackgroundColor);
        scriptList = new ArrayList<String>();
        scriptListAdaptor = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, scriptList);

        scriptListView.setAdapter(scriptListAdaptor);

        scriptListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String id = scriptList.get(i);
                startScript(id);
            }
        });

        // existing experiment list
        selectAllCheckBox = (CheckBox)findViewById(R.id.checkBoxSelectAll);
        selectAllCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                for (CheckBoxListEntry entry : existingScriptList)
                    entry.setSelected(b);
                existingScriptListAdaptor.notifyDataSetChanged();
            }
        });

        ListView existingScriptListView = (ListView)findViewById(R.id.existingScriptListView);
        existingScriptListView.setBackgroundColor(listBackgroundColor);
        existingScriptList = new ArrayList<CheckBoxListEntry>();
        existingScriptListAdaptor = new CheckBoxAdapter(this, R.layout.check_box_list_item, existingScriptList);
        existingScriptListView.setAdapter(existingScriptListAdaptor);

        existingScriptListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String id = existingScriptList.get(i).getName();
                loadPreviousScript(id);
            }
        });

        checkBoxListEntryListener = new CheckBoxListEntry.OnCheckBoxListEntryListener() {
            @Override
            public void onSelected(CheckBoxListEntry entry) {
                updateSelectedMenuItem();
            }
        };

        copyResourceScripts(true);
    }

    private void updateSelectedMenuItem() {
        invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();

        selectAllCheckBox.setChecked(false);
        invalidateOptionsMenu();

        updateScriptList();
        updateExistingScriptList();
    }

    /**
     * The script directory is the directory the stores the script files, i.e., the lua files.
     * @param context
     * @return the script directory File
     */
    static public File getScriptDirectory(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        File scriptDir = new File(baseDir, "scripts");
        if (!scriptDir.exists())
            scriptDir.mkdir();
        return scriptDir;
    }

    /**
     * The script user data is the directory that contains the stored script state, i.e., the results.
     * @param context
     * @return the script user data
     */
    private File getScriptUserDataDir(Context context) {
        File baseDir = context.getExternalFilesDir(null);
        File scriptDir = new File(baseDir, "script_user_data");
        if (!scriptDir.exists())
            scriptDir.mkdir();
        return scriptDir;
    }

    private boolean isAtLeastOneExistingScriptSelected() {
        boolean itemSelected = false;
        for (CheckBoxListEntry entry : existingScriptList) {
            if (entry.getSelected()) {
                itemSelected = true;
                break;
            }
        }
        return itemSelected;
    }

    private void deleteSelectedExistingScript() {
        File scriptDir = getScriptUserDataDir(this);
        for (CheckBoxListEntry entry : existingScriptList) {
            if (!entry.getSelected())
                continue;
            File file = new File(scriptDir, entry.getName());
            StorageLib.recursiveDeleteFile(file);
        }
        selectAllCheckBox.setChecked(false);
        deleteItem.setVisible(false);
        updateExistingScriptList();
    }

    private void startScript(String scriptId) {
        String fileName = scriptId + ".lua";

        File scriptPath = new File(getScriptDirectory(this), fileName);
        File scriptUserDataDir = new File(getScriptUserDataDir(this), Script.generateScriptUid(scriptId));
        Intent intent = new Intent(this, ScriptRunnerActivity.class);
        intent.putExtra("script_path", scriptPath.getPath());
        intent.putExtra("script_user_data_dir", scriptUserDataDir.getPath());
        startActivityForResult(intent, START_SCRIPT);
    }

    private boolean loadPreviousScript(String scriptDir) {
        File scriptUserDataDir = new File(getScriptUserDataDir(this), scriptDir);

        Intent intent = new Intent(this, ScriptRunnerActivity.class);
        intent.putExtra("script_user_data_dir", scriptUserDataDir.getPath());
        startActivityForResult(intent, START_SCRIPT);
        return true;
    }

    private void updateScriptList() {
        scriptList.clear();
        File scriptDir = getScriptDirectory(this);
        if (scriptDir.isDirectory()) {
            File[] children = scriptDir.listFiles();
            for (File child : children != null ? children : new File[0]) {
                String name = child.getName();
                if (!isLuaFile(name))
                    continue;
                name = name.substring(0, name.length() - 4);
                scriptList.add(name);
            }
        }

        scriptListAdaptor.notifyDataSetChanged();
    }

    private void copyResourceScripts(boolean overwriteExisting) {
        SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, 0);
        if (settings.getBoolean("scripts_copied", false))
            return;

        File scriptDir = getScriptDirectory(this);
        if (!scriptDir.exists()) {
            if (!scriptDir.mkdir())
                return;
        }
        try {
            String[] files = getAssets().list("");
            for (String file : files) {
                if (!isLuaFile(file))
                    continue;
                InputStream inputStream = getAssets().open(file);
                File scriptOutFile = new File(scriptDir, file);
                if (!overwriteExisting && scriptOutFile.exists())
                    continue;

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(scriptOutFile, false));
                byte[] buffer = new byte[16384];
                while(true) {
                    int n = inputStream.read(buffer);
                    if (n <= -1)
                        break;
                    outputStream.write(buffer, 0, n);
                }

                inputStream.close();
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        settings.edit().putBoolean("scripts_copied", true).commit();
    }

    private boolean isLuaFile(String name) {
        return name.lastIndexOf(".lua") == name.length() - 4;
    }

    private void updateExistingScriptList() {
        existingScriptList.clear();
        File scriptDir = getScriptUserDataDir(this);
        if (scriptDir.isDirectory()) {
            File[] children = scriptDir.listFiles();
            for (File child : children != null ? children : new File[0])
                existingScriptList.add(new CheckBoxListEntry(child.getName(), checkBoxListEntryListener));
        }

        existingScriptListAdaptor.notifyDataSetChanged();
    }
}
