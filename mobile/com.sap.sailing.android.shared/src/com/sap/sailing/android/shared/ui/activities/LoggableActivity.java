package com.sap.sailing.android.shared.ui.activities;

import java.io.Closeable;
import java.io.IOException;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.logging.LifecycleLogger;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public abstract class LoggableActivity extends AppCompatActivity {
    private static final String TAG = LoggableActivity.class.getName();

    private LifecycleLogger lifeLogger;

    public LoggableActivity() {
        this.lifeLogger = new LifecycleLogger();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifeLogger.onCreate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        lifeLogger.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lifeLogger.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        lifeLogger.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        lifeLogger.onStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lifeLogger.onDestroy(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ExLog.i(this, TAG, String.format("Back pressed on activity %s", this.getClass().getSimpleName()));
    }

    public void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                ExLog.ex(this, TAG, e);
            }
        }
    }
}
