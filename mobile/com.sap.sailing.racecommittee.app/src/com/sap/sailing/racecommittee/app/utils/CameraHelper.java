package com.sap.sailing.racecommittee.app.utils;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraHelper {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static final String MAIN_SUB_FOLDER = "pictures";
    private static final String TAG = CameraHelper.class.getName();

    private Context mContext;

    private CameraHelper(Context context) {
        mContext = context;
    }

    public static CameraHelper on(Context context) {
        return new CameraHelper(context);
    }

    public String getSubFolder(ManagedRace race) {
        String path = "";

        if (race.getRaceGroup() != null) {
            path += race.getRaceGroup().getName() + File.separator;

            if (race.getRaceGroup().getBoatClass() != null) {
                path += race.getRaceGroup().getBoatClass().getName() + File.separator;
            }
        }

        if (race.getFleet() != null && !race.getFleet().getName().equals("Default")) {
            path += race.getFleet().getName() + File.separator;
        }

        if (race.getSeries() != null && !race.getSeries().getName().equals("Default")) {
            path += race.getSeries().getName() + File.separator;
        }

        path += race.getName();

        String sha1 = "";
        try {
            sha1 = HashHelper.SHA1(path);
        } catch (NoSuchAlgorithmException e) {
            ExLog.ex(mContext, TAG, e);
        } catch (UnsupportedEncodingException e) {
            ExLog.ex(mContext, TAG, e);
        }

        return sha1;
    }

    /**
     * Returns a folder Uri for app depended picture folder
     */
    public Uri getOutputMediaFolderUri(@Nullable String subFolder) {
        return Uri.fromFile(getOutputMediaFolder(subFolder));
    }

    /**
     * Create a file Uri for saving an image or video
     */
    public Uri getOutputMediaFileUri(int type, @Nullable String subFolder) {
        return Uri.fromFile(getOutputMediaFile(type, subFolder));
    }

    /**
     * Returns a folder file for app depended picture folder
     */
    public File getOutputMediaFolder(@Nullable String subFolder) {
        if (subFolder == null) {
            subFolder = "";
        } else {
            subFolder += File.separator;
        }

        File mediaStorageDir = new File(mContext.getExternalFilesDir(null), MAIN_SUB_FOLDER + File.separator + subFolder);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                ExLog.i(mContext, TAG, "failed to create directory");
                return null;
            }
        }

        return mediaStorageDir;
    }

    /**
     * Create a File for saving an image or video
     */
    public File getOutputMediaFile(int type, @Nullable String subFolder) {
        File mediaStorageDir = getOutputMediaFolder(subFolder);

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", mContext.getResources().getConfiguration().locale).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
