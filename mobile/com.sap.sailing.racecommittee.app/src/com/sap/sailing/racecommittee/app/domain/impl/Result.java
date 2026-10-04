package com.sap.sailing.racecommittee.app.domain.impl;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class Result {

    public final static int OK = 0;

    private int mErrorString;
    private Object[] mObjects;

    public Result() {
        this(OK);
    }

    public Result(@StringRes int stringRes) {
        setError(stringRes);
    }

    public void resetError() {
        mErrorString = OK;
        mObjects = null;
    }

    public void setError(@StringRes int stringRes) {
        resetError();

        if (stringRes != 0) {
            mErrorString = stringRes;
        }
    }

    public void setError(@StringRes int stringRes, @Nullable Object... objects) {
        setError(stringRes);
        mObjects = objects;
    }

    public boolean isOk() {
        return (mErrorString == OK);
    }

    public boolean hasError() {
        return (mErrorString != OK);
    }

    public @StringRes int getMessageId() {
        return mErrorString;
    }

    public String getMessage(Context context) {
        return context.getString(getMessageId(), mObjects);
    }
}
