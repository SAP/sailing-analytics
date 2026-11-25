package com.sap.sailing.android.shared.ui.activities;

import com.sap.sailing.android.shared.logging.ExLog;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;

public abstract class AbstractBaseActivity extends SendingServiceAwareActivity {

    private static final String TAG = AbstractBaseActivity.class.getSimpleName();

    /**
     * An object used to synchronize access to the {@link #progressDialog} field to avoid concurrency issues during
     * re-assigning a new dialog.
     */
    private final Object progressDialogMonitor = new Object();

    private ProgressDialog progressDialog;

    public void replaceFragment(int view, Fragment fragment) {
        ExLog.i(this, TAG, "Set new Fragment: " + fragment.toString());

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(view, fragment);
        transaction.commit();
    }

    public void showProgressDialog(String title, String message) {
        synchronized (progressDialogMonitor) {
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        }
    }

    public void showProgressDialog(int string1Id, int string2Id) {
        showProgressDialog(getString(string1Id), getString(string2Id));
    }

    public void dismissProgressDialog() {
        synchronized (progressDialogMonitor) {
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (final IllegalArgumentException e) {
                ExLog.i(this, TAG, "progressDialog was not attached: " + e);
            } catch (final Exception e) {
                ExLog.i(this, TAG, "Unknown exception: " + e);
            } finally {
                progressDialog = null;
            }
        }
    }

    public void showErrorPopup(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public void showErrorPopup(int title, int message) {
        showErrorPopup(getString(title), getString(message));
    }
}
