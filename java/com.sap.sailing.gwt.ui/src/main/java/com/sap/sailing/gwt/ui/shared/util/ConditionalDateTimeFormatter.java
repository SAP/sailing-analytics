package com.sap.sailing.gwt.ui.shared.util;

import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.hoursAndMinutesTimeFormatterUTC;
import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.minutesTimeFormatterUTC;

import java.util.Date;

import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class ConditionalDateTimeFormatter {

    public static String format(Date newsTimestamp, Date currentTimestamp, StringMessages stringMessages) {
        final String result;
        if (lessThanOneMinuteAgo(currentTimestamp, newsTimestamp)) {
            result = stringMessages.now();
        } else if (lessThanOneHourAgo(currentTimestamp, newsTimestamp)) {
            Date timespanBetween = timespanBetween(currentTimestamp, newsTimestamp);
            result = stringMessages.minutesAgo(minutesTimeFormatterUTC.render(timespanBetween));
        } else if (sameDay(currentTimestamp, newsTimestamp)) {
            Date timespanBetween = timespanBetween(currentTimestamp, newsTimestamp);
            result = stringMessages.hoursAgo(hoursAndMinutesTimeFormatterUTC.render(timespanBetween));
        } else {
            result = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT).format(newsTimestamp);
        }
        return result;
    }

    private static boolean lessThanOneMinuteAgo(Date currentTimestamp, Date newsTimestamp) {
        return newsTimestamp.after(new Date(currentTimestamp.getTime() - 60_000));
    }

    private static boolean lessThanOneHourAgo(Date currentTimestamp, Date newsTimestamp) {
        return newsTimestamp.after(new Date(currentTimestamp.getTime() - 3_600_000));
    }

    private static Date timespanBetween(Date currentTimestamp, Date newsTimestamp) {
        return new Date(currentTimestamp.getTime() - newsTimestamp.getTime());
    }

    private static boolean sameDay(Date currentTimestamp, Date newsTimestamp) {
        return CalendarUtil.isSameDate(currentTimestamp, newsTimestamp);
    }
}
