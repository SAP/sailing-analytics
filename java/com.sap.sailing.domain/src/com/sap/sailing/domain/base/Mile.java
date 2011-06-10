package com.sap.sailing.domain.base;

public interface Mile {
    static double METERS_PER_SEA_MILE = 1852.3;
    static double METERS_PER_NAUTICAL_MILE = 1852;
    static double METERS_PER_GEOGRAPHICAL_MILE = 400080. / 216. /* = 40008000/360/60 */;
}
