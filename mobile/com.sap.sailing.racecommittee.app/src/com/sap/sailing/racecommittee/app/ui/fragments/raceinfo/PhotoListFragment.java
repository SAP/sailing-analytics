package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.TimeUtils;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.BuildConfig;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.PhotoListAdapter;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.ui.views.decoration.PaddingItemDecoration;
import com.sap.sailing.racecommittee.app.utils.CameraHelper;
import com.sap.sailing.racecommittee.app.utils.MailHelper;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sailing.racecommittee.app.utils.StringHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoListFragment extends BaseFragment {

    private final static String TAG = PhotoListFragment.class.getName();
    private static final int RC_PERMISSION = 2;
    private final static int PHOTO_SHOOTING = 9000;

    private ArrayList<Uri> mPhotos;
    private PhotoListAdapter mAdapter;
    private RecyclerView mPhotoList;
    private Button mSubmit;

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

        ImageView button = ViewHelper.get(layout, R.id.photo_button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        dispatchTakePictureIntent();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, RC_PERMISSION);
                    }
                }
            });
        }

        mPhotoList = ViewHelper.get(layout, R.id.photo_list);
        if (mPhotoList != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            mPhotoList.setLayoutManager(layoutManager);
            mPhotoList.addItemDecoration(
                    new PaddingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.side_padding)));
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
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == RC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
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

    private void dispatchTakePictureIntent() {
        CameraHelper cameraHelper = CameraHelper.on(getActivity());
        Uri photoUri = cameraHelper.getOutputMediaFileUri(CameraHelper.MEDIA_TYPE_IMAGE,
                cameraHelper.getSubFolder(getRace()));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager manager = getActivity().getPackageManager();
        List<ResolveInfo> activities = manager.queryIntentActivities(intent, 0);
        if (activities.size() > 0) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                intent.setClipData(ClipData.newRawUri("", photoUri));
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivityForResult(intent, PHOTO_SHOOTING);
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_camera), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshPhotoList() {
        mPhotos.clear();
        CameraHelper cameraHelper = CameraHelper.on(getActivity());
        File folder = cameraHelper.getOutputMediaFolder(cameraHelper.getSubFolder(getRace()));
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(CameraHelper.MEDIA_TYPE_IMAGE_EXT)
                    || file.getName().endsWith(CameraHelper.MEDIA_TYPE_VIDEO_EXT)) {
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
            if (file.getName().endsWith(CameraHelper.MEDIA_TYPE_IMAGE_EXT)
                    || file.getName().endsWith(CameraHelper.MEDIA_TYPE_VIDEO_EXT)) {
                String authority = BuildConfig.APPLICATION_ID + ".fileprovider";
                Uri contentUri = FileProvider.getUriForFile(requireContext(), authority, file);
                retValue.add(contentUri);
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
        builder.append(
                getString(R.string.results_mail_body_boat_class, getRace().getRaceGroup().getBoatClass().getName()));
        builder.append(getString(R.string.results_mail_body_start,
                TimeUtils.formatTime(getRaceState().getStartTime())));
        builder.append(getString(R.string.results_mail_body_finish,
                TimeUtils.formatTime(getRaceState().getFinishedTime())));
        return builder.toString();
    }
}
