package com.sap.sailing.gwt.home.server;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.util.EventUtil;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.gwt.shared.DTO;

/**
 * This is a convenience class, which e.g. provides static methods to iterate over leaderboards or races of an entire
 * event or the races of a single regatta while keeping their orders. These methods provide a {@link LeaderboardContext}
 * or {@link RaceContext} instance to the given {@link LeaderboardCallback} or {@link RaceCallback}, respectively.
 * 
 * Also methods to calculate the {@link Duration time to live} based on the event state(non-live) are provided, which
 * are used to define the refresh interval in a {@link ResultWithTTL} for different UI components.
 */
@GwtIncompatible
public final class EventActionUtil {
    private static final Logger logger = Logger.getLogger(EventActionUtil.LeaderboardCallback.class.getName());
    
    private EventActionUtil() {
    }
    
    public interface CalculationWithEvent<T extends DTO> {
        ResultWithTTL<T> calculateWithEvent(Event event);
    }
    
    public interface RaceCallback {
        void doForRace(RaceContext context);
    }
    
    public interface LeaderboardCallback {
        void doForLeaderboard(LeaderboardContext context);
    }
    
    public static LeaderboardContext getOverallLeaderboardContext(SailingDispatchContext context, UUID eventId) {
        RacingEventService service = context.getRacingEventService();
        Event event = service.getEvent(eventId);
        if (!EventUtil.isFakeSeries(event)) {
            throw new DispatchException("The given event is not a series event.");
        }
        Leaderboard overallLeaderboard = null;
        for (final LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
            overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
            if (overallLeaderboard != null) {
                break;
            }
        }
        return new LeaderboardContext(context, event, event.getLeaderboardGroups(), overallLeaderboard);
    }
    
