package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.common.racelog.AuthorPriority;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.LoginType;
import com.sap.sailing.racecommittee.app.ui.adapters.checked.CheckedItem;
import com.sap.sailing.racecommittee.app.ui.adapters.checked.CheckedItemAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.PositionSelectedListenerHost;

import java.util.ArrayList;
import java.util.List;

public class PositionListFragment extends LoggableListFragment {

    private LogEventAuthorImpl author;
    private AppPreferences preferences;
    private final List<LoginTypeItem> values = new ArrayList<>();

    private PositionSelectedListenerHost host;

    public static PositionListFragment newInstance(int priority) {
        final PositionListFragment positionListFragment = new PositionListFragment();
        final Bundle args = new Bundle();
        args.putInt(AppConstants.EXTRA_PRIORITY, priority);
        positionListFragment.setArguments(args);
        return positionListFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        host = (PositionSelectedListenerHost) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.list_fragment, container, false);
        preferences = AppPreferences.on(getActivity());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int priority = getArguments() != null ? getArguments().getInt(AppConstants.EXTRA_PRIORITY, -1) : -1;
        values.clear();
        //Define configuration to activate super user
        if (AuthorPriority.ADMIN.getPriority() == priority) {
            values.add(new LoginTypeItem(
                    AuthorPriority.ADMIN.getPriority(),
                    getString(R.string.login_type_superuser),
                    AppConstants.AUTHOR_TYPE_SUPERUSER /* TODO bug 5456: use user name instead */,
                    LoginType.OFFICER
            ));
        }
        values.add(new LoginTypeItem(
                AuthorPriority.OFFICER_ON_VESSEL.getPriority(),
                getString(R.string.login_type_officer_on_vessel),
                AppConstants.AUTHOR_TYPE_OFFICER_VESSEL /* TODO bug 5456: use user name instead */,
                LoginType.OFFICER
        ));
        values.add(new LoginTypeItem(
                AuthorPriority.SHORE_CONTROL.getPriority(),
                getString(R.string.login_type_shore_control),
                AppConstants.AUTHOR_TYPE_SHORE_CONTROL /* TODO bug 5456: use user name instead */,
                LoginType.OFFICER
        ));
        //Demo mode
        if (AuthorPriority.DEMO_MODE.getPriority() == priority || preferences.isDemoAllowed()) {
            values.add(new LoginTypeItem(
                    AuthorPriority.DEMO_MODE.getPriority(),
                    getString(R.string.login_type_viewer),
                    AppConstants.AUTHOR_TYPE_VIEWER /* TODO bug 5456: use user name instead */,
                    LoginType.VIEWER
            ));
        }

        final List<CheckedItem> items = new ArrayList<>();
        for (LoginTypeItem loginType : values) {
            CheckedItem item = new CheckedItem();
            item.setText(loginType.label);
            items.add(item);
        }

        final CheckedItemAdapter adapter = new CheckedItemAdapter(getActivity(), items);
        setListAdapter(adapter);

        for (int i = 0; i < values.size(); i++) {
            final LoginTypeItem item = values.get(i);
            if (item.priority == priority) {
                positionSelected(i, adapter);
                break;
            }
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ListAdapter adapter = listView.getAdapter();
        positionSelected(position, adapter);
    }

    private void positionSelected(int position, ListAdapter adapter) {
        if (adapter instanceof CheckedItemAdapter) {
            CheckedItemAdapter checkedItemAdapter = (CheckedItemAdapter) adapter;
            checkedItemAdapter.setCheckedPosition(position);
            checkedItemAdapter.notifyDataSetChanged();
        }
        LoginTypeItem item = values.get(position);
        LoginType selectedLoginType = item.type;
        author = new LogEventAuthorImpl(item.name, item.priority);
        preferences.setSendingActive(item.type == LoginType.OFFICER);
        preferences.setAuthor(author);
        ExLog.i(getActivity(), PositionListFragment.class.getName(),
                "Logging in as: " + selectedLoginType + "->" + author);
        if (host != null) {
            host.onPositionSelected(selectedLoginType);
        }
    }

    public LogEventAuthorImpl getAuthor() {
        return author;
    }

    private static class LoginTypeItem {

        private final int priority;
        private final String label;
        private final String name;
        private final LoginType type;

        LoginTypeItem(int priority, String label, String name, LoginType type) {
            this.priority = priority;
            this.label = label;
            this.name = name;
            this.type = type;
        }
    }
}