package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.sap.sailing.domain.common.Named;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.ui.adapters.NamedArrayAdapter;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.BaseDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DialogListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.FragmentAttachedDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.LoadFailedDialog;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;

public abstract class NamedListFragment<T extends Named> extends ListFragment implements LoadClient<Collection<T>>,
        DialogListenerHost {
    
    //private static String TAG = NamedListFragment.class.getName();
    
    private ItemSelectedListener<T> listener;
    private NamedArrayAdapter<T> listAdapter;

    protected ArrayList<T> namedList;

    protected abstract ItemSelectedListener<T> attachListener(Activity activity);

    protected abstract String getHeaderText();

    protected NamedArrayAdapter<T> createAdapter(Context context, ArrayList<T> items) {
        return new NamedArrayAdapter<T>(context, items);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.listener = attachListener(activity);
    }
    
    protected abstract LoaderCallbacks<DataLoaderResult<Collection<T>>> createLoaderCallbacks(ReadonlyDataManager manager);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addHeader();

        namedList = new ArrayList<T>();
        listAdapter = createAdapter(getActivity(), /*android.R.layout.simple_list_item_single_choice, */namedList);

        this.getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        this.setListAdapter(listAdapter);

        showProgressBar(true);
        loadItems();
    }

    private void loadItems() {
        getLoaderManager().restartLoader(0, null, createLoaderCallbacks(OnlineDataManager.create(getActivity())));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // this unchecked cast here seems unavoidable.
        // even SDK example code does it...
        @SuppressWarnings("unchecked")
        T item = (T) l.getItemAtPosition(position);
        listener.itemSelected(this, item);
    }

    private void addHeader() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.selection_list_header_view, getListView(), false);
        getListView().addHeaderView(header, null, false);
        TextView textText = ((TextView) header.findViewById(R.id.textHeader));
        textText.setText(getHeaderText());
    }

    public void onLoadSucceded(Collection<T> data) {
        namedList.clear();
        namedList.addAll(data);
        Collections.sort(namedList, new NaturalNamedComparator());
        listAdapter.notifyDataSetChanged();

        showProgressBar(false);
    }

    @Override
    public void onLoadFailed(Exception reason) {
        namedList.clear();
        listAdapter.notifyDataSetChanged();
        
        showProgressBar(false);

        String message = reason.getMessage();
        if (message == null) {
            message = reason.toString();
        }
        showLoadFailedDialog(message);
    }

    private void showProgressBar(boolean visible) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setProgressBarIndeterminateVisibility(visible);
        }
    }

    private void showLoadFailedDialog(String message) {
        FragmentManager manager = getFragmentManager();
        FragmentAttachedDialogFragment dialog = LoadFailedDialog.create(message);
        dialog.setTargetFragment(this, 0);
        // We cannot use DialogFragment#show here because we need to commit the transaction
        // allowing a state loss, because we are effectively in Loader#onLoadFinished()
        manager.beginTransaction().add(dialog, "failedDialog").commitAllowingStateLoss();
    }
    
    @Override
    public DialogResultListener getListener() {
        return new DialogResultListener() {
            
            @Override
            public void onDialogPositiveButton(BaseDialogFragment dialog) {
                loadItems();
            }
            
            @Override
            public void onDialogNegativeButton(BaseDialogFragment dialog) {
                
            }
        };
    }
}