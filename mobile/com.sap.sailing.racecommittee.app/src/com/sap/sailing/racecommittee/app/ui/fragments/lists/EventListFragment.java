package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import android.text.TextUtils;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.EventSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class EventListFragment extends NamedListFragment<EventBase> {

    //Only used in conjunction with a given ID in arguments
    private boolean loadMore;

    public static EventListFragment newInstance(@Nullable final String id) {
        final EventListFragment fragment = new EventListFragment();
        final Bundle args = new Bundle();
        if (!TextUtils.isEmpty(id)) {
            args.putString(AppConstants.EXTRA_EVENT_ID, id);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ItemSelectedListener<EventBase> attachListener(Context context) {
        if (context instanceof EventSelectedListenerHost) {
            EventSelectedListenerHost listener = (EventSelectedListenerHost) context;
            return listener.getEventSelectionListener();
        }

        throw new IllegalStateException(String.format("%s cannot be attached to a instance of %s",
                EventListFragment.class.getName(), context.getClass().getName()));
    }

    @Override
    protected LoaderCallbacks<DataLoaderResult<Collection<EventBase>>> createLoaderCallbacks(
            ReadonlyDataManager manager
    ) {
        if (getArguments() != null && getArguments().containsKey(AppConstants.EXTRA_EVENT_ID) && !loadMore) {
            final UUID eventId = UUID.fromString(getArguments().getString(AppConstants.EXTRA_EVENT_ID));
            return manager.createEventsLoader(this, eventId);
        }
        return manager.createEventsLoader(this);
    }

    @Override
    public DialogResultListener getListener() {
        return (DialogResultListener) getActivity();
    }

    @Override
    public void onLoadSucceeded(Collection<EventBase> data, boolean isCached) {
        final List<EventBase> list = new ArrayList<>(data.size());
        list.addAll(data);
        final Comparator<EventBase> namedComparator = new NaturalNamedComparator<>();
        Collections.sort(list, (event, anotherEvent) -> {
            if (event.getEndDate() != null) {
                if (anotherEvent.getEndDate() != null) {
                    return event.getEndDate().compareTo(anotherEvent.getEndDate());
                }
                return -1;
            }
            if (event.getStartDate() != null) {
                if (anotherEvent.getStartDate() != null) {
                    return event.getStartDate().compareTo(anotherEvent.getStartDate());
                }
                return -1;
            }
            if (anotherEvent.getStartDate() != null || anotherEvent.getEndDate() != null) {
                return 1;
            }
            final int value = namedComparator.compare(event, anotherEvent);
            if (value != 0) {
                return value;
            }
            return event.getId().toString().compareToIgnoreCase(anotherEvent.getId().toString());
        });
        Collections.reverse(list);
        super.onLoadSucceeded(list, isCached);
        final Bundle arguments = getArguments();
        if (arguments != null && mSelectedIndex == -1) {
            final String id = arguments.getString(AppConstants.EXTRA_EVENT_ID);
            for (EventBase event : list) {
                if (event.getId().toString().equals(id)) {
                    selectItem(event, !loadMore);
                    break;
                }
            }
        }
    }

    public void onExpanded() {
        final Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(AppConstants.EXTRA_EVENT_ID) && !loadMore) {
            loadMore = true;
            showProgressBar(true);
            restartLoader().forceLoad();
        }
    }

    public void onCollapsed() {
        showProgressBar(false);
    }
}
