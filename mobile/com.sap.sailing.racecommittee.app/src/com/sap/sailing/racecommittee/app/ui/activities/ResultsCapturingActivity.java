package com.sap.sailing.racecommittee.app.ui.activities;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.util.FileHandlerUtils;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.PhotoAdapter;
import com.sap.sailing.racecommittee.app.ui.views.CameraView;
import com.sap.sailing.racecommittee.app.utils.MailHelper;

// Deprecation of Camera API.
// New Camera API in Android 5.0
// New handling for camera in new RC App.
@SuppressWarnings("deprecation")
public class ResultsCapturingActivity extends SessionActivity {
    private static String ARGUMENTS_KEY_SUBJECT = "subject";
    private static String ARGUMENTS_KEY_TEXT = "text";

    private Camera camera; 
    private CameraView cameraView;

    private int currentImageIndex;
    private File currentImageFile;
    private PhotoAdapter photoList;

    private EditText subjectEditText;
    private EditText bodyEditText;

    public static Intent createIntent(Context context, String mailSubject, String mailBody) {
        Intent intent = new Intent();
        intent.setClass(context, ResultsCapturingActivity.class);
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENTS_KEY_SUBJECT, mailSubject);
        arguments.putString(ARGUMENTS_KEY_TEXT, mailBody);
        intent.putExtras(arguments);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.results_capturing_view);
        setupActionBar();

        currentImageIndex = 0;
        createAndAdvanceImageFile();

        setupListView();
        setupCameraView();

        Button footer = (Button) findViewById(R.id.results_capturing_view_button_add_photo);
        footer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.snap(pictureHandler);
            }
        });

        String subjectValue = getString(R.string.scores);
        String bodyValue = getString(R.string.no_text);
        Bundle arguments = getIntent().getExtras();
        if (arguments != null) {
            if (arguments.containsKey(ARGUMENTS_KEY_SUBJECT)) {
                subjectValue = arguments.getString(ARGUMENTS_KEY_SUBJECT);
            }
            if (arguments.containsKey(ARGUMENTS_KEY_TEXT)) {
                bodyValue = arguments.getString(ARGUMENTS_KEY_TEXT);
            }
        }

        subjectEditText = (EditText) findViewById(R.id.results_capturing_view_text_subject);
        subjectEditText.setText(subjectValue);

        bodyEditText = (EditText) findViewById(R.id.results_capturing_view_text_body);
        bodyEditText.setText(bodyValue);

        Button sendButton = (Button) findViewById(R.id.results_capturing_view_button_send);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
				String recipient = AppPreferences.on(ResultsCapturingActivity.this).getMailRecipient();
                MailHelper.send(new String[] { recipient }, getSubjectText(), getBodyText(), photoList.getItems(),
                        ResultsCapturingActivity.this);
                finish();
            }
        });
    }

    private void setupCameraView() {
        cameraView = new CameraView(this);
        FrameLayout view = (FrameLayout) findViewById(R.id.results_capturing_view_camera_preview);
        view.addView(cameraView);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        setupCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    private void setupCamera() {
        int cameraId = getBackCameraId();
        if (cameraId >= 0) {
            camera = Camera.open(cameraId);
            cameraView.setCamera(camera);
        }
    }
    
    private int getBackCameraId() {
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return -1;
    }

    private void releaseCamera() {
        if (camera != null) {
            cameraView.setCamera(null);
            camera.release();
            camera = null;
        }
    }

    private String getSubjectText() {
        return subjectEditText.getText().toString();
    }

    private String getBodyText() {
        return bodyEditText.getText().toString();
    }

    private void createAndAdvanceImageFile() {
        currentImageFile = createFinisherImageFile(currentImageIndex++, this);
    }

    private void setupListView() {
        TextView header = new TextView(this);
        header.setText(getString(R.string.results_capturing_view_list_header));
        header.setTextSize(TypedValue.COMPLEX_UNIT_PT, 10.0f);

        ListView listView = (ListView) findViewById(R.id.results_capturing_view_list);
        listView.setEmptyView(findViewById(R.id.results_capturing_view_list_empty));
        listView.addHeaderView(header);
        photoList = new PhotoAdapter(this);
        listView.setAdapter(photoList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.results_capturing_title));
        }
    }

    private static File createFinisherImageFile(int index, Context context) {
        return new File(FileHandlerUtils.getExternalApplicationFolder(context), String.format("image_%d.jpg", index));
    }

    private PictureCallback pictureHandler = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(currentImageFile);
                fos.write(data);
                fos.close();
                photoList.add(Uri.fromFile(currentImageFile));
                createAndAdvanceImageFile();
            } catch (Exception e) {
                String toastText = getString(R.string.error_picture_callback);
                Toast.makeText(ResultsCapturingActivity.this, toastText, Toast.LENGTH_LONG).show();
            } finally {
                if (fos != null) {
                    safeClose(fos);
                }
            }
        }
    };
}
