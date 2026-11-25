package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import java.util.Locale;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import android.view.View;
import android.widget.NumberPicker;

public class FullTimePickerDialog extends AlertDialog {

    private NumberPicker mHour;
    private NumberPicker mMinute;
    private NumberPicker mSecond;

    public FullTimePickerDialog(Context context, int res, @NonNull final OnTimeSetListener listener, int hourOfDay,
            int minute, int second, boolean is24Hour) {
        super(context, resolveDialogTheme(res));

        View view = getLayoutInflater().inflate(R.layout.time_picker_hms, null);
        mHour = ViewHelper.get(view, R.id.picker_hour);
        mMinute = ViewHelper.get(view, R.id.picker_minute);
        mSecond = ViewHelper.get(view, R.id.picker_second);
        initPicker(mHour, 0, (is24Hour) ? 23 : 12, (!is24Hour && hourOfDay > 12) ? hourOfDay - 12 : hourOfDay);
        initPicker(mMinute, 0, 59, minute);
        initPicker(mSecond, 0, 59, second);
        updateTitle();

        int margin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        setView(view, margin, 0, margin, 0);
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onTimeSet(FullTimePickerDialog.this, mHour.getValue(), mMinute.getValue(), mSecond.getValue());
                dismiss();
            }
        });
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
    }

    private static int resolveDialogTheme(@StyleRes int resId) {
        if (resId == 0) {
            resId = R.style.AppTheme_PickerDialog;
        }
        return resId;
    }

    private void initPicker(NumberPicker picker, int start, int end, int initial) {
        ThemeHelper.setPickerColor(getContext(), picker, ThemeHelper.getColor(getContext(), R.attr.white),
                ThemeHelper.getColor(getContext(), R.attr.sap_blue_1));

        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateTitle();
            }
        });
        picker.setMinValue(start);
        picker.setMaxValue(end);
        picker.setValue(initial);
        picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.getDefault(), "%02d", value);
            }
        });
    }

    private void updateTitle() {
        setTitle(String.format(Locale.getDefault(), "%02d:%02d:%02d", mHour.getValue(), mMinute.getValue(),
                mSecond.getValue()));
    }

    public interface OnTimeSetListener {
        void onTimeSet(FullTimePickerDialog dialog, int hourOfDay, int minutes, int second);
    }

}
