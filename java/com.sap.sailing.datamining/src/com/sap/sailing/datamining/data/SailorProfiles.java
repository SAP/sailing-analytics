package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Competitor;

/**
 * A "data mining" view on the current user's sailor profiles as defined in the user preferences. When an instance is
 * constructed, it collects all of the user's sailor profiles and builds a map from the {@link Competitor} objects to
 * the {@link SailorProfile} objects that refer to that {@link Competitor}.
 * <p>
 * 
 * When more than one of the user's {@link SailorProfile}s comprise the same {@link Competitor}, a stable algorithm
 * for determining a single preferred {@link SailorProfile} for the {@link Competitor} will be used in the
 * {@link #getProfileForCompetitor(Competitor)}, finding the "greatest" (as defined by the {@link Comparable} property
 * of the {@link SailorProfile} interface) profile for the {@link Competitor}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface SailorProfiles {
    SailorProfile getProfileForCompetitor(Competitor competitor);
}