    public static LeaderboardContext getLeaderboardContextWithReadPermissions(SailingDispatchContext context, UUID eventId, String leaderboardId) {
        RacingEventService service = context.getRacingEventService();
        Event event = service.getEvent(eventId);
        context.getSecurityService().checkCurrentUserReadPermission(event);
        final LinkedHashSet<LeaderboardGroup> leaderboardGroups = new LinkedHashSet<>();
        Leaderboard leaderboard = null;
        for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
            if (context.getSecurityService().hasCurrentUserReadPermission(leaderboardGroup)) {
                for (Leaderboard l : leaderboardGroup.getLeaderboards()) {
                    if (context.getSecurityService().hasCurrentUserReadPermission(l)) {
                        if (l.getName().equals(leaderboardId)) {
                            leaderboardGroups.add(leaderboardGroup);
                            leaderboard = l;
                        }
                    }
                }

            }
        }
        if (leaderboardGroups.isEmpty()) {
            throw new DispatchException("The leaderboard '" + leaderboardId + "' is not part of the given event '" + eventId + "'.");
        }
        return new LeaderboardContext(context, event, leaderboardGroups, leaderboard);
    }
    
    /**
     * Gets or calculates the time to live for the event with the given ID, depending on its
     * {@link HomeServiceUtil#calculateEventState(com.sap.sailing.domain.base.EventBase) calculated state}.
     * 
     * @param context
     *            {@link SailingDispatchContext} to retrieve {@link Event}
     * @param eventId
     *            {@link UUID} of the {@link Event} to get/calculate the time to live for
     * @param liveTTL
     *            the {@link Duration time to live} to use if the event is currently running
     * @return the given liveTTL if the event is currently running, otherwise the
     *         {@link #calculateTtlForNonLiveEvent(Event, EventState) calculated TTL}
     */
    public static Duration getEventStateDependentTTL(SailingDispatchContext context, UUID eventId, Duration liveTTL) {
        Event event = context.getRacingEventService().getEvent(eventId);
        return getEventStateDependentTTL(event, liveTTL);
    }
    
    private static Duration getEventStateDependentTTL(Event event, Duration liveTTL) {
        EventState eventState = HomeServiceUtil.calculateEventState(event);
        if(eventState == EventState.RUNNING) {
            return liveTTL;
        }
        return calculateTtlForNonLiveEvent(event, eventState);
    }

    public static <T extends DTO> ResultWithTTL<T> withLiveRaceOrDefaultScheduleWithReadPermissions(SailingDispatchContext context,
            UUID eventId, CalculationWithEvent<T> callback) {
        return withLiveRaceOrDefaultScheduleWithReadPermissions(context, eventId, callback, null);
    }

    public static <T extends DTO> ResultWithTTL<T> withLiveRaceOrDefaultScheduleWithReadPermissions(SailingDispatchContext context,
            UUID eventId, CalculationWithEvent<T> callback, T defaultResult) {
        Event event = context.getRacingEventService().getEvent(eventId);
        if (event == null) {
            logger.warning("Event with ID "+eventId+" not found.");
            return new ResultWithTTL<T>(Duration.FOREVER, defaultResult);
        }
        context.getSecurityService().checkCurrentUserReadPermission(event);
        EventState eventState = HomeServiceUtil.calculateEventState(event);
        if (eventState == EventState.FINISHED) {
            return new ResultWithTTL<T>(calculateTtlForNonLiveEvent(event, eventState), defaultResult);
        }
        return callback.calculateWithEvent(event);
    }

    /**
     * Calculates the time to live based on the given {@link Event}'s {@link Event#getStartDate() start time} and the
     * given {@link HomeServiceUtil#calculateEventState(com.sap.sailing.domain.base.EventBase) calculated state}:
     * <p><table>
     *   <tr><th>Event state      </th><th>Start time                 </th><th>TTL               </th></tr>  
     *   <tr><td>UPMCOMING/PLANNED</td><td>36 hours and more    before</td><td>=>          1 hour</td></tr>  
     *   <tr><td>                 </td><td>less than 36 hours   before</td><td>=>      30 minutes</td></tr>  
     *   <tr><td>                 </td><td>less than  3 hours   before</td><td>=>      15 minutes</td></tr>  
     *   <tr><td>                 </td><td>less than 15 minutes before</td><td>=> time till start</td></tr>  
     *   <tr><td>FINISHED         </td><td>any                        </td><td>=>        12 hours</td></tr>
     *   <tr><td>RUNNING         </td><td>any                         </td><td>=>             [0]</td></tr>
     * </table></p>
     * 
     * @param event
     *            {@link Event} to calculate the time to live for
     * @param eventState
     *            the {@link EventState} to calculate the time to live for
     * @return The calculated {@link Duration time to live}
     */
    public static Duration calculateTtlForNonLiveEvent(Event event, EventState eventState) {
        if(eventState == EventState.UPCOMING || eventState == EventState.PLANNED) {
            long ttl = Duration.ONE_HOUR.asMillis();
            final TimePoint startDate = event.getStartDate();
            if (startDate != null) {
                final Duration tillStart = MillisecondsTimePoint.now().until(startDate);
                final double hoursTillStart = tillStart.asHours();
                if (hoursTillStart < 36) {
                    ttl = Duration.ONE_MINUTE.times(30).asMillis();
                }
                if (hoursTillStart < 3) {
                    ttl = Duration.ONE_MINUTE.times(15).asMillis();
                }
                ttl = Math.min(ttl, tillStart.asMillis());
            }
            return new MillisecondsDurationImpl(ttl);
        }
        if(eventState == EventState.FINISHED) {
            return Duration.ONE_HOUR.times(12);
        }
        return Duration.NULL;
    }
    
    public static void forLeaderboardsOfEventWithReadPermissions(SailingDispatchContext context, UUID eventId, LeaderboardCallback callback) {
        RacingEventService service = context.getRacingEventService();
        Event event = service.getEvent(eventId);
        if (event == null) {
            throw new RuntimeException("Event not found");
        }
        forLeaderboardsOfEventWithReadPermissions(context, event, callback);
    }

    public static void forLeaderboardsOfEventWithReadPermissions(SailingDispatchContext context, Event event, LeaderboardCallback callback) {
        final Map<Leaderboard, LinkedHashSet<LeaderboardGroup>> leaderboardGroupsForLeaderboard = new HashMap<>();
        for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
            if (context.getSecurityService().hasCurrentUserReadPermission(leaderboardGroup)) {
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if (leaderboard instanceof RegattaLeaderboard) {
                        if (!context.getSecurityService()
                                .hasCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta())) {
                            continue;
                        }
                    }
                    if (context.getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                        LinkedHashSet<LeaderboardGroup> set = leaderboardGroupsForLeaderboard.get(leaderboard);
                        if (set == null) {
                            set = new LinkedHashSet<>();
                            leaderboardGroupsForLeaderboard.put(leaderboard, set);
                        }
                        set.add(leaderboardGroup);
                    }
                }
            }
        }
        for (Entry<Leaderboard, LinkedHashSet<LeaderboardGroup>> e : leaderboardGroupsForLeaderboard.entrySet()) {
            final Set<LeaderboardGroup> leaderboardGroupsForRegatta = e.getValue();
            final LeaderboardContext leaderboardContext = new LeaderboardContext(context, event, e.getValue(), e.getKey());
            if (leaderboardGroupsForRegatta.size() == 1) {
                final LeaderboardGroup singleLeaderboardGroup = leaderboardGroupsForRegatta.iterator().next();
                if (singleLeaderboardGroup.hasOverallLeaderboard() && !leaderboardContext.isPartOfEvent()) {
                    // Regatta is associated to LeaderboardGroup that forms a series.
                    // In this case we only assume the Regatta to be part of the current event if the Regatta references the Event through the associated CourseArea.
                    continue;
                }
            }
            callback.doForLeaderboard(leaderboardContext);
        }
    }
    
    public static void forRacesOfEventWithReadPermissions(final SailingDispatchContext context, UUID eventId, final RaceCallback callback) {
        forLeaderboardsOfEventWithReadPermissions(context, eventId, new LeaderboardCallback() {
            @Override
            public void doForLeaderboard(LeaderboardContext leaderboardContext) {
                leaderboardContext.forRacesWithReadPermissions(callback);
            }
        });
    }
    
    public static void forRacesOfRegattaWithReadPermissions(SailingDispatchContext context, UUID eventId, String regattaName, RaceCallback callback) {
        getLeaderboardContextWithReadPermissions(context, eventId, regattaName).forRacesWithReadPermissions(callback);
    }
}
