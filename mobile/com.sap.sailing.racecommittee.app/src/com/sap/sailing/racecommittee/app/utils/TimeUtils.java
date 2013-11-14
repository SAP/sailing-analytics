package com.sap.sailing.racecommittee.app.utils;

import android.text.format.DateFormat;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;

public class TimeUtils {

    public static long timeUntil(TimePoint targetTime) {
        return targetTime.asMillis() - MillisecondsTimePoint.now().asMillis();
    }
    
    /**
     * Formats your time to 'kk:mm:ss'.
     * @param timePoint timestamp to format
     */
    public static String formatTime(TimePoint timePoint) {
        return formatTime(timePoint, "kk:mm:ss");
    }
    
    /**
     * Formats your time with the help of {@link DateFormat}.
     * @param timePoint timestamp to format
     * @param format format as defined by {@link DateFormat}
     * @return timestamp formatted as {@link String}
     */
    public static String formatTime(TimePoint timePoint, String format) {
        return DateFormat.format(format, timePoint.asDate()).toString();
    }

    public static String formatDuration(long milliseconds) {
        int secondsTillStart = (int) Math.ceil(milliseconds / 1000f);
        int hours = secondsTillStart / 3600;
        int minutes = (secondsTillStart % 3600) / 60;
        int seconds = (secondsTillStart % 60);
        String timePattern = "%s:%s:%s";
        String secondsString = seconds < 10 ? "0" + seconds : "" + seconds;
        String minutesString = minutes < 10 ? "0" + minutes : "" + minutes;
        String hoursString = hours < 10 ? "0" + hours : "" + hours;
        return String.format(timePattern, hoursString, minutesString, secondsString);
    }
}
