package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment {

    public static final String TAG = DatePickerFragment.class.getSimpleName();
    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH = "month";
    private static final String KEY_DAY_OF_MONTH = "day_of_month";
    private static final String KEY_MIN_DATE = "min_date";
    private static final String KEY_MAX_DATE = "max_date";

    private DatePickerDialog.OnDateSetListener listener;

    public static DatePickerFragment newInstance(int year, int month, int dayOfMonth) {
        return newInstance(year, month, dayOfMonth, null, null);
    }

    public static DatePickerFragment newInstance(int year, int month, int dayOfMonth,
                                                 @Nullable Long minDate, @Nullable Long maxDate) {
        final DatePickerFragment fragment = new DatePickerFragment();
        final Bundle args = new Bundle();
        args.putInt(KEY_YEAR, year);
        args.putInt(KEY_MONTH, month);
        args.putInt(KEY_DAY_OF_MONTH, dayOfMonth);
        if (minDate != null) {
            args.putLong(KEY_MIN_DATE, minDate);
        }
        if (maxDate != null) {
            args.putLong(KEY_MAX_DATE, maxDate);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDateSetListener(DatePickerDialog.OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar calendar = Calendar.getInstance();

        final Bundle args = getArguments();
        if (args != null) {
            final int year = args.getInt(KEY_YEAR, calendar.get(Calendar.YEAR));
            final int month = args.getInt(KEY_MONTH, calendar.get(Calendar.MONTH));
            final int dayOfMonth = args.getInt(KEY_DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
            calendar.set(year, month, dayOfMonth);
        }

        //Use the current date as the default date in the picker
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        //Create a new instance of DatePickerDialog and return it
        final DatePickerDialog dialog = new DatePickerDialog(requireContext(), listener, year, month, dayOfMonth);

        //Consider min date
        if (args != null && args.containsKey(KEY_MIN_DATE)) {
            dialog.getDatePicker().setMinDate(args.getLong(KEY_MIN_DATE));
        }
        //Consider max date
        if (args != null && args.containsKey(KEY_MAX_DATE)) {
            dialog.getDatePicker().setMaxDate(args.getLong(KEY_MAX_DATE));
        }

        return dialog;
    }
}
