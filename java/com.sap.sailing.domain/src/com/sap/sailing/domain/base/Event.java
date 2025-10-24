package com.sap.sailing.domain.base;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * An event is a group of {@link Regatta regattas} carried out at a common venue within a common time frame. For
 * example, Kiel Week 2011 is an event, and the International German Championship 2011 held, e.g., in Travem�nde, is an event,
 * too.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface Event extends EventBase {
    
    /**
     * For events, the ID is always a UUID.
     */
    UUID getId();
    
    /**
     * Returns a non-<code>null</code> live but unmodifiable collection of leaderboard groups that were previously
     * {@link #addLeaderboardGroup(LeaderboardGroup) added} to this event, in the order of their addition. Therefore, to
     * change the iteration order, {@link #removeLeaderboardGroup(LeaderboardGroup)} and
     * {@link #addLeaderboardGroup(LeaderboardGroup)} need to be used.
     */
    @Override
    Iterable<LeaderboardGroup> getLeaderboardGroups();
    
    void addLeaderboardGroup(LeaderboardGroup leaderboardGroup);
    
    /**
     * @return <code>true</code> if and only if a leaderboard group equal to <code>leaderboardGroup</code> was part of
     *         {@link #getLeaderboardGroups()} and therefore was actually removed
     */
    boolean removeLeaderboardGroup(LeaderboardGroup leaderboardGroup);

    /**
     * Replaces the {@link #getLeaderboardGroups() current contents of the leaderboard groups sequence} by the
     * leaderboard groups in <code>leaderboardGroups</code>.
     */
    void setLeaderboardGroups(Iterable<LeaderboardGroup> leaderboardGroups);
    
    /**
     * Adds an event listener to this event. Note that event listeners are <em>not</em> serialized together with this
     * event. The de-serialized event starts out with a new, empty event listener collection.
     */
    void addEventListener(EventListener eventListener);
    
    void removeEventListener(EventListener eventListener);
    
    /**
     * An event may happen in the vicinity of one or more WindFinder (https://www.windfinder.com) weather stations.
     * Which ones those are can be defined using {@link #setWindFinderReviewedSpotsCollection(Iterable)}, and this
     * getter returns the IDs last set.
     * 
     * @return an always valid (non-{@code null}) but possibly empty and unmodifiable set of strings; the set is "live,"
     *         meaning it will change as the underlying data changes
     */
    Iterable<String> getWindFinderReviewedSpotsCollectionIds();
    
    /**
     * This method may return WindFinder spot IDs based on the tracked races reachable from this event's associated
     * leaderboard groups and their wind sources. The {@link WindSource#getId() wind source IDs} of all wind sources of
     * type {@link WindSourceType#WINDFINDER} will be collected and returned.
     * 
     * @return an always valid (non-{@code null}) but possibly empty set of strings that is a non-live copy computed
     *         just for this call
     */
    Iterable<String> getAllFinderSpotIdsUsedByTrackedRacesInEvent();

    /**
     * Set the IDs of the reviewed WindFinder spot collections to consider during this event.
     * Setting this to a non-empty value shall lead to a corresponding display of a WindFinder
     * logo / link on the event's UI representation.
     */
    void setWindFinderReviewedSpotsCollection(Iterable<String> reviewedSpotsCollectionIds);
    
    /**
     * From indicators such as the tracking data inside {@link TrackedRace}s connected to this event and/or the
     * geo-coded {@link #getVenue() venue} name tries to obtain a position of this event. It is possible
     * that no such position can be obtained in which case this method returns {@code null}.
     */
    Position getLocation();

    /**
     * Obtains the leaderboards of this event. The "link" is made through the {@link #getLeaderboardGroups() leaderboard
     * groups} and from there to their {@link LeaderboardGroup#getLeaderboards() leaderboards}, filtering only those
     * whose {@link Leaderboard#getCourseAreas() course areas} have a non-empty intersection with this event
     * {@link #getVenue() venue}'s {@link Venue#getCourseAreas() course areas}.
     */
    default Iterable<Leaderboard> getLeaderboards() {
        final Set<Leaderboard> result = new HashSet<>();
        for (final LeaderboardGroup leaderboardGroup : getLeaderboardGroups()) {
            for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                if (leaderboard.isPartOfEvent(this)) {
                    result.add(leaderboard);
                }
            }
        }
        return result;
    }
}
