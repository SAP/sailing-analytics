package com.sap.sailing.racecommittee.app.utils.autoupdate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.racecommittee.app.R;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class AutoUpdaterChecker {

    public static final String TAG = AutoUpdaterChecker.class.getName();

    public interface AutoUpdaterState {
        void onApkDownloadProgress(Float progress);

        void updateToVersion(int version, String apkFileName);

        void onApkDownloadFinished(File result);

        void onError();
    }

    private final Context context;
    private final ProgressDialog dialog;
    private final AutoUpdater updater;
    private final AutoUpdaterStateImpl state;

    public AutoUpdaterChecker(Context context, AutoUpdater updater, boolean forceUpdate) {
        this.context = context;
        this.updater = updater;
        this.state = new AutoUpdaterStateImpl(forceUpdate);

        this.dialog = new ProgressDialog(context);
        dialog.setTitle(context.getString(R.string.auto_update));
        dialog.setCancelable(true);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        dialog.cancel();
                    }
                });
    }

    public void check(URL serverUrl) {
        state.start(serverUrl);
    }

    private class AutoUpdaterStateImpl implements AutoUpdaterState {

        private final boolean forceUpdate;
        private URL serverUrl;

        public AutoUpdaterStateImpl(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        public void start(URL serverUrl) {
            this.serverUrl = serverUrl;

            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(true);
            dialog.setMessage(context.getString(R.string.auto_update_checking_version));
            dialog.show();

            try {
                URL versionUrl = composeVersionUrl();
                ExLog.i(context, TAG,
                        context.getString(R.string.auto_update_downloading_version, versionUrl.toString()));

                final AutoUpdaterVersionDownloader downloader = new AutoUpdaterVersionDownloader(this, context);
                downloader.execute(versionUrl);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloader.cancel(true);
                    }
                });
            } catch (MalformedURLException e) {
                onError();
                Toast.makeText(context, R.string.auto_update_version_file_url_invalid, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void updateToVersion(int serverVersion, final String apkFileName) {
            int currentVersion = AppUtils.with(context).getPackageInfo().versionCode;
            boolean needsUpdate = currentVersion != serverVersion;
            ExLog.i(context, TAG,
                    String.format("Server version is %d. Local version is %d.", serverVersion, currentVersion));

            DialogInterface.OnClickListener dismissListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface prompt, int which) {
                    dialog.cancel();
                    prompt.dismiss();
                }
            };
            DialogInterface.OnCancelListener cancelListener = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface prompt) {
                    dialog.cancel();
                }
            };
            DialogInterface.OnClickListener updateListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface prompt, int which) {
                    downloadUpdate(apkFileName);
                    prompt.dismiss();
                }
            };

            String messageFormat = needsUpdate ? context.getString(R.string.auto_update_click_install)
                    : context.getString(R.string.auto_update_force_install);
            if (!AppUtils.with(context).isSideLoaded()) {
                messageFormat += "\n\n"
                        + context.getString(R.string.auto_update_with_store, AppUtils.with(context).getStoreName());
            }

            AlertDialog.Builder updateDialog = new AlertDialog.Builder(context);
            updateDialog.setTitle(R.string.auto_update).setMessage(String.format(messageFormat, serverVersion))
                    .setPositiveButton(
                            context.getString(needsUpdate ? R.string.auto_update_install : android.R.string.ok),
                            needsUpdate ? updateListener : dismissListener)
                    .setNegativeButton(
                            context.getString(
                                    needsUpdate ? android.R.string.cancel : R.string.auto_update_install_anyway),
                            needsUpdate ? dismissListener : updateListener)
                    .setOnCancelListener(cancelListener);
            if (needsUpdate || forceUpdate) {
                updateDialog.show();
            } else {
                dialog.dismiss();
                Toast.makeText(context, R.string.auto_update_already_up_to_date, Toast.LENGTH_LONG).show();
            }
        }

        private void downloadUpdate(String apkFileName) {
            dialog.setMessage(context.getString(R.string.auto_update_downloading_apk));

            try {
                File target = updater.createApkTargetFile();

                final AutoUpdaterApkDownloader downloader = new AutoUpdaterApkDownloader(this, target, context);

                URL downloadUrl = composeDownloadUrl(apkFileName);
                ExLog.i(context, TAG, String.format("Download from %s to file %s.", downloadUrl.toString(),
                        target.getAbsolutePath()));
                downloader.execute(downloadUrl);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloader.cancel(true);
                    }
                });
            } catch (MalformedURLException e) {
                onError();
                Toast.makeText(context, R.string.auto_update_apk_link_invalid, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                onError();
                Toast.makeText(context, R.string.auto_update_apk_file_error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onApkDownloadProgress(Float progress) {
            if (dialog.isIndeterminate()) {
                dialog.setIndeterminate(false);
            }
            dialog.setProgress((int) (progress * 100));
        }

        @Override
        public void onApkDownloadFinished(File result) {
            dialog.dismiss();
            updater.updateFromFile(result);
        }

        @Override
        public void onError() {
            dialog.dismiss();
            AlertDialog.Builder errorDialog = new AlertDialog.Builder(context);
            errorDialog.setTitle(R.string.auto_update).setMessage(R.string.auto_update_error)
                    .setPositiveButton(android.R.string.ok, null).show();
        }

        protected URL composeVersionUrl() throws MalformedURLException {
            String packageName = context.getPackageName();
            return new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(),
                    serverUrl.getPath() + "/apps/" + packageName + ".version");
        }

        protected URL composeDownloadUrl(String apkFileName) throws MalformedURLException {
            return new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(),
                    serverUrl.getPath() + "/apps/" + apkFileName);
        }
    }
}
