package com.sap.sailing.server.util;

import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.impl.StrippedEventImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class EventUtilTest {

    @Test
    public void testGetYearOfEvent() {
        EventBase event = getEventForDate(2017, 6, 27);
        Assertions.assertEquals(Integer.valueOf(2017), EventUtil.getYearOfEvent(event));
    }

    private EventBase getEventForDate(int year, int month, int date) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, date);
        MillisecondsTimePoint startEnd = new MillisecondsTimePoint(calendar.getTime());
        return new StrippedEventImpl("Test", startEnd, startEnd, "Anywhere", true, UUID.randomUUID(), 
                /** leaderBoardGroups **/ null, /** trackingConnectorInfos **/ Collections.emptySet());
    }
}
