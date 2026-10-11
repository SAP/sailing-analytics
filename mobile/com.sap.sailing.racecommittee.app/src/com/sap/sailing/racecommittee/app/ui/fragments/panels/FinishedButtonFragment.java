package com.sap.sailing.racecommittee.app.ui.fragments.panels;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.NavigationEvents;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.PenaltyFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.PhotoListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.TrackingListFragment;

public class FinishedButtonFragment extends BasePanelFragment implements NavigationEvents.NavigationListener {

    private final static String TAG = FinishedButtonFragment.class.getName();

    private RelativeLayout mRecord;
    private View mRecordLock;
    private RelativeLayout mPhoto;
    private View mPhotoLock;
    private RelativeLayout mList;
    private View mListLock;
    private ImageView mWarning;

    public FinishedButtonFragment() {
    }

    public static FinishedButtonFragment newInstance(Bundle args) {
        FinishedButtonFragment fragment = new FinishedButtonFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isCameraAvailable(Context context) {
        PackageManager manager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        } else {
            return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                    || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_finished_buttons, container, false);

        mRecord = ViewHelper.get(layout, R.id.record_button);
        if (mRecord != null) {
            mRecord.setOnClickListener(new RecordClick());
        }
        mRecordLock = ViewHelper.get(layout, R.id.voice_lock);

        mPhoto = ViewHelper.get(layout, R.id.photo_button);
        if (mPhoto != null) {
            mPhoto.setOnClickListener(new PhotoClick());
        }
        mPhotoLock = ViewHelper.get(layout, R.id.photo_lock);
        if (!isCameraAvailable(requireContext())) {
            mPhotoLock.setVisibility(View.VISIBLE);
        }

        mList = ViewHelper.get(layout, R.id.list_button);
        if (mList != null) {
            mList.setOnClickListener(new ListClick());
        }
        mListLock = ViewHelper.get(layout, R.id.list_lock);

        mWarning = ViewHelper.get(layout, R.id.panel_additional_image);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!preferences.getRacingProcedureIsResultEntryEnabled(getRaceState().getRacingProcedure().getType())) {
            mList.setVisibility(View.GONE);
        } else {
            CompetitorResults results = getRaceState().getConfirmedFinishPositioningList().getCompetitorResults();
            if (results != null) {
                mWarning.setVisibility(results.hasConflicts() ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        NavigationEvents.INSTANCE.subscribeFragmentAttachment(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        NavigationEvents.INSTANCE.unSubscribeFragmentAttachment(this);
    }

    private void uncheckMarker() {
        if (isAdded()) {
            // TODO
            setMarkerLevel(mRecord, R.id.record_marker, LEVEL_NORMAL);

            resetFragment(null, getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, false),
                    PhotoListFragment.class);
            setMarkerLevel(mPhoto, R.id.photo_marker, LEVEL_NORMAL);

            resetFragment(null, getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, false),
                    TrackingListFragment.class);
            setMarkerLevel(mList, R.id.list_marker, LEVEL_NORMAL);
        }
    }

    private void updateMarker(View view, boolean checked) {
        int level = checked ? LEVEL_TOGGLED : LEVEL_NORMAL;
        if (isAdded()) {
            if (mRecord.equals(view)) {
                // TODO
                setMarkerLevel(mRecord, R.id.record_marker, level);
            }

            if (mPhoto.equals(view)) {
                setMarkerLevel(mPhoto, R.id.photo_marker, level);
            }

            if (mList.equals(view)) {
                setMarkerLevel(mList, R.id.list_marker, level);
            }
        }
    }


    @Override
    public void onFragmentAttach(Fragment fragment) {
        uncheckMarker();
        if (fragment instanceof PhotoListFragment) {
            updateMarker(mPhoto, true);
        } else if (fragment instanceof TrackingListFragment || fragment instanceof PenaltyFragment) {
            updateMarker(mList, true);
        }
    }

    @Override
    public void onFragmentDetach(Fragment fragment) {
        if (fragment instanceof PhotoListFragment) {
            updateMarker(mPhoto, false);
        } else if (fragment instanceof TrackingListFragment || fragment instanceof PenaltyFragment) {
            updateMarker(mList, false);
        }
    }

    private class RecordClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRecordLock == null || mRecordLock.getVisibility() == View.GONE) {
                switch (toggleMarker(v, R.id.record_marker)) {
                    case LEVEL_NORMAL:
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                        break;

                    case LEVEL_TOGGLED:
                        // TODO
                        break;

                    default:
                        ExLog.i(getActivity(), TAG, "Unknown return value");
                        break;
                }
            }
        }
    }

    private class PhotoClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mPhotoLock == null || mPhotoLock.getVisibility() == View.GONE) {
                switch (toggleMarker(v, R.id.photo_marker)) {
                    case LEVEL_NORMAL:
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                        break;

                    case LEVEL_TOGGLED:
                        replaceFragment(PhotoListFragment.newInstance(getRecentArguments()),
                                getFrameId(requireActivity(), R.id.finished_edit, R.id.finished_content, true));
                        break;

                    default:
                        ExLog.i(getActivity(), TAG, "Unknown return value");
                        break;
                }
            }
        }
    }

    private class ListClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mListLock == null || mListLock.getVisibility() == View.GONE) {
                switch (toggleMarker(v, R.id.list_marker)) {
                    case LEVEL_NORMAL:
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                        break;

                    case LEVEL_TOGGLED:
                        replaceFragment(TrackingListFragment.newInstance(getRecentArguments(), 0),
                                getFrameId(requireActivity(), R.id.finished_edit, R.id.finished_content, true));
                        break;

                    default:
                        ExLog.i(getActivity(), TAG, "Unknown return value");
                        break;
                }
            }
        }
    }
}
