package com.sap.sailing.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardSearchResult;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.LeaderboardSearchResultImpl;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.interfaces.KeywordQueryWithOptionalEventQualification;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.TaggingService;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.AbstractListFilter;
import com.sap.sse.common.search.KeywordQuery;
import com.sap.sse.common.search.Result;
import com.sap.sse.common.search.ResultImpl;
import com.sap.sse.security.SecurityService;

/**
 * Searches a {@link RacingEventService} instance for regattas that somehow match with a
 * {@link KeywordQuery}. Several attributes on the way are considered, in particular the event
 * name (if a regatta is somehow linked to an event), the event's venue name, the leaderboard group
 * name that contains a regatta leaderboard for the subject regatta, the regatta name itself,
 * the regatta's boat class name, and the names of all competitors entered into the regatta.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RegattaByKeywordSearchService {
    private static final Logger logger = Logger.getLogger(RegattaByKeywordSearchService.class.getName());
    
    Result<LeaderboardSearchResult> search(final RacingEventService racingEventService, KeywordQueryWithOptionalEventQualification query) {
        final SecurityService securityService = racingEventService.getSecurityService();
        final ResultImpl<LeaderboardSearchResult> result = new ResultImpl<>(query, new LeaderboardSearchResultRanker(racingEventService));
        final Map<LeaderboardGroup, Set<Event>> eventsForLeaderboardGroup = new HashMap<>();
        final Map<Leaderboard, Set<LeaderboardGroup>> leaderboardGroupsForLeaderboard = new HashMap<>();
        final Map<CourseArea, Event> eventForCourseArea = new HashMap<>();
        final Map<Event, Set<String>> stringsForEvent = new HashMap<>();
        final Map<LeaderboardGroup, Set<String>> stringsForLeaderboardGroup = new HashMap<>();
        for (final Event event : racingEventService.getEventsSelectively(query.isInclude(), query.getEventUUIDs())) {
            if (securityService.hasCurrentUserReadPermission(event)) {
                final Set<String> s4e = new HashSet<>();
                s4e.add(event.getName());
                s4e.add(event.getVenue().getName());
                stringsForEvent.put(event, s4e);
                for (final LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
                    Util.add(eventsForLeaderboardGroup, leaderboardGroup, event);
                }
                for (final CourseArea courseArea : event.getVenue().getCourseAreas()) {
                    eventForCourseArea.put(courseArea, event);
                }
            }
        }
        for (final LeaderboardGroup leaderboardGroup : racingEventService.getLeaderboardGroups().values()) {
            if (securityService.hasCurrentUserReadPermission(leaderboardGroup)) {
                final Set<String> s4lg = new HashSet<>();
                s4lg.add(leaderboardGroup.getName());
                s4lg.add(leaderboardGroup.getDescription());
                stringsForLeaderboardGroup.put(leaderboardGroup, s4lg);
                for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    Util.add(leaderboardGroupsForLeaderboard, leaderboard, leaderboardGroup);
                }
            }
        }
        final TaggingService taggingService = racingEventService.getTaggingService();
        final AbstractListFilter<Leaderboard> leaderboardFilter = new AbstractListFilter<Leaderboard>() {
            @Override
            public Iterable<String> getStrings(Leaderboard leaderboard) {
                // TODO allow recording which part of the leaderboard was matched by the keywords by returning "annotated strings" that the matcher can understand
                List<String> leaderboardStrings = new ArrayList<>();
                leaderboardStrings.add(leaderboard.getName());
                leaderboardStrings.add(leaderboard.getDisplayName());
                for (Competitor competitor : leaderboard.getCompetitors()) {
                    leaderboardStrings.add(competitor.getName());
                    if (competitor.getShortName() != null) {
                        leaderboardStrings.add(competitor.getShortName());
                    }
                    if (competitor.getSearchTag() != null) {
                        leaderboardStrings.add(competitor.getSearchTag());
                    }
                    if (competitor.hasBoat() && ((CompetitorWithBoat) competitor).getBoat().getSailID() != null) {
                        leaderboardStrings.add(((CompetitorWithBoat) competitor).getBoat().getSailID());
                    }
                    String competitorDisplayName = leaderboard.getDisplayName(competitor);
                    if (competitorDisplayName != null) {
                        leaderboardStrings.add(competitorDisplayName);
                    }
                }
                for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    for (final Fleet fleet : raceColumn.getFleets()) {
                        try {
                            for (final TagDTO tag : taggingService.getTags(leaderboard, raceColumn, fleet, /* searchSince */ null, /* returnRevokedTags */ false)) {
                                if (tag.getTag() != null) {
                                    leaderboardStrings.add(tag.getTag());
                                }
                                if (tag.getComment() != null) {
                                    leaderboardStrings.add(tag.getComment());
                                }
                            }
                        } catch (RaceLogNotFoundException | ServiceNotFoundException e) {
                            logger.log(Level.WARNING, "Problem obtaining tags for leaderboard "+leaderboard.getName()+
                                    ", race column "+raceColumn.getName()+" and fleet "+fleet.getName(), e);
                        }
                    }
                }
                final Iterable<LeaderboardGroup> leaderboardGroupsHostingLeaderboard = leaderboardGroupsForLeaderboard.get(leaderboard);
                if (leaderboardGroupsHostingLeaderboard != null) {
                    for (LeaderboardGroup leaderboardGroup : leaderboardGroupsHostingLeaderboard) {
                        leaderboardStrings.addAll(stringsForLeaderboardGroup.get(leaderboardGroup));
                        final Set<Event> eventsForLG = filterEventsForLeaderboard(leaderboard, leaderboardGroup, eventsForLeaderboardGroup.get(leaderboardGroup));
                        if (eventsForLG != null) {
                            for (final Event event : eventsForLG) {
                                leaderboardStrings.addAll(stringsForEvent.get(event));
                            }
                        }
                    }
                }
                final Optional<Event> eventByDefaultCourseArea = StreamSupport
                        .stream(leaderboard.getCourseAreas().spliterator(), /* parallel */ false)
                        .filter(ca -> eventForCourseArea.containsKey(ca)).findFirst()
                        .map(ca -> eventForCourseArea.get(ca));
                eventByDefaultCourseArea.ifPresent(e -> leaderboardStrings.addAll(stringsForEvent.get(e)));
                return leaderboardStrings;
            }
        };
        final Set<Leaderboard> leaderboardsToConsider = StreamSupport.stream(racingEventService.getAllEvents().spliterator(), /* parallel */ false).
            filter(e->e.isPublic() && securityService.hasCurrentUserReadPermission(e)).
            flatMap(e->StreamSupport.stream(e.getLeaderboardGroups().spliterator(), /* parallel */ false).
                           filter(lg->securityService.hasCurrentUserReadPermission(lg))).
            flatMap(lg->StreamSupport.stream(lg.getLeaderboards().spliterator(), /* parallel */ false).
                           filter(l->securityService.hasCurrentUserReadPermission(l))).
            collect(Collectors.toSet());
        for (Leaderboard matchingLeaderboard : leaderboardFilter.applyFilter(query.getKeywords(), leaderboardsToConsider)) {
            result.addHit(new LeaderboardSearchResultImpl(matchingLeaderboard,
                    getEventsForLeaderboard(matchingLeaderboard, leaderboardGroupsForLeaderboard,
                            eventsForLeaderboardGroup, eventForCourseArea), leaderboardGroupsForLeaderboard
                            .get(matchingLeaderboard)));
        }
        return result;
    }
    
    /**
     * For leaderboards that are part of a series, there used to be search results that linked to the leaderboard in
     * combination with one random event of that series. It was the wrong event for all leaderboards except one of a
     * series. In bug3348 a change was made to show all events associated to a leaderboard in the search results. This
     * lead to an "explosion of results" as there were potentially n results referencing n events instead of each result
     * only referencing the associated event.
     * <p>
     * 
     * This filters the events to be associated to a leaderboard. If the leaderboardGroup has a OverallLeaderboard (in
     * case of a series), there is a special matching to find the right event. If a leaderboard has a defaultCourseArea,
     * the event hosting this CourseArea is the right one. If this reference isn't given or the CourseArea doesn't
     * belong to an event of the series, the fallback behavior is causing all events to be returned.
     * <p>
     * 
     * This doesn't affect any leaderboard's event set if the leaderboard isn't part of a series.
     * <p>
     * 
     * @param leaderboard
     *            the leaderboard to get the matching events for
     * @param leaderboardGroup
     *            the LeaderboardGroup hosting the leaderboard
     * @param events
     *            all events hosting the LeaderboardGroup; may be {@code null}; if {@code null}, result will be
     *            {@code null}
     * @return the best matching events for the given Leaderboard/LeaderboardGroup
     */
    private Set<Event> filterEventsForLeaderboard(Leaderboard leaderboard, LeaderboardGroup leaderboardGroup, Set<Event> events) {
        final Set<Event> result;
        if (events != null && leaderboardGroup.hasOverallLeaderboard()) {
            Set<Event> preResult = null;
            for (Event event : events) {
                if (Util.containsAny(event.getVenue().getCourseAreas(), leaderboard.getCourseAreas())) {
                    preResult = Collections.singleton(event);
                    break;
                }
            }
            result = preResult;
        } else {
            result = events;
        }
        return result;
    }

    private Set<Event> getEventsForLeaderboard(Leaderboard matchingLeaderboard,
            Map<Leaderboard, Set<LeaderboardGroup>> leaderboardGroupsForLeaderboard,
            Map<LeaderboardGroup, Set<Event>> eventsForLeaderboardGroup, Map<CourseArea, Event> eventForCourseArea) {
        final Set<Event> result = new HashSet<>();
        final Set<LeaderboardGroup> lgs = leaderboardGroupsForLeaderboard.get(matchingLeaderboard);
        if (lgs != null) {
            for (final LeaderboardGroup lg : lgs) {
                final Set<Event> eventsForLG = filterEventsForLeaderboard(matchingLeaderboard, lg, eventsForLeaderboardGroup.get(lg));
                if (eventsForLG != null) {
                    result.addAll(eventsForLG);
                }
            }
        }
        final Optional<Event> eventByDefaultCourseArea = StreamSupport
                .stream(matchingLeaderboard.getCourseAreas().spliterator(), /* parallel */ false)
                .filter(ca -> eventForCourseArea.containsKey(ca)).findFirst()
                .map(ca -> eventForCourseArea.get(ca));
        eventByDefaultCourseArea.ifPresent(e -> result.add(e));
        return result;
    }
}
