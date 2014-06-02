package com.sap.sse.gwt.client.player;

import java.util.Date;

import com.sap.sse.common.Util.Pair;

public interface TimeRangeProvider {

    public void addTimeRangeChangeListener(TimeRangeChangeListener listener);

    public void removeTimeRangeChangeListener(TimeRangeChangeListener listener);

    public void setTimeRange(Date fromTime, Date toTime, TimeRangeChangeListener... listenersNotToNotify);
    
    public Date getFromTime();

    public Date getToTime();

    public Pair<Date, Date> getTimeRange();
}
