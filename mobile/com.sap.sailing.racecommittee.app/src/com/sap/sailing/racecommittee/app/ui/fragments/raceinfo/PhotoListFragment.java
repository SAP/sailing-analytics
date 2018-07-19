package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.PhotoListAdapter;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.ui.views.decoration.PaddingItemDecoration;
import com.sap.sailing.racecommittee.app.utils.CameraHelper;
import com.sap.sailing.racecommittee.app.utils.MailHelper;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sailing.racecommittee.app.utils.StringHelper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoListFragment extends BaseFragment {

    private final static String TAG = PhotoListFragment.class.getName();
    private final static int PHOTO_SHOOTING = 9000;

    private ArrayList<Uri> mPhotos;
    private PhotoListAdapter mAdapter;
    private RecyclerView mPhotoList;
    private Button mSubmit;
    private SimpleDateFormat mDateFormat;

    public PhotoListFragment() {
        mPhotos = new ArrayList<>();
    }

    public static PhotoListFragment newInstance(Bundle args) {
        PhotoListFragment fragment = new PhotoListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.photo_list, container, false);

        mDateFormat = new SimpleDateFormat("HH:mm:ss", getResources().getConfiguration().locale);

        ImageView button = ViewHelper.get(layout, R.id.photo_button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CameraHelper cameraHelper = CameraHelper.on(getActivity());
                    Uri photoUri = cameraHelper.getOutputMediaFileUri(CameraHelper.MEDIA_TYPE_IMAGE, cameraHelper.getSubFolder(getRace()));
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    PackageManager manager = getActivity().getPackageManager();
                    List<ResolveInfo> activities = manager.queryIntentActivities(intent, 0);
                    if (activities.size() > 0) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(intent, PHOTO_SHOOTING);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.no_camera), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        mPhotoList = ViewHelper.get(layout, R.id.photo_list);
        if (mPhotoList != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            mPhotoList.setLayoutManager(layoutManager);
            mPhotoList.addItemDecoration(new PaddingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.side_padding)));
        }

        mSubmit = ViewHelper.get(layout, R.id.submit_button);
        if (mSubmit != null) {
            mSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String recipient = preferences.getMailRecipient();
                    MailHelper.send(new String[] { recipient }, getSubject(), getBody(), getPhotos(), getActivity());
                }
            });
        }

        HeaderLayout header = ViewHelper.get(layout, R.id.header);
        if (header != null) {
            if (getActivity().findViewById(R.id.finished_edit) == null) {
                header.setVisibility(View.GONE);
            } else {
                header.setHeaderOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendIntent(AppConstants.INTENT_ACTION_CLEAR_TOGGLE);
                        sendIntent(AppConstants.INTENT_ACTION_SHOW_SUMMARY_CONTENT);
                    }
                });
            }
        }
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        mAdapter = new PhotoListAdapter(mPhotos);
        mPhotoList.setAdapter(mAdapter);
        refreshPhotoList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case PHOTO_SHOOTING:
                refreshPhotoList();
                ExLog.i(getActivity(), TAG, "Returned from Photo");
                break;

            default:
                break;
        }
    }

    private void refreshPhotoList() {
        mPhotos.clear();
        CameraHelper cameraHelper = CameraHelper.on(getActivity());
        File folder = cameraHelper.getOutputMediaFolder(cameraHelper.getSubFolder(getRace()));
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(CameraHelper.MEDIA_TYPE_IMAGE_EXT) || file.getName().endsWith(CameraHelper.MEDIA_TYPE_VIDEO_EXT)) {
                mPhotos.add(Uri.fromFile(file));
            }
        }
        Collections.sort(mPhotos, new Comparator<Uri>() {
            @Override
            public int compare(Uri lhs, Uri rhs) {
                return lhs.getEncodedPath().compareTo(rhs.getEncodedPath());
            }
        });
        if (mSubmit != null) {
            mSubmit.setEnabled(mPhotos.size() != 0);
        }
    }

    private ArrayList<Uri> getPhotos() {
        ArrayList<Uri> retValue = new ArrayList<>();
        CameraHelper cameraHelper = CameraHelper.on(getActivity());
        File folder = cameraHelper.getOutputMediaFolder(cameraHelper.getSubFolder(getRace()));
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(CameraHelper.MEDIA_TYPE_IMAGE_EXT) || file.getName().endsWith(CameraHelper.MEDIA_TYPE_VIDEO_EXT)) {
                retValue.add(Uri.fromFile(file));
            }
        }
        Collections.sort(retValue, new Comparator<Uri>() {
            @Override
            public int compare(Uri lhs, Uri rhs) {
                return lhs.getEncodedPath().compareTo(rhs.getEncodedPath());
            }
        });
        return retValue;
    }

    private String getSubject() {
        String author = StringHelper.on(getActivity()).getAuthor(preferences.getAuthor().getName());
        return getString(R.string.results_mail_subject, author, RaceHelper.getRaceName(getRace()));
    }

    private String getBody() {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.results_mail_body, RaceHelper.getRaceName(getRace())));
        builder.append(getString(R.string.results_mail_body_race_group, getRace().getRaceGroup().getName()));
        builder.append(getString(R.string.results_mail_body_boat_class, getRace().getRaceGroup().getBoatClass().getName()));
        builder.append(getString(R.string.results_mail_body_start, mDateFormat.format(getRaceState().getStartTime().asDate())));
        builder.append(getString(R.string.results_mail_body_finish, mDateFormat.format(getRaceState().getFinishedTime().asDate())));
        return builder.toString();
    }
}
