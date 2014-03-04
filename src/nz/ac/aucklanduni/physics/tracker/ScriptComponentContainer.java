package nz.ac.aucklanduni.physics.tracker;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;


class ScriptComponentContainer<ItemType extends ScriptComponent>
        implements ScriptComponent.IScriptComponentListener {
    private List<ItemType> items = new ArrayList<ItemType>();
    private IItemContainerListener listener = null;
    private boolean allItemsDone = false;
    private String lastErrorMessage = "";

    public interface IItemContainerListener {
        public void onAllItemStatusChanged(boolean allDone);
    }

    public boolean initCheck() {
        for (ItemType item : items) {
            if (!item.initCheck()) {
                lastErrorMessage = item.getLastErrorMessage();
                return false;
            }
        }
        return true;
    }

    public void setListener(IItemContainerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onStateChanged(ScriptComponent item, int state) {
        boolean allItemsWereDone = allItemsDone;

        if (state < 0)
            allItemsDone = false;
        else if (!allItemsWereDone)
            allItemsDone = calculateAllItemsDone();

        if (listener == null)
            return;
        if (allItemsDone != allItemsWereDone)
            listener.onAllItemStatusChanged(allItemsDone);
    }

    public void addItem(ItemType item) {
        items.add(item);
        item.setListener(this);

        onStateChanged(item, item.getState());
    }

    public List<ItemType> getItems() {
        return items;
    }

    public boolean allItemsDone() {
        return allItemsDone;
    }

    private boolean calculateAllItemsDone() {
        for (ScriptComponent item : items) {
            if (item.getState() < 0)
                return false;
        }
        return true;
    }

    public void toBundle(Bundle bundle) {
        int i = 0;
        for (ScriptComponent item : items) {
            Bundle childBundle = new Bundle();
            item.toBundle(childBundle);
            String key = "child";
            key += i;

            bundle.putBundle(key, childBundle);
            i++;
        }
    }

    public boolean fromBundle(Bundle bundle) {
        int i = 0;
        for (ScriptComponent item : items) {
            String key = "child";
            key += i;

            Bundle childBundle = bundle.getBundle(key);
            if (childBundle == null)
                return false;
            if (!item.fromBundle(childBundle))
                return false;

            i++;
        }
        return true;
    }
}

abstract class ScriptComponentViewHolder extends ScriptComponent {
    abstract public View createView(Context context, android.support.v4.app.Fragment parent);
}

abstract class ScriptComponentFragmentHolder extends ScriptComponentViewHolder {

    @Override
    public View createView(Context context, android.support.v4.app.Fragment parentFragment) {
        LinearLayout fragmentView = new LinearLayout(context);
        int viewId = View.generateViewId();
        fragmentView.setId(viewId);
        android.support.v4.app.Fragment fragment = createFragment();
        parentFragment.getChildFragmentManager().beginTransaction().add(fragmentView.getId(), fragment).commit();
        return fragmentView;
    }

    abstract public android.support.v4.app.Fragment createFragment();

}