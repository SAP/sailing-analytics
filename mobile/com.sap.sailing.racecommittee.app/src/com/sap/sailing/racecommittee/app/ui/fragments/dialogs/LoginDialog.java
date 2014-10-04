package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.impl.RaceLogEventAuthorImpl;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.android.shared.logging.ExLog;

public class LoginDialog extends ActivityAttachedDialogFragment {

    private static final LoginType DefaultLoginType = LoginType.NONE;
    public enum LoginType {
        OFFICER, VIEWER, NONE;
    }

    private CharSequence[] loginTypeDescriptions;
    private LoginType selectedLoginType;
    private RaceLogEventAuthor author;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginTypeDescriptions = new CharSequence[] { getString(R.string.login_type_officer_on_start_vessel),
                                                     getString(R.string.login_type_officer_on_finish_vessel),
                                                     getString(R.string.login_type_shore_control),
                                                     getString(R.string.login_type_viewer) };
        selectedLoginType = DefaultLoginType;
    }

    public LoginType getSelectedLoginType() {
        return selectedLoginType;
    }
    
    public RaceLogEventAuthor getAuthor() {
        return author;
    }

    @Override
    protected CharSequence getNegativeButtonLabel() {
        return getString(R.string.cancel);
    }

    @Override
    protected CharSequence getPositiveButtonLabel() {
        return getString(R.string.login);
    }

    @Override
    protected Builder createDialog(Builder builder) {
        return builder
                .setTitle(getString(R.string.login))
                .setIcon(R.drawable.ic_menu_login)
                .setSingleChoiceItems(loginTypeDescriptions, -1, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        // see loginTypeDescriptions for the indices of the login types
                        case 0:
                            selectedLoginType = LoginType.OFFICER;
                            author = new RaceLogEventAuthorImpl("Race Officer on Start Vessel", 0);
                            break;
                        case 1:
                            selectedLoginType = LoginType.OFFICER;
                            author = new RaceLogEventAuthorImpl("Race Officer on Finish Vessel", 1);
                            break;
                        case 2:
                            selectedLoginType = LoginType.OFFICER;
                            author = new RaceLogEventAuthorImpl("Shore Control", 2);
                            break;
                        case 3:
                            selectedLoginType = LoginType.VIEWER;
                            author = new RaceLogEventAuthorImpl("Viewer", 3);
                            break;
                        default:
                            selectedLoginType = LoginType.NONE;
                            break;
                        }
                    }
                });
    }

    @Override
    protected void onNegativeButton() {
        selectedLoginType = DefaultLoginType;
        ExLog.i(ExLog.LOGIN_BUTTON_NEGATIVE, String.valueOf(selectedLoginType), getActivity());
        super.onNegativeButton();
    }

    @Override
    protected void onPositiveButton() {
        ExLog.i(ExLog.LOGIN_BUTTON_POSITIVE, String.valueOf(selectedLoginType), getActivity());
        super.onPositiveButton();
    }

}
