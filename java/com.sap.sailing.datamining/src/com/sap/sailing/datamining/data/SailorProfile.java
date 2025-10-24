package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.common.NamedWithUUID;
import com.sap.sse.common.Util;

/**
 * A sailor profile currently mainly consists of a set of competitors that it consolidates into a single profile. It can
 * be used particularly in a data mining context when there are multiple distinct records that from a user's point of
 * view represent the same sailor/competitor. With this, a user can define a {@link SailorProfile} like an "equivalence
 * class" that allows the user to, e.g., filter or group data mining results based on a competitor's "equivalence
 * class."
 * <p>
 * 
 * A typical example would be a single-handed class, say "ILCA 6," where the same person has been participating in
 * several events, but each time captured with a different technical competitor ID, maybe even competing under different
 * sail numbers. Yet, during analysis, it may make sense to group the results for all of these entities under one sailor
 * profile.
 * <p>
 * 
 * Technically, the sailor profiles are managed as user preferences in the user store. A preference converter can turn
 * the underlying JSON string into an {@link SailorProfilePreferences} object which contains
 * {@link SailorProfilePreference}. These, in turn, can be converted to instances of a type implementing this interface.
 * <p>
 * 
 * Comparability is defined such that no two profiles with different {@link #getId() IDs} will ever compare equal.
 * Beyond this, {@link SailorProfile}s with a greater number of {@link #getCompetitors() competitors} than another will
 * be considered "greater." In case of an equal number of competitors, the sailor profile {@link #getName() name} will
 * be used for lexicographic ordering. In case of equal names, sorting will work by the stringified version of the
 * {@link #getId() UUID}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface SailorProfile extends NamedWithUUID, Comparable<SailorProfile> {
    Iterable<Competitor> getCompetitors();

    @Override
    default int compareTo(SailorProfile o) {
        int result = Integer.compare(Util.size(getCompetitors()), Util.size(o.getCompetitors()));
        if (result == 0) {
            result = getName().compareTo(o.getName());
            if (result == 0) {
                result = getId().compareTo(o.getId());
            }
        }
        return result;
    }
}
