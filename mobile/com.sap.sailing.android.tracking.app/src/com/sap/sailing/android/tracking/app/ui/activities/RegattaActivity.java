package com.sap.sailing.android.tracking.app.ui.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.data.CheckinUrlInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.data.http.HttpGetRequest;
import com.sap.sailing.android.shared.data.http.HttpJsonPostRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.AbstractRegattaActivity;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperError;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperFailureListener;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperSuccessListener;
import com.sap.sailing.android.shared.util.UniqueDeviceUuid;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.fragments.RegattaFragment;
import com.sap.sailing.android.tracking.app.upload.UploadResponseHandler;
import com.sap.sailing.android.tracking.app.upload.UploadResult;
import com.sap.sailing.android.tracking.app.upload.UploadTeamImageTask;
import com.sap.sailing.android.tracking.app.utils.AboutHelper;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.CheckinManager;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.valueobjects.CheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorInfo;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import static com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants.URL_LEADERBOARD_NAME;
import static com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants.URL_LEADERBOARD_NAME_FOR_SECRET;
import static com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants.URL_SECRET;

public class RegattaActivity extends AbstractRegattaActivity<CheckinData>
        implements RegattaFragment.FragmentWatcher, UploadResponseHandler {

    private final static String TAG = RegattaActivity.class.getName();
    private final static String COMPETITOR_IMAGE_FILENAME_PREFIX = "competitor_";
    private final static String COMPETITOR_IMAGE_FOLDER = "pictures";
    private final static String FLAG_IMAGE_FILENAME_PREFIX = "flag_";

    public EventInfo event;
    public CompetitorInfo competitor;
    public LeaderboardInfo leaderboard;
    public CheckinUrlInfo checkinUrl;

    private boolean hasPicture;
    private String checkinDigest;
    private CheckinManager manager;
    private AppPreferences prefs;
    private File pictureFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new AppPreferences(this);
        Intent intent = getIntent();

        event = new EventInfo();
        competitor = new CompetitorInfo();
        leaderboard = new LeaderboardInfo();

        checkinDigest = intent.getStringExtra(getString(R.string.checkin_digest));

        checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(this, checkinDigest);
        manager = new CheckinManager(checkinUrl.urlString, this, false);

        competitor = DatabaseHelper.getInstance().getCompetitor(this, checkinDigest);
        event = DatabaseHelper.getInstance().getEventInfo(this, checkinDigest);
        leaderboard = DatabaseHelper.getInstance().getLeaderboard(this, checkinDigest);

        setContentView(R.layout.fragment_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
            getSupportActionBar().setTitle(leaderboard.displayName);
            getSupportActionBar().setSubtitle(event.name);
            ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
        }
        // FIXME bug 3823: a RegattaFragment must only be created if a competitor, not a mark, is being tracked
        RegattaFragment regattaFragment = new RegattaFragment();
        regattaFragment.setFragmentWatcher(this);
        replaceFragment(R.id.content_frame, regattaFragment);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (prefs.getTrackerIsTracking()) {
            String checkinDigest = prefs.getTrackerIsTrackingCheckinDigest();
            startTrackingActivity(checkinDigest);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_with_checkout, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem teamPhotoItem = menu.findItem(R.id.options_menu_add_team_image);
        if (hasPicture) {
            teamPhotoItem.setTitle(getString(R.string.options_replace_team_photo));
        } else {
            teamPhotoItem.setTitle(getString(R.string.options_add_team_photo));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.options_menu_settings) {
            ExLog.i(this, TAG, "Clicked SETTINGS.");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.options_menu_checkout) {
            ExLog.i(this, TAG, "Clicked CHECKOUT.");
            displayCheckoutConfirmationDialog();
            return true;
        } else if (id == R.id.options_menu_add_team_image) {
            ExLog.i(this, TAG, "Clicked ADD TEAM IMAGE");
            getRegattaFragment().showChooseExistingPictureOrTakeNewPhotoAlert();
            return true;
        } else if (id == R.id.options_menu_refresh) {
            manager.callServerAndGenerateCheckinData();
            return true;
        } else if (id == R.id.options_menu_info) {
            AboutHelper.showInfoActivity(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private RegattaFragment getRegattaFragment() {
        return (RegattaFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    private void userImageUpdated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hasPicture = true;
                LinearLayout addTeamPhotoTextView = (LinearLayout) findViewById(R.id.add_photo);
                addTeamPhotoTextView.setVisibility(View.INVISIBLE);
                getRegattaFragment().setChangePhotoButtonHidden(false);
            }
        });
    }

    @Override
    protected void onResume() {
        RegattaFragment regattaFragment = getRegattaFragment();
        if (regattaFragment != null) {
            regattaFragment.setFragmentWatcher(this);
        }
        super.onResume();
    }

    private void setUpView() {
        TextView competitorNameTextView = (TextView) findViewById(R.id.competitor_name);
        competitorNameTextView.setText(competitor.name);

        TextView sailIdTextView = (TextView) findViewById(R.id.sail_id);
        sailIdTextView.setText(competitor.sailId);

        getRegattaFragment().setChangePhotoButtonHidden(true);

        final ImageView imageView = (ImageView) findViewById(R.id.userImage);
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                int measuredWidth = imageView.getMeasuredWidth();
                int measuredHeight = imageView.getMeasuredHeight();
                setTeamImage(imageView, measuredWidth, measuredHeight);
                return true;
            }
        });

        final ImageView flagImageView = (ImageView) findViewById(R.id.flag_image);
        flagImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                flagImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                int measuredWidth = flagImageView.getMeasuredWidth();
                int measuredHeight = flagImageView.getMeasuredHeight();
                setFlagImage(flagImageView, measuredWidth, measuredHeight);
                return true;
            }
        });
    }

    private void setTeamImage(ImageView imageView, int width, int height) {
        if (competitor != null && competitor.id != null) {
            String fileName = getCompetitorImageFileName(competitor.id);
            Bitmap storedImage = getStoredImage(fileName, width, height);
            if (storedImage == null) {
                askServerAboutTeamImageUrl(imageView);
            } else {
                imageView.setImageBitmap(storedImage);
                userImageUpdated();
            }
        }
    }

    private void setFlagImage(ImageView imageView, int width, int height) {
        if (competitor != null && competitor.countryCode != null) {
            String flagFileName = getFlagImageFileName(competitor.countryCode.toLowerCase(Locale.getDefault()));
            Bitmap storedFlagImage = getStoredImage(flagFileName, width, height);
            if (storedFlagImage == null) {
                String urlStr = String.format("%s/gwt/images/flags/%s.png", event.server,
                        competitor.countryCode.toLowerCase(Locale.getDefault()));
                new DownloadFlagImageTask(imageView, competitor.countryCode).execute(urlStr);
            } else {
                imageView.setImageBitmap(storedFlagImage);
            }
        }
    }

    private URL getTeamImageApiUrl(String competitorId) throws MalformedURLException {
        Uri uri = Uri.parse(checkinUrl.urlString);
        Uri.Builder builder = new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(uri.getAuthority())
                .appendEncodedPath(prefs.getServerCompetitorTeamPath(competitorId));
        String secret = uri.getQueryParameter(URL_SECRET);
        if (secret != null) {
            builder.appendQueryParameter(URL_SECRET, secret);
            builder.appendQueryParameter(URL_LEADERBOARD_NAME_FOR_SECRET, uri.getQueryParameter(URL_LEADERBOARD_NAME));
        }
        return new URL(builder.build().toString());
    }

    public void askServerAboutTeamImageUrl(final ImageView imageView) {
        try {
            HttpGetRequest getCompetitorTeamRequest = new HttpGetRequest(getTeamImageApiUrl(competitor.id), this);
            NetworkHelper.getInstance(this).executeHttpJsonRequestAsync(getCompetitorTeamRequest,
                    new NetworkHelperSuccessListener() {
                        @Override
                        public void performAction(JSONObject response) {
                            try {
                                String teamImageUri = response.getString("imageUri");
                                if (teamImageUri != null) {
                                    new DownloadLeaderboardImageTask(imageView)
                                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, teamImageUri);
                                }
                            } catch (JSONException e) {
                                ExLog.e(getApplicationContext(), TAG,
                                        "Error: Failed to get teamImageURL: " + e.getMessage());
                            }
                        }
                    }, new NetworkHelperFailureListener() {
                        @Override
                        public void performAction(NetworkHelperError e) {
                            ExLog.e(getApplicationContext(), TAG,
                                    "Error: Failed to get teamImageURL: " + e.getMessage());
                        }
                    });

        } catch (MalformedURLException e) {
            ExLog.e(this, TAG, "Error: Failed to perform checking due to a MalformedURLException: " + e.getMessage());
        }
    }

    /**
     * @param bitmap
     */
    public void updateLeaderboardPictureChosenByUser(final Bitmap bitmap) {
        storeImageAndSendToServer(bitmap, getCompetitorImageFileName(competitor.id), true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) findViewById(R.id.userImage);
                imageView.setImageBitmap(bitmap);
            }
        });
        userImageUpdated();
    }

    /**
     * Store image for quicker retrieval later and trigger upload to server.
     *
     * @param image
     */
    private void storeImageAndSendToServer(Bitmap image, String fileName, boolean sendToServer) {
        pictureFile = getImageFile(fileName);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            if (sendToServer) {
                sendTeamImageToServer(pictureFile);
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Get stored image if there's one saved.
     *
     * @return
     */
    private Bitmap getStoredImage(String fileName, int width, int height) {
        File pictureFile = getImageFile(fileName);
        if (pictureFile == null || !pictureFile.exists()) {
            return null;
        }
        Bitmap.Config preferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapHelper.decodeSampleBitmapFromFile(pictureFile.getAbsolutePath(), width, height, preferredConfig);
    }

    /**
     * Silently send the team image to the server.
     *
     * @param imageFile
     */
    private void sendTeamImageToServer(File imageFile) {
        if (BuildConfig.DEBUG) {
            ExLog.i(this, TAG, "Sending imageFile to server: " + imageFile);
        }
        try {
            Uri uri = Uri.parse(checkinUrl.urlString);
            Uri.Builder builder = new Uri.Builder()
                    .scheme(uri.getScheme())
                    .authority(uri.getAuthority())
                    .appendEncodedPath(prefs.getServerUploadTeamImagePath(competitor.id));
            String secret = uri.getQueryParameter(URL_SECRET);
            if (secret != null) {
                builder.appendQueryParameter(URL_SECRET, secret);
                builder.appendQueryParameter(URL_LEADERBOARD_NAME_FOR_SECRET, uri.getQueryParameter(URL_LEADERBOARD_NAME));
            }
            new UploadTeamImageTask(this, imageFile, this).execute(builder.build().toString());
        } catch (UnsupportedEncodingException e) {
            ExLog.e(this, TAG, "Failed to encode competitor ID: " + e.getMessage());
        }
    }

    /**
     * Get Path for cached leaderbaord image.
     *
     * @return
     */
    public File getImageFile(String fileName) {
        File mediaStorageDir = getMediaStorageDir();

        String mImageName = fileName + ".png";
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    public boolean deleteImageFile(String fileName) {
        File mediaFile = getImageFile(fileName);
        return mediaFile.delete();
    }

    public File getMediaStorageDir() {
        File mediaStorageDir;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mediaStorageDir = new File(getExternalFilesDir(null), COMPETITOR_IMAGE_FOLDER);
        } else {
            mediaStorageDir = getApplicationContext().getCacheDir();
        }

        if (mediaStorageDir != null && !mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return mediaStorageDir;
    }

    @Override
    public void onCheckinDataAvailable(CheckinData checkinData) {
        if (checkinData != null) {
            try {
                if (checkinData instanceof CompetitorCheckinData) {
                    CompetitorCheckinData competitorCheckinData = (CompetitorCheckinData) checkinData;
                    DatabaseHelper.getInstance().deleteRegattaFromDatabase(this, checkinDigest);
                    DatabaseHelper.getInstance().storeCompetitorCheckinRow(this, competitorCheckinData);
                    competitor = DatabaseHelper.getInstance().getCompetitor(this, checkinDigest);
                    event = DatabaseHelper.getInstance().getEventInfo(this, checkinDigest);
                    leaderboard = DatabaseHelper.getInstance().getLeaderboard(this, checkinDigest);
                    checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(this, checkinDigest);
                    RegattaFragment regattaFragment = new RegattaFragment();
                    regattaFragment.setFragmentWatcher(this);
                    replaceFragment(R.id.content_frame, regattaFragment);
                }
                // FIXME bug3823: what about MarkCheckinData? Which fragment shall be shown?
            } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
                ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
                displayDatabaseError();
            }
        }
    }

    @Override
    public void onViewCreated() {
        setUpView();
    }

    private String getCompetitorImageFileName(String competitorId) {
        return COMPETITOR_IMAGE_FILENAME_PREFIX + competitorId;
    }

    private String getFlagImageFileName(String countryCode) {
        return FLAG_IMAGE_FILENAME_PREFIX + countryCode;
    }

    private void displayCheckoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setMessage(R.string.checkout_warning_message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkout();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    /**
     * Check out from regatta;
     */
    public void checkout() {
        final String checkoutURLStr = event.server
                + prefs.getServerCheckoutPath().replace("{leaderboard-name}", Uri.encode(leaderboard.name));
        showProgressDialog(R.string.please_wait, R.string.checking_out);
        JSONObject checkoutData = new JSONObject();
        try {
            String secret = Uri.parse(checkinUrl.urlString).getQueryParameter(DeviceMappingConstants.URL_SECRET);
            checkoutData.putOpt(DeviceMappingConstants.JSON_REGISTER_SECRET, secret);
            checkoutData.put(DeviceMappingConstants.JSON_COMPETITOR_ID_AS_STRING, competitor.id);
            checkoutData.put(DeviceMappingConstants.JSON_DEVICE_UUID, UniqueDeviceUuid.getUniqueId(this));
            checkoutData.put(DeviceMappingConstants.JSON_TO_MILLIS, System.currentTimeMillis());
        } catch (JSONException e) {
            showErrorPopup(R.string.error, R.string.error_could_not_complete_operation_on_server_try_again);
            ExLog.e(this, TAG, "Error populating checkout-data: " + e.getMessage());
            return;
        }
        try {
            HttpJsonPostRequest request = new HttpJsonPostRequest(this, new URL(checkoutURLStr),
                    checkoutData.toString());
            NetworkHelper.getInstance(this).executeHttpJsonRequestAsync(request, new NetworkHelperSuccessListener() {
                @Override
                public void performAction(JSONObject response) {
                    DatabaseHelper.getInstance().deleteRegattaFromDatabase(RegattaActivity.this, event.checkinDigest);
                    deleteImageFile(getCompetitorImageFileName(competitor.id));
                    dismissProgressDialog();
                    finish();
                }
            }, new NetworkHelperFailureListener() {
                @Override
                public void performAction(NetworkHelperError e) {
                    dismissProgressDialog();
                    showErrorPopup(R.string.error, R.string.error_could_not_complete_operation_on_server_try_again);
                }
            });
        } catch (MalformedURLException e) {
            ExLog.w(this, TAG, "Error, can't check out, MalformedURLException: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void startTrackingActivity(String checkinDigest) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra(getString(R.string.tracking_activity_checkin_digest_parameter), checkinDigest);
        startActivity(intent);
    }

    @Override
    protected int getOptionsMenuResId() {
        return R.menu.options_menu_with_checkout;
    }

    @Override
    public void onUploadTaskStarted() {
        hideRetryUploadLayout();
        showUploadProgressLayout();
    }

    @Override
    public void onUploadCancelled() {
        Toast.makeText(this, getString(R.string.error_sending_team_image_to_server), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUploadTaskFinished(UploadResult uploadResult) {
        hideUploadProgressLayout();
        if (uploadResult.resultCode != HttpURLConnection.HTTP_OK) {
            prefs.setFailedUpload(leaderboard.name);
            showRetryUploadLayout();
            Toast.makeText(this, uploadResult.resultCode + ": " + uploadResult.resultMessage, Toast.LENGTH_LONG).show();
        } else {
            prefs.removeFailedUpload(leaderboard.name);
        }
    }

    public void retryUpload(View view) {
        if (prefs.hasFailedUpload(leaderboard.name)) {
            pictureFile = getImageFile(getCompetitorImageFileName(competitor.id));
        }
        if (pictureFile != null) {
            sendTeamImageToServer(pictureFile);
        } else {
            hideRetryUploadLayout();
        }
    }

    public void showUploadProgressLayout() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.upload_progress);
        if (layout != null) {
            layout.setVisibility(View.VISIBLE);
        }
    }

    public void hideUploadProgressLayout() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.upload_progress);
        if (layout != null) {
            layout.setVisibility(View.GONE);
        }
    }

    public void showRetryUploadLayout() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.retry_upload);
        if (layout != null) {
            layout.setVisibility(View.VISIBLE);
        }
    }

    public void hideRetryUploadLayout() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.retry_upload);
        if (layout != null) {
            layout.setVisibility(View.GONE);
        }
    }

    private class DownloadLeaderboardImageTask extends AsyncTask<String, Void, File> {
        ImageView bmImage;
        String downloadUrl;
        ProgressDialog dialog;

        public DownloadLeaderboardImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(RegattaActivity.this);
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.download_team_image_message));
            dialog.show();
        }

        protected File doInBackground(String... urls) {
            downloadUrl = urls[0];
            File imageFile = null;
            InputStream in = null;
            FileOutputStream outputStream = null;
            try {
                in = new java.net.URL(downloadUrl).openStream();
                imageFile = getImageFile(getCompetitorImageFileName(competitor.id));
                if (!imageFile.exists()) {
                    imageFile.createNewFile();
                }
                outputStream = new FileOutputStream(imageFile);
                int read;
                byte[] bytes = new byte[1024];

                while ((read = in.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.close();
                in.close();
            } catch (Exception e) {
                ExLog.e(RegattaActivity.this, TAG, "Failed to download image file " + imageFile);
            } finally {
                if (in != null) {
                    safeClose(in);
                }
                if (outputStream != null) {
                    safeClose(outputStream);
                }
            }
            return imageFile;
        }

        protected void onPostExecute(File result) {
            if (result != null && result.exists()) {
                bmImage.setImageBitmap(BitmapHelper.decodeSampleBitmapFromFile(result.getPath(),
                        bmImage.getMeasuredWidth(), bmImage.getMeasuredHeight(), null));
                userImageUpdated();
            } else {
                ExLog.e(RegattaActivity.this, TAG, "Failed to download leaderboard image at url " + downloadUrl);
            }
            dialog.cancel();
        }
    }

    private class DownloadFlagImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        String countryCode;
        String downloadUrl;

        public DownloadFlagImageTask(ImageView bmImage, String countryCode) {
            this.bmImage = bmImage;
            this.countryCode = countryCode;
        }

        protected Bitmap doInBackground(String... urls) {
            downloadUrl = urls[0];
            Bitmap mIcon11 = null;

            System.out.println("URL DISPLAY: " + downloadUrl);

            try {
                InputStream in = new java.net.URL(downloadUrl).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                ExLog.e(RegattaActivity.this, TAG,
                        "Failed to download flat image at url " + downloadUrl + ": " + e.getMessage());
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            System.out.println("RESULT: " + result);
            if (result != null) {
                bmImage.setImageBitmap(result);
                storeImageAndSendToServer(result, getFlagImageFileName(countryCode), false);
            } else {
                ExLog.e(RegattaActivity.this, TAG, "Failed to download flag image at url " + downloadUrl);
            }
        }
    }
}
