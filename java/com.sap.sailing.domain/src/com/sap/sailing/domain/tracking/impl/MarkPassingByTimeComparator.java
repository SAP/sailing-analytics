package com.sap.sailing.domain.tracking.impl;

import java.io.Serializable;
import java.util.Comparator;

import com.sap.sailing.domain.tracking.MarkPassing;

/**
 * Compares two {@link MarkPassing} objects, using their {@link MarkPassing#getTimePoint() time point} as the primary
 * ordering criterion in ascending order. If two mark passings have equal time points, the competitor IDs' string
 * representations are compared as the secondary ordering criterion to ensure that two competitors passing a mark at the
 * same time are still identified as not being equal.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class MarkPassingByTimeComparator implements Comparator<MarkPassing>, Serializable {
    private static final long serialVersionUID = 5758820139503482888L;
    public static MarkPassingByTimeComparator INSTANCE = new MarkPassingByTimeComparator();
    
    @Override
    public int compare(MarkPassing o1, MarkPassing o2) {
        int result = o1 == null ? o2 == null ? 0 : -1 : o2 == null ? 1 : o1.getTimePoint() == null ? (o2.getTimePoint() == null ? 0 : -1) : o2.getTimePoint() == null ? 1 : o1.getTimePoint().compareTo(o2.getTimePoint());
        if (result == 0 && o1 != null) {
            assert o2 != null;
            Object c1ID = o1.getCompetitor().getId();
            Object c2ID = o2.getCompetitor().getId();
            // shortcut comparison and don't use expensive toString() in case IDs are comparable by nature 
            if (c1ID instanceof Comparable<?> && c1ID.getClass() == c2ID.getClass()) {
                @SuppressWarnings("unchecked")
                final Comparable<Object> c1OID = (Comparable<Object>) c1ID;
                result = c1OID.compareTo(o2.getCompetitor().getId());
            } else {
                result = o1.getCompetitor().getId().toString().compareTo(o2.getCompetitor().getId().toString());
            }
        }
        return result;
    }

}
