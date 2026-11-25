package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import android.text.TextUtils;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.ui.adapters.checked.CheckedItem;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.CourseAreaSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class CourseAreaListFragment extends NamedListFragment<CourseArea> {

    private Serializable eventId;

    public static CourseAreaListFragment newInstance(Serializable eventId, @Nullable String uuid) {
        final CourseAreaListFragment fragment = new CourseAreaListFragment();
        final Bundle args = new Bundle();
        args.putSerializable(AppConstants.EXTRA_EVENT_ID, eventId);
        if (!TextUtils.isEmpty(uuid)) {
            args.putSerializable(AppConstants.EXTRA_COURSE_UUID, uuid);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (args != null) {
            eventId = getArguments().getSerializable(AppConstants.EXTRA_EVENT_ID);
        }
    }

    @Override
    protected ItemSelectedListener<CourseArea> attachListener(Context context) {
        if (context instanceof CourseAreaSelectedListenerHost) {
            CourseAreaSelectedListenerHost listener = (CourseAreaSelectedListenerHost) context;
            return listener.getCourseAreaSelectionListener();
        }

        throw new IllegalStateException(String.format("%s cannot be attached to a instance of %s",
                CourseAreaListFragment.class.getName(), context.getClass().getName()));
    }

    @Override
    protected LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createLoaderCallbacks(
            ReadonlyDataManager manager
    ) {
        return manager.createCourseAreasLoader(eventId, this);
    }

    @Override
    public void onLoadSucceeded(Collection<CourseArea> data, boolean isCached) {
        super.onLoadSucceeded(data, isCached);

        for (CheckedItem item : checkedItems) {
            item.setDisabled(true);
        }

        List<String> courses = AppPreferences.on(getActivity()).getManagedCourseAreaNames();
        for (CheckedItem item : checkedItems) {
            for (String allowedCourse : courses) {
                if ("*".equals(allowedCourse) || allowedCourse.equals(item.getText())) {
                    item.setDisabled(false);
                }
            }
        }

        final Bundle args = getArguments();
        if (args != null && mSelectedIndex == -1) {
            final String uuid = args.getString(AppConstants.EXTRA_COURSE_UUID);
            for (CourseArea area : data) {
                if (area.getId().toString().equals(uuid)) {
                    selectItem(area, true);
                    break;
                }
            }
        }
    }

    @Override
    public DialogResultListener getListener() {
        return (DialogResultListener) getActivity();
    }
}
