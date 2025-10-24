package com.sap.sailing.domain.abstractlog.impl;

import java.io.Serializable;
import java.util.Comparator;

import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;

/**
 * Comparator sorting by pass, then by {@link AbstractLogEventAuthor}, then by {@link RaceLogEvent#getCreatedAt()}
 * timestamp.
 * 
 * If one of the passed objects is not a {@link RaceLogEvent}, sorting is done by {@link Timed#getTimePoint()}.
 */
public class LogEventComparator implements Comparator<Timed>, Serializable {
    private static final long serialVersionUID = -1337219742246147546L;
    private Comparator<Timed> timedComparator;

    public LogEventComparator() {
        this.timedComparator = TimedComparator.INSTANCE;
    }

    @Override
    public int compare(Timed o1, Timed o2) {
        if (o1 instanceof AbstractLogEvent && o2 instanceof AbstractLogEvent) {
            return compareEvents((AbstractLogEvent<?>) o1, (AbstractLogEvent<?>) o2);
        }
        // fallback to timed comparison
        return timedComparator.compare(o1, o2);
    }
    
    protected int compareEvents(AbstractLogEvent<?> e1, AbstractLogEvent<?> e2) {
        // compare author priorities
        int result = e1.getAuthor().compareTo(e2.getAuthor());
        if (result == 0) {
            // compare created at timepoints
            result = e1.getCreatedAt().compareTo(e2.getCreatedAt());
            if (result == 0) {
                // compare logical timepoints
                result = Util.compareToWithNull(e1.getLogicalTimePoint(), e2.getLogicalTimePoint(), /* nullIsLess */ false);
                if (result == 0) {
                    // compare ids
                    result = e1.getId().toString().compareTo(e2.getId().toString());
                }
            }
        }
        return result;
    }
}
