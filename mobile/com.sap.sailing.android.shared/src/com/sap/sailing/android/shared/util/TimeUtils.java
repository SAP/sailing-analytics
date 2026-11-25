package com.sap.sailing.android.shared.util;

import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.sap.sailing.android.shared.R;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TimeUtils {

    protected TimeUtils() {
        // only static methods
    }

    /**
     * Formats your time to 'kk:mm:ss'.
     *
     * @param timePoint timestamp to format
     */
    public static String formatTime(TimePoint timePoint, boolean includeSeconds) {
        if (includeSeconds) {
            return formatTime(timePoint, "kk:mm:ss");
        } else {
            return formatTime(timePoint, "kk:mm");
        }
    }

    /**
     * Formats your time to 'kk:mm:ss'.
     *
     * @param timePoint timestamp to format
     */
    public static String formatTime(TimePoint timePoint) {
        return formatTime(timePoint, true);
    }

    /**
     * Formats your time with the help of {@link DateFormat}.
     *
     * @param timePoint timestamp to format
     * @param format    format as defined by {@link DateFormat}
     * @return timestamp formatted as {@link String}
     */
    public static String formatTime(TimePoint timePoint, String format) {
        return DateFormat.format(format, timePoint.asDate()).toString();
    }

    public static String formatDuration(TimePoint now, TimePoint timePoint) {
        return formatDuration(now, timePoint, true);
    }

    public static String formatDuration(TimePoint now, TimePoint timePoint, boolean inclSign) {
        String duration;
        if (timePoint.after(now)) {
            String sign = (inclSign) ? "-" : "";
            duration = sign + TimeUtils.formatDurationUntil(timePoint.minus(now.asMillis()).asMillis(), false);
        } else {
            duration = TimeUtils.formatDurationSince(now.minus(timePoint.asMillis()).asMillis(), false);
        }
        return duration;
    }

    public static String formatDurationSince(long milliseconds) {
        return formatDurationSince(milliseconds, false);
    }

    public static String formatDurationSince(long milliseconds, boolean emptyHours) {
        int secondsTillStart = (int) Math.floor(milliseconds / 1000d);
        return formatDuration(secondsTillStart, emptyHours);
    }

    public static String formatDurationUntil(long milliseconds) {
        return formatDurationUntil(milliseconds, true);
    }

    public static String formatDurationUntil(long milliseconds, boolean emptyHours) {
        int secondsTillStart = (int) Math.ceil(milliseconds / 1000d);
        return formatDuration(secondsTillStart, emptyHours);
    }

    /**
     * Formats milliseconds to a string like: 01h 23'45"
     */
    public static String formatTimeAgo(Context context, long milliseconds) {
        return formatDurationSince(milliseconds, false);
    }

    public static String calcDuration(Calendar from, Calendar to) {
        String retValue;

        long millis = to.getTimeInMillis() - from.getTimeInMillis();

        long min = millis / (1000 * 60);
        long sec = (millis - (min * 60 * 1000)) / 1000;

        retValue = String.valueOf(sec) + "\"";
        if (retValue.length() == 2) {
            retValue = "0" + retValue;
        }
        if (min > 0) {
            retValue = String.valueOf(min) + "' " + retValue;
        }

        return retValue;
    }

    public static Calendar floorTime(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public static int daysBetween(Calendar day1, Calendar day2) {
        Calendar dayOne = (Calendar) day1.clone();
        Calendar dayTwo = (Calendar) day2.clone();

        if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
            return dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR);
        } else {
            if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
                // swap them
                Calendar temp = dayOne;
                dayOne = dayTwo;
                dayTwo = temp;
            }
            int extraDays = 0;

            while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
                dayOne.add(Calendar.YEAR, -1);
                // getActualMaximum() important for leap years
                extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
            }

            return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOne.get(Calendar.DAY_OF_YEAR);
        }
    }

    private static String formatDuration(int secondsTillStart, boolean emptyHours) {
        int hours = secondsTillStart / 3600;
        int minutes = (secondsTillStart % 3600) / 60;
        int seconds = (secondsTillStart % 60);
        boolean negative = (hours < 0 || minutes < 0 || seconds < 0);
        String timePattern = ((negative) ? "-" : "") + "%s%s'%s\"";
        String secondsString = seconds < 10 ? "0" + Math.abs(seconds) : "" + Math.abs(seconds);
        String minutesString = minutes < 10 ? "0" + Math.abs(minutes) : "" + Math.abs(minutes);
        String hoursString = hours < 10 ? "0" + Math.abs(hours) : "" + Math.abs(hours);
        hoursString = !emptyHours && hoursString.equals("00") ? "" : hoursString + ":";
        return String.format(timePattern, hoursString, minutesString, secondsString);
    }

    public static void initDatePicker(Context context, NumberPicker datePicker, Calendar time, int pastDays,
                                      int futureDays) {
        initDatePicker(context, datePicker, time, pastDays, futureDays, true);
    }

    public static void initDatePicker(Context context, NumberPicker datePicker, Calendar time, int pastDays,
                                      int futureDays, boolean useWords) {
        java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
        ArrayList<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time.getTime());
        // Past
        calendar.add(Calendar.DAY_OF_MONTH, pastDays);
        for (int i = pastDays; i < -1; i++) {
            dates.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (useWords) {
            dates.add(context.getString(R.string.yesterday));
            dates.add(context.getString(R.string.today));
            dates.add(context.getString(R.string.tomorrow));
            calendar.add(Calendar.DAY_OF_MONTH, 3);
        } else {
            for (int i = (pastDays == 0 ? pastDays : -1); i < (futureDays < 2 ? futureDays + 1 : 2); i++) {
                dates.add(dateFormat.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        // Future
        for (int i = 2; i <= futureDays; i++) {
            dates.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        datePicker.setDisplayedValues(dates.toArray(new String[dates.size()]));
        datePicker.setMinValue(0);
        datePicker.setMaxValue(dates.size() - 1);
        datePicker.setWrapSelectorWheel(false);
        datePicker.setValue(TimeUtils.daysBetween(calendar, Calendar.getInstance()) + Math.abs(pastDays));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            TextView date = new TextView(context);
            date.setText(dateFormat.format(calendar.getTime()));
            int padding = context.getResources().getDimensionPixelSize(R.dimen.default_padding_half);
            date.setPadding(padding, padding, padding, padding);
            date.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            datePicker.getLayoutParams().width = date.getMeasuredWidth();
        }
    }

    public static ArrayList<String> getDates(Context context, TimePoint start, TimePoint end) {
        ArrayList<String> dates = new ArrayList<>();
        SimpleDateFormat simpleFormat = new SimpleDateFormat(context.getString(R.string.date_short), Locale.US);
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = (Calendar) startDate.clone();
        startDate.setTime(start.asDate());
        endDate.setTime(end.asDate());
        int dayDiff = daysBetween(endDate, startDate);
        for (int i = 0; i <= dayDiff; i++) {
            dates.add(simpleFormat.format(startDate.getTime()));
            startDate.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    public static TimePoint getTime(TimePicker timePicker) {
        return getTime(timePicker, null);
    }

    public static TimePoint getTime(TimePicker timePicker, @Nullable NumberPicker secondPicker) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
        time.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        time.set(Calendar.SECOND, secondPicker == null ? 0 : secondPicker.getValue());
        time.set(Calendar.MILLISECOND, 0);
        return new MillisecondsTimePoint(time.getTime());
    }

    public static TimePoint getTime(final int year, final int month, final int dayOfMonth, final TimePicker timePicker,
                                    @Nullable final NumberPicker secondPicker) {
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        final int second = secondPicker == null ? 0 : secondPicker.getValue();
        calendar.set(year, month, dayOfMonth, timePicker.getCurrentHour(), timePicker.getCurrentMinute(), second);
        return new MillisecondsTimePoint(calendar.getTime());
    }
}
