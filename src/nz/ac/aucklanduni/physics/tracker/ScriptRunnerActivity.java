/*
 * Copyright 2013-2014.
 * Distributed under the terms of the GPLv3 License.
 *
 * Authors:
 *      Clemens Zeidler <czei002@aucklanduni.ac.nz>
 */
package nz.ac.aucklanduni.physics.tracker;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptRunnerActivity extends FragmentActivity implements Script.IScriptListener {
    private Script script = null;
    private ViewPager pager = null;
    private ScriptFragmentPagerAdapter pagerAdapter = null;
    private List<ScriptComponentTree> activeChain = new ArrayList<ScriptComponentTree>();
    private File scriptUserDataDir = null;
    private File scriptFile = null;
    private String lastErrorMessage = "";

    final String SCRIPT_USER_DATA_FILENAME = "user_data.xml";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.experiment_analyser);
        // Instantiate a ViewPager and a PagerAdapter.
        pager = (ViewPager)findViewById(R.id.pager);
        pagerAdapter = new ScriptFragmentPagerAdapter(getSupportFragmentManager(), activeChain);
        pager.setAdapter(pagerAdapter);

        if (savedInstanceState != null) {
            String userDataDir = savedInstanceState.getString("script_user_data_dir");
            if (userDataDir == null) {
                showErrorAndFinish("Can't start script from saved instance state (user data directory is null)");
                return;
            }
            scriptUserDataDir = new File(Script.getScriptUserDataDir(this), userDataDir);
            if (!scriptUserDataDir.exists())
                scriptUserDataDir.mkdir();
            if (!loadExistingScript(scriptUserDataDir)) {
                showErrorAndFinish("Can't continue script:", lastErrorMessage);
                return;
            }
        } else if (!createFormIntent()) {
            showErrorAndFinish("Can't start script", lastErrorMessage);
            return;
        }
        script.start();
    }

    private boolean createFormIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            lastErrorMessage = "intent is null";
            return false;
        }

        String scriptName = intent.getStringExtra("script_name");
        String userDataDir = intent.getStringExtra("script_user_data_dir");

        if (userDataDir == null) {
            lastErrorMessage = "user data directory is missing";
            return false;
        }
        scriptUserDataDir = new File(Script.getScriptUserDataDir(this), userDataDir);
        if (!scriptUserDataDir.exists())
            scriptUserDataDir.mkdir();

        if (scriptName != null) {
            // start new script
            scriptFile = new File(Script.getScriptDirectory(this), scriptName);
            if (!loadScript(scriptFile)) {
                ExperimentActivity.recursiveDeleteFile(scriptUserDataDir);
                return false;
            }
        } else if (!loadExistingScript(scriptUserDataDir))
            return false;

        activeChain = script.getActiveChain();
        pagerAdapter.setComponents(activeChain);
        return true;
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putString("script_user_data_dir", scriptUserDataDir.getName());
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveScriptDataToFile();
    }

    protected boolean loadScript(File scriptFile) {
        ScriptComponentFragmentFactory factory = new ScriptComponentFragmentFactory();
        LuaScriptLoader loader = new LuaScriptLoader(factory);
        script = loader.load(scriptFile);
        if (script == null) {
            lastErrorMessage = loader.getLastError();
            return false;
        }
        script.setListener(this);
        return true;
    }

    protected boolean loadExistingScript(File scriptDir) {
        File userDataFile = new File(scriptDir, SCRIPT_USER_DATA_FILENAME);

        Bundle bundle;
        InputStream inStream;
        try {
            inStream = new FileInputStream(userDataFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            lastErrorMessage = "can't open script file \"" + userDataFile.getPath() + "\"";
            return false;
        }

        PersistentBundle persistentBundle = new PersistentBundle();
        try {
            bundle = persistentBundle.unflattenBundle(inStream);
        } catch (Exception e) {
            e.printStackTrace();
            lastErrorMessage = "can't read bundle from \"" + userDataFile.getPath() + "\"";
            return false;
        }

        String scriptPath = bundle.getString("script_name");
        if (scriptPath == null) {
            lastErrorMessage = "bundle contains no script_name";
            return false;
        }

        scriptFile = new File(Script.getScriptDirectory(this), scriptPath);
        if (!loadScript(scriptFile))
            return false;

        if (!script.loadScript(bundle)) {
            lastErrorMessage = script.getLastError();
            return false;
        }

        activeChain = script.getActiveChain();
        pagerAdapter.setComponents(activeChain);

        int lastSelectedFragment = bundle.getInt("current_fragment", 0);
        pager.setCurrentItem(lastSelectedFragment);

        return true;
    }

    protected boolean saveScriptDataToFile() {
        if (scriptFile == null || scriptUserDataDir == null)
            return false;
        if (script == null)
            return false;

        Bundle bundle = new Bundle();
        bundle.putString("script_name", scriptFile.getName());
        bundle.putInt("current_fragment", pager.getCurrentItem());
        if (!script.saveScript(bundle))
            return false;

        File projectFile = new File(scriptUserDataDir, SCRIPT_USER_DATA_FILENAME);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(projectFile);

            PersistentBundle persistentBundle = new PersistentBundle();
            persistentBundle.flattenBundle(bundle, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected void showErrorAndFinish(String error) {
        showErrorAndFinish(error, null);
    }

    protected void showErrorAndFinish(String error, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(error);
        if (message != null)
            builder.setMessage(message);
        builder.setNeutralButton("Ok", null);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public void onComponentStateChanged(ScriptComponentTree current, int state) {
        if (pagerAdapter == null)
            return;

        ScriptComponentTree lastSelectedComponent = null;
        if (activeChain.size() > 0)
            lastSelectedComponent = activeChain.get(pager.getCurrentItem());
        activeChain = script.getActiveChain();
        pagerAdapter.setComponents(activeChain);
        pagerAdapter.notifyDataSetChanged();

        int index = activeChain.indexOf(lastSelectedComponent);
        if (index < 0)
            index = activeChain.size() - 1;
        if (index >= 0)
            pager.setCurrentItem(index);
    }

    @Override
    public void onGoToComponent(ScriptComponentTree next) {
        int index = activeChain.indexOf(next);
        if (index >0)
            pager.setCurrentItem(index);
    }

    private class ScriptFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private List<ScriptComponentTree> components;

        public ScriptFragmentPagerAdapter(android.support.v4.app.FragmentManager fragmentManager,
                                          List<ScriptComponentTree> components) {
            super(fragmentManager);

            this.components = components;
        }

        public void setComponents(List<ScriptComponentTree> components) {
            this.components = components;
            notifyDataSetChanged();
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            ScriptComponentTreeFragmentHolder fragmentCreator
                    = (ScriptComponentTreeFragmentHolder)components.get(position);
            android.support.v4.app.Fragment fragment = fragmentCreator.createFragment();
            return fragment;
        }

        @Override
        public int getCount() {
            return components.size();
        }

        /*
        @Override
        public int getItemPosition(Object object)
        {
            View o = (View)object;
            int index = mMessages.indexOf(o.getTag());
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
            return POSITION_NONE;
        }*/
    }
}
