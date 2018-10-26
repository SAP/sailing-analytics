package com.sap.sailing.android.shared.ui.activities;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.util.CheckinDataHandler;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public abstract class CheckinDataActivity<C extends BaseCheckinData> extends AbstractBaseActivity
        implements CheckinDataHandler<C> {

    /**
     * Shows a pop-up-dialog that informs the user than an DB-operation has failed.
     */
    public void displayDatabaseError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog);
        builder.setMessage(getString(R.string.notify_user_db_operation_failed));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder.show();
    }
}
