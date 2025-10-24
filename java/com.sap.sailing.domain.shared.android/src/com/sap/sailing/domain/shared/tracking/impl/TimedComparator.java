package com.sap.sailing.domain.shared.tracking.impl;

import java.io.Serializable;
import java.util.Comparator;

import com.sap.sse.common.Timed;

public class TimedComparator implements Comparator<Timed>, Serializable {
    private static final long serialVersionUID = 1604511471599854988L;
    public static final Comparator<Timed> INSTANCE = new TimedComparator();

    @Override
    public int compare(Timed o1, Timed o2) {
        return o1.getTimePoint().compareTo(o2.getTimePoint());
    }
}

