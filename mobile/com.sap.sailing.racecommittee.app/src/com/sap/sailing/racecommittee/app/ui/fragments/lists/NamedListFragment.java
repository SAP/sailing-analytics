package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.sap.sailing.android.shared.data.http.UnauthorizedException;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.ui.adapters.checked.CheckedItem;
import com.sap.sailing.racecommittee.app.ui.adapters.checked.CheckedItemAdapter;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DialogListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.FragmentAttachedDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.LoadFailedDialog;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;
import com.sap.sse.common.Named;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class NamedListFragment<T extends Named> extends LoggableListFragment
        implements LoadClient<Collection<T>>, DialogListenerHost {

    protected ArrayList<T> namedList;
    protected List<CheckedItem> checkedItems;
    protected ItemSelectedListener<T> listener;
    protected CheckedItemAdapter listAdapter;
    protected int mSelectedIndex = -1;
    private View footerView;

    protected abstract ItemSelectedListener<T> attachListener(Context context);

    protected abstract LoaderCallbacks<DataLoaderResult<Collection<T>>> createLoaderCallbacks(
            ReadonlyDataManager manager
    );

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = attachListener(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        namedList = new ArrayList<>();
        checkedItems = new ArrayList<>();
        listAdapter = new CheckedItemAdapter(getActivity(), checkedItems);
        if (savedInstanceState != null) {
            mSelectedIndex = savedInstanceState.getInt("position", -1);
            if (mSelectedIndex >= 0) {
                listAdapter.setCheckedPosition(mSelectedIndex);
            }
        }
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        setListAdapter(listAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        footerView = View.inflate(requireContext(), R.layout.footer_progress, null);
        getListView().addFooterView(footerView);

        showProgressBar(true);
        loadItems();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position", mSelectedIndex);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        listAdapter.setCheckedPosition(position);

        mSelectedIndex = position;

        // this unchecked cast here seems unavoidable.
        // even SDK example code does it...
        listener.itemSelected(this, namedList.get(position));
    }

    @Override
    public void onLoadFailed(int loaderId, Exception reason) {
        namedList.clear();
        listAdapter.notifyDataSetChanged();

        showProgressBar(false);

        if (reason instanceof UnauthorizedException) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.loading_failure);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setMessage(getActivity().getString(R.string.user_unauthorized));
            builder.show();
        } else {
            String message = reason.getMessage();
            if (message == null) {
                message = reason.toString();
            }
            showLoadFailedDialog(message);
        }
    }

    @Override
    public void onLoadSucceeded(int loaderId, Collection<T> data, boolean isCached) {
        namedList.clear();
        checkedItems.clear();
        if (isForceLoad() && !isCached) {
            listAdapter.setCheckedPosition(-1);
        }
        // TODO: Quickfix for 2889
        if (data != null) {
            namedList.addAll(data);
            Collections.sort(namedList, new NaturalNamedComparator<T>());
            for (Named named : namedList) {
                CheckedItem item = new CheckedItem();
                item.setText(named.getName());
                item.setSubtext(getEventSubText(named));
                checkedItems.add(item);
            }
            listAdapter.notifyDataSetChanged();
        }

        if (!isForceLoad() || !isCached) {
            showProgressBar(false);
        }
    }

    private String getEventSubText(Named named) {
        String subText = null;
        if (named instanceof EventBase) {
            EventBase eventBase = (EventBase) named;
            String dateString;
            if (eventBase.getStartDate() != null && eventBase.getEndDate() != null) {
                Locale locale = getResources().getConfiguration().locale;
                Calendar startDate = Calendar.getInstance();
                startDate.setTime(eventBase.getStartDate().asDate());
                Calendar endDate = Calendar.getInstance();
                endDate.setTime(eventBase.getEndDate().asDate());
                String start = String.format("%s %s", startDate.getDisplayName(Calendar.MONTH, Calendar.LONG, locale),
                        startDate.get(Calendar.DATE));
                String end = "";
                if (startDate.get(Calendar.MONTH) != endDate.get(Calendar.MONTH)) {
                    end = endDate.getDisplayName(Calendar.MONTH, Calendar.LONG, locale);
                }
                if (startDate.get(Calendar.MONTH) != endDate.get(Calendar.MONTH)
                        || startDate.get(Calendar.DATE) != endDate.get(Calendar.DATE)) {
                    end += " " + endDate.get(Calendar.DATE);
                }
                dateString = String.format("%s %s %s", start, (!TextUtils.isEmpty(end.trim())) ? "-" : "", end.trim());
                subText = String.format("%s%s %s", eventBase.getVenue().getName().trim(),
                        (!TextUtils.isEmpty(dateString) ? ", " : ""),
                        (!TextUtils.isEmpty(dateString) ? dateString : ""));
            }
        }
        return subText;
    }

    ReadonlyDataManager getDataManager() {
        return OnlineDataManager.create(getActivity());
    }

    Loader<DataLoaderResult<Collection<T>>> getLoader() {
        return getLoaderManager().initLoader(0, null, createLoaderCallbacks(getDataManager()));
    }

    private void loadItems() {
        if (isForceLoad()) {
            getLoader().forceLoad();
        } else {
            getLoader().startLoading();
        }
    }

    private boolean isForceLoad() {
        return getArguments() != null && getArguments().getBoolean(AppConstants.ACTION_EXTRA_FORCED);
    }

    private void showLoadFailedDialog(String message) {
        FragmentManager manager = getFragmentManager();
        FragmentAttachedDialogFragment dialog = LoadFailedDialog.create(message);
        // FIXME this can't be the real solution for the autologin
        dialog.setTargetFragment(this, 0);
        // We cannot use DialogFragment#show here because we need to commit the transaction
        // allowing a state loss, because we are effectively in Loader#onLoadFinished()
        manager.beginTransaction().add(dialog, "failedDialog").commitAllowingStateLoss();
    }

    void showProgressBar(boolean visible) {
        footerView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected void selectItem(T eventBase) {
        final int position = namedList.indexOf(eventBase);
        listAdapter.setCheckedPosition(position);

        mSelectedIndex = position;
        listener.itemSelected(this, eventBase);
    }
}