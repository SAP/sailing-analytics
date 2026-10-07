package com.sap.sailing.android.tracking.app.ui.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.LocationHelper;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.activities.LeaderboardWebViewActivity;
import com.sap.sailing.android.tracking.app.ui.activities.RegattaActivity;
import com.sap.sailing.android.tracking.app.ui.activities.TrackingActivity;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RegattaFragment extends BaseFragment implements OnClickListener {

    private final String TAG = RegattaFragment.class.toString();

    private final int CAMERA_REQUEST_CODE = 3118;
    private final int SELECT_PHOTO_REQUEST_CODE = 1693;
    private final int IMAGE_MAX_SIZE = 2000;

    private final String CAMERA_TEMP_FILE = "cameraTempFile";

    private boolean showingThankYouNote;

    private TimerRunnable timer;

    private FragmentWatcher watcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_regatta, container, false);

        Button startTrackingButton = (Button) view.findViewById(R.id.start_tracking);
        startTrackingButton.setOnClickListener(this);

        Button showLeaderboardButton = (Button) view.findViewById(R.id.show_leaderboards_button);
        showLeaderboardButton.setOnClickListener(this);

        Button showEventButton = (Button) view.findViewById(R.id.show_event_button);
        showEventButton.setOnClickListener(this);

        Button changePhotoButton = (Button) view.findViewById(R.id.change_photo_button);
        changePhotoButton.setOnClickListener(this);

        ImageButton addPhotoButton = (ImageButton) view.findViewById(R.id.add_photo_button);
        addPhotoButton.setOnClickListener(this);

        TextView addPhotoText = (TextView) view.findViewById(R.id.add_photo_text);
        addPhotoText.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppPreferences prefs = new AppPreferences(getActivity());
        RegattaActivity activity = (RegattaActivity) getActivity();
        if (prefs.hasFailedUpload(activity.leaderboard.name)) {
            activity.showRetryUploadLayout();
        }
    }

    /**
     * If the regatta started, don't display the countdown any more.
     */
    private void checkAndHideCountdownIfRegattaIsInProgress() {
        TextView textView = (TextView) getActivity().findViewById(R.id.regatta_starts_in);

        RegattaActivity regattaActivity = (RegattaActivity) getActivity();
        long regattaStart = regattaActivity.event.startMillis;

        LinearLayout threeBoxesLayout = (LinearLayout) getActivity().findViewById(R.id.three_boxes_regatta_starts);

        if (System.currentTimeMillis() > regattaStart) {
            textView.setText(getString(R.string.regatta_in_progress));
            threeBoxesLayout.setVisibility(View.GONE);
            centerViewInParent(textView);
        } else {
            textView.setText(getString(R.string.regatta_starts_in));
            threeBoxesLayout.setVisibility(View.VISIBLE);
        }
    }

    private void centerViewInParent(View view) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        view.setLayoutParams(layoutParams);
    }

    public boolean isShowingBigCheckoutButton() {
        return showingThankYouNote;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void switchToThankYouScreen() {
        showingThankYouNote = true;

        RelativeLayout startsInLayout = (RelativeLayout) getActivity().findViewById(R.id.start_date_layout);
        startsInLayout.setVisibility(View.INVISIBLE);

        Button startTrackingButton = (Button) getActivity().findViewById(R.id.start_tracking);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            startTrackingButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_btn_yellow));
        } else {
            startTrackingButton.setBackground(getResources().getDrawable(R.drawable.rounded_btn_yellow));
        }

        startTrackingButton.setText(getActivity().getString(R.string.close));

        TextView bottomAnnouncement = (TextView) getActivity().findViewById(R.id.bottom_announcement);
        bottomAnnouncement.setVisibility(View.INVISIBLE);

        RelativeLayout thankYouLayout = (RelativeLayout) getActivity().findViewById(R.id.thank_you_layout);
        thankYouLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (watcher != null) {
            watcher.onViewCreated();
        }
        timer = new TimerRunnable();
        timer.start();
        checkAndHideCountdownIfRegattaIsInProgress();
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.stop();
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.show_leaderboards_button) {
            startLeaderboardActivity();
        } else if (id == R.id.show_event_button) {
            startEventActivity();
        } else if (id == R.id.start_tracking) {
            if (showingThankYouNote) {
                final RegattaActivity regattaActivity = (RegattaActivity) getActivity();
                regattaActivity.checkout();
            } else if (LocationHelper.isGPSEnabled(getActivity())) {
                startTrackingActivity();
            } else {
                LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
            }
        } else if (id == R.id.add_photo_button) {
            showChooseExistingPictureOrTakeNewPhotoAlert();
        } else if (id == R.id.add_photo_text) {
            showChooseExistingPictureOrTakeNewPhotoAlert();
        } else if (id == R.id.change_photo_button) {
            showChooseExistingPictureOrTakeNewPhotoAlert();
        }
    }

    public void setChangePhotoButtonHidden(boolean hidden) {
        LinearLayout changePhotoLayout = (LinearLayout) getActivity().findViewById(R.id.change_photo);
        if (hidden) {
            changePhotoLayout.setVisibility(View.INVISIBLE);
        } else {
            changePhotoLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Ask user if he wants to take a new picture or select an existing one.
     */
    public void showChooseExistingPictureOrTakeNewPhotoAlert() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_photo_select)
                .setMessage(R.string.do_you_want_to_choose_existing_img_or_take_a_new_one)
                .setPositiveButton(R.string.existing_image, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        pickExistingImage();
                    }
                }).setNegativeButton(R.string.take_photo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showTakePhotoActivity();
                    }
                }).create();
        dialog.show();
    }

    private void pickExistingImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO_REQUEST_CODE);
    }

    private void showTakePhotoActivity() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = ((RegattaActivity) getActivity()).getImageFile(CAMERA_TEMP_FILE);
        Uri file = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", photoFile);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            RegattaActivity activity = (RegattaActivity) getActivity();

            if (requestCode == CAMERA_REQUEST_CODE) {
                File photoFile = activity.getImageFile(CAMERA_TEMP_FILE);
                Bitmap photo;
                try {
                    photo = decodeUri(Uri.fromFile(photoFile));
                    activity.updateLeaderboardPictureChosenByUser(photo);
                } catch (FileNotFoundException e) {
                    if (BuildConfig.DEBUG) {
                        ExLog.i(getActivity(), TAG, "update photo, file not found: " + photoFile);
                    }
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        ExLog.i(getActivity(), TAG, "update photo, io exception: " + e.getMessage());
                    }
                } finally {
                    activity.deleteFile(CAMERA_TEMP_FILE);
                }
            } else if (requestCode == SELECT_PHOTO_REQUEST_CODE) {
                Uri selectedImage = data.getData();

                try {
                    Bitmap photo = decodeUri(selectedImage);
                    activity.updateLeaderboardPictureChosenByUser(photo);
                } catch (FileNotFoundException e) {
                    ExLog.e(getActivity(), TAG, "File not found exception after picking image from gallery");
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        ExLog.i(getActivity(), TAG, "update photo, io exception: " + e.getMessage());
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Decode image with maximum size
     *
     * @param uri
     * @return
     * @throws IOException
     */
    private Bitmap decodeUri(Uri uri) throws IOException {
        Bitmap bitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream fis = getActivity().getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(fis, null, options);
        fis.close();

        int scale = 1;
        if (options.outHeight > IMAGE_MAX_SIZE || options.outWidth > IMAGE_MAX_SIZE) {
            scale = (int) Math.pow(2, (int) Math.ceil(
                    Math.log(IMAGE_MAX_SIZE / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
        }

        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = scale;
        fis = getActivity().getContentResolver().openInputStream(uri);
        bitmap = BitmapFactory.decodeStream(fis, null, options2);
        fis.close();

        return bitmap;
    }

    private void startLeaderboardActivity() {
        RegattaActivity activity = (RegattaActivity) getActivity();

        Intent intent = new Intent(getActivity(), LeaderboardWebViewActivity.class);
        intent.putExtra(LeaderboardWebViewActivity.LEADERBOARD_EXTRA_SERVER_URL, activity.event.server);
        intent.putExtra(LeaderboardWebViewActivity.LEADERBOARD_EXTRA_EVENT_ID, activity.event.id);
        intent.putExtra(LeaderboardWebViewActivity.LEADERBOARD_EXTRA_LEADERBOARD_NAME, activity.leaderboard.name);

        startActivity(intent);
    }

    private void startEventActivity() {
        RegattaActivity activity = (RegattaActivity) getActivity();
        AppPreferences preferences = new AppPreferences(getActivity());
        EventInfo eventInfo = activity.event;
        String url = eventInfo.server + preferences.getServerEventUrl(eventInfo.id);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startTrackingActivity() {
        prefs.setTrackingTimerStarted(MillisecondsTimePoint.now().asMillis());
        RegattaActivity regattaActivity = (RegattaActivity) getActivity();
        Intent intent = new Intent(getActivity(), TrackingActivity.class);
        String checkinDigest = regattaActivity.event.checkinDigest;
        intent.putExtra(getString(R.string.tracking_activity_checkin_digest_parameter), checkinDigest);
        getActivity().startActivity(intent);
    }

    private void timerFired() {
        RegattaActivity regattaActivity = (RegattaActivity) getActivity();
        if (regattaActivity != null) {
            updateCountdownTimer(regattaActivity.event.startMillis);
        }
    }

    private void updateCountdownTimer(long startTime) {
        long diff = startTime - System.currentTimeMillis();
        if (diff < 0) // start of event is in the past
        {
            setCountdownTime(0, 0, 0);
        } else {
            int days = (int) TimeUnit.MILLISECONDS.toDays(diff);
            int hours = (int) TimeUnit.MILLISECONDS.toHours(diff) - (days * 24);
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(diff) - (hours * 60) - (days * 24 * 60);

            setCountdownTime(days, hours, minutes);
        }
    }

    private void setCountdownTime(int days, int hours, int minutes) {
        TextView daysTextView = getActivity().findViewById(R.id.starts_in_days);
        TextView hoursTextView = getActivity().findViewById(R.id.starts_in_hours);
        TextView minutesTextView = getActivity().findViewById(R.id.starts_in_minutes);

        daysTextView.setText(String.format("%02d", days, Locale.getDefault()));
        hoursTextView.setText(String.format("%02d", hours, Locale.getDefault()));
        minutesTextView.setText(String.format("%02d", minutes, Locale.getDefault()));
    }

    public void setFragmentWatcher(FragmentWatcher fWatcher) {
        watcher = fWatcher;
    }

    public interface FragmentWatcher {
        public void onViewCreated();
    }

    private class TimerRunnable implements Runnable {

        public Thread t;
        public volatile boolean running = true;

        public void start() {
            running = true;
            if (t == null) {
                t = new Thread(this);
                t.start();
            }
        }

        @Override
        public void run() {
            while (running) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerFired();
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    ExLog.w(RegattaFragment.this.getActivity(), TAG, "Interrupted sleep");
                }
            }
        }

        public void stop() {
            running = false;
        }

    }

}
