package com.sap.sailing.domain.statistics;

import com.sap.sailing.domain.common.Distance;

/**
 * A {@link Statistics} object represents several statistical information.
 */
public interface Statistics {

    /**
     * @return the number of competitors
     */
    int getNumberOfCompetitors();

    /**
     * @return the number of regattas
     */
    int getNumberOfRegattas();

    /**
     * @return the number of races
     */
    int getNumberOfRaces();

    /**
     * @return the number of tracked races
     */
    int getNumberOfTrackedRaces();

    /**
     * @return the number of GPS fixes
     */
    long getNumberOfGPSFixes();

    /**
     * @return the number of wind fixes
     */
    long getNumberOfWindFixes();

    /**
     * @return the totally sailed {@link Distance}; never {@code null}
     */
    Distance getDistanceTraveled();
}
