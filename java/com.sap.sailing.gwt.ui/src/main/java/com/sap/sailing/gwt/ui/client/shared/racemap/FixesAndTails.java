package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.ui.actions.GetBoatPositionsAction;
import com.sap.sailing.gwt.ui.actions.GetRaceMapDataAction;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegTypeIterable;
import com.sap.sse.common.ColorMapper;
import com.sap.sse.common.Duration;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.ValueRangeFlexibleBoundaries;
import com.sap.sse.gwt.client.async.TimeRangeActionsExecutor;

/**
 * Manages the cache of {@link GPSFixDTOWithSpeedWindTackAndLegType}s for the competitors and the {@link Colorline}s
 * representing sub-sets of those fixes, encoding the tails that visualize the course the boats took. The fixes cached
 * for each competitor are intended to represent a contiguous segment of the competitor's track managed by the server. A
 * tail may show a sub-segment of the fixes cached for a competitor, for example, because new fixes are added to the end
 * of a competitors fixes cache, extending the contiguous cached segment beyond the length visualized by the tail. This
 * way, moving back and forth in time while updating the tail may sometimes be possible even without having to load more
 * data from the server. The last fix of a segment of cached fixes may be an
 * {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated} one. When a later (extrapolated or real) fix
 * is to be cached for that competitor, the earlier extrapolated fix is removed from the cache and, if currently
 * visualized on the tail, also from the tail.
 * <p>
 * 
 * The "contiguous condition" may be violated temporarily due to requests for data being executed asynchronously, with
 * results being processed out of order, and with compound requests getting dropped and only their position data request
 * being re-sent later.
 * <p>
 * 
 * If a competitor's tail is to be shown for a time range for which data is missing from the cache, and the time range
 * does not have an overlap with the cached time range, in order to maintain only contiguous track segments in the
 * cache, the previously cached fixes will be dropped from the cache, and a new contiguous segment will be started as
 * the response to the new request is processed. "In-flight" requests for data that would extend the previous track
 * segment will have their callbacks informed so that when processing the response they don't try to merge their fixes
 * into the cache anymore, basically dropping the fixes received to avoid cache inconsistencies.
 * <p>
 * 
 * Fixes can optionally be annotated with detail values that can be used to color the tail based on the respective
 * value. For example, the detail value may represent the boat's speed over ground (SOG) in knots, or its velocity made
 * good (VMG) in knots. The semantics of the detail value is described by a {@link DetailType}. Currently, the detail
 * values are always obtained together with the basic GPS fix data such as position and time point. As the user selects
 * a different detail type, the detail values of those competitors who have their tail colored based on the detail
 * values need to be updated to reflect the new detail type; this requires a re-load at least of the track segment
 * currently visualized for that competitor, dropping the data cached for that competitor so far. (See also bug 5925
 * which considers separating the detail values from the fixes.)
 * <p>
 * 
 * When showing colored tails, the color range is determined by monitoring minimum and maximum detail values across all
 * competitors for which colored tails are shown. The color range is adjusted in case the value range changes beyond a
 * certain threshold (see {@link ValueRangeFlexibleBoundaries}). To achieve this, adding fixes to a colored tail must
 * check the new fixes for extreme values; when removing a fix from a colored tail that had a minimal / maximal detail
 * value, that tail must be searched again for a new minimal / maximal value.
 * <p>
 * 
 * A client of an instance of this class (typically a {@link RaceMap}) interacts in three possible ways:
 * <ol>
 * <li>Preparing the requests that update the fixes cache: Based on the tail length and a "current time" (usually
 * defined by the {@link com.sap.sse.gwt.client.player.Timer Timer}'s
 * {@link com.sap.sse.gwt.client.player.Timer#getTime() time slider}), one or two requests including callback handlers
 * are returned: one for a quick request for only a short piece of the track, and optionally another one for a longer
 * piece of the track, where the remote call is expected to take a bit longer than we would like to wait with showing
 * the current boat position. The requests know the time ranges to request for which competitor, and they know whether
 * when processing their response they first need to {@link #clearTails() clear} the cache before entering new,
 * non-overlapping positions. The two requests are linked to each other and share the knowledge about whether the first
 * of them to process its response needs to clear the cache. The resulting time ranges already received or requested per
 * competitor are remembered and are used to trim subsequent request. Should later requests require a clearing of the
 * cache (such as after changing the detail type or when requesting a disconnected time range), requests still "in
 * flight" will have their callbacks informed so that they will discard the positions they will receive.</li>
 * <li>Processing the responses to those requests: The callbacks returned with the one or two requests from the previous
 * step check whether their results are still valid; if so, they use
 * {@link #updateFixes(Map, Map, long, boolean, DetailType)} to install the position fixes received in the cache and
 * either clear (in case the response started a new contiguous track segment) or incrementally update an existing
 * tail.</li>
 * <li>Request initial creation or incremental update of a competitor's tail: This is used to adjust the time range
 * for which the tail visualizes the cached fixes. Tail creation and update is largely independent of requesting and
 * updating cached fixes; instead, tail updates use the cached fixes currently available. Fixes being updated into
 * the cache upon receiving responses from the server will update the visible tails if they fall into the desired
 * visible time range.</li>
 * </ol>
 * 
 * This class offers methods to update the fixes and the tails, making sure that the data is always managed
 * consistently. In particular, it keeps an eye on {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated
 * fixes}. Those are just a guess where a boat may have been and will need to be removed once actual data for that time
 * is available.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FixesAndTails {
    private static final Duration MAX_DURATION_FOR_QUICK_REQUESTS = Duration.ONE_SECOND.times(10l);
    

    /**
     * Fixes of each competitors tail. If a list is contained for a competitor, the list contains a timely "contiguous"
     * list of fixes for the competitor. This means the server has no more data for the time interval covered, unless
     * the last fix was {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated obtained by extrapolation}.
     * <p>
     * 
     * If the fixes for a competitor contain an {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated}
     * fix, that fix is always guaranteed to be the last element of the list when outside the execution of a method on
     * this class. This in particular means that when more fixes are added, and there is now one fix later than the
     * extrapolated fix, the extrapolated fix will be removed, re-establishing the invariant of an extrapolated fix
     * always being the last in the list.
     */
    private final Map<String, List<GPSFixDTOWithSpeedWindTackAndLegType>> fixesByCompetitorIdsAsStrings;
    
    /**
     * Tails of competitors currently displayed as overlays on the map. A tail may have an {@link MVCArray#getLength()
     * empty} {@link Polyline#getPath()}. In this case, {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings} will hold
     * <code>-1</code> for that competitor key.
     */
    private final Map<String, Colorline> tailsByCompetitorIdsAsStrings;

    /**
     * Key set is equal to that of {@link #tailsByCompetitorIdsAsStrings} and tells what the index in {@link #fixesByCompetitorIdsAsStrings} of the first fix shown
     * in {@link #tailsByCompetitorIdsAsStrings} is. If a key is contained in this map, it is also contained in {@link #lastShownFixByCompetitorIdsAsStrings} and vice
     * versa. If a tail is present but has an empty path, this map does not contain an entry for that competitor.
     */
    private final Map<String, Integer> firstShownFixByCompetitorIdsAsStrings;

    /**
     * Key set is equal to that of {@link #tailsByCompetitorIdsAsStrings} and tells what the index in {@link #fixesByCompetitorIdsAsStrings} of the last fix shown in
     * {@link #tailsByCompetitorIdsAsStrings} is. If a key is contained in this map, it is also contained in {@link #firstShownFixByCompetitorIdsAsStrings} and vice
     * versa. If a tail is present but has an empty path, this map does not contain an entry for that competitor.
     */
    private final Map<String, Integer> lastShownFixByCompetitorIdsAsStrings;
    
    /**
     * The position fixes in {@link #fixesByCompetitorIdsAsStrings} are filled by processing the responses to asynchronous requests. This map
     * keeps track of the beginning and end of the contiguous time range for which position data has been requested, per
     * competitor. This may cover time ranges requested but not yet responded to, represented by the
     * {@link #inFlightRequests}. The earliest requested time point for a competitor may also be earlier than the
     * earliest fix eventually received and cached, for example, because the start of the requested time range does not
     * coincide exactly with the time stamp of a fix. Likewise, the latest requested time point may be later than the
     * latest fix eventually received and cached.
     * <p>
     * 
     * These time ranges shall be used to trim new position requests (see
     * {@link #computeFromAndTo(Date, Iterable, long, long, boolean)}), and to decide
     * whether a new request will contiguously extend the time range or will have to start a new, disconnected time
     * range, clearing the cache contents for that competitor. When trimming, if no request is currently
     * {@link #inFlightRequests in flight}, instead of the last time point <em>requested</em>, the time
     * point of the last fix <em>received</em> shall be used for trimming because we may hope to receive
     * fixes delivered late, e.g. in a live situation with too short a delay.
     */
    private final Map<String, TimeRange> timeRangesRequestedByCompetitorIdAsString;

    /**
     * Stores the index to the smallest found detailValue in {@link #fixesByCompetitorIdsAsStrings} for a given competitor.
     */
    private final Map<String, Integer> minDetailValueFixByCompetitorIdsAsStrings;
    
    /**
     * Stores the index to the largest found detailValue in {@link #fixesByCompetitorIdsAsStrings} for a given competitor.
     */
    private final Map<String, Integer> maxDetailValueFixByCompetitorIdsAsStrings;
    
    /**
     * Stores at what index the last search on the fixes of a given competitor stopped.
     */
    private final Map<String, Integer> lastSearchedFixByCompetitorIdsAsStrings;

    /**
     * Keeps track of detailValues (stored in {@link GPSFixDTOWithSpeedWindTackAndLegType} in {@link #fixesByCompetitorIdsAsStrings})
     * boundaries so that a {@link ColorMapper} can be used.
     */
    private ValueRangeFlexibleBoundaries detailValueBoundaries;

    private final CoordinateSystem coordinateSystem;
    
    /**
     * Tells the type of value stored in the fixes' {@link GPSFixDTOWithSpeedWindTackAndLegType#detailValue} field for
     * the competitor used as the key in this map. If {@code null} or no mapping exists for the competitor, the detail
     * values shall be ignored and may be of inconsistent types. If not {@code null}, all fixes stored in {@link #fixesByCompetitorIdsAsStrings}
     * are guaranteed to have their detail value of this type.
     */
    private final Map<String, DetailType> detailTypesRequestedByCompetitorIdsAsStrings;
    
    /**
     * Requests returned from {@link #computeFromAndTo(Date, Iterable, long, long, boolean)} for execution as quick or slow
     * requests that have not yet started {@link PositionRequest#processResponse(Map) processing their response}.
     */
    private final Set<PositionRequest> inFlightRequests;
    
    /**
     * The result of {@link FixesAndTails#computeFromAndTo(Date, Iterable, long, long, boolean)}; two such requests may result
     * from the ask to obtain positions for a certain time range per competitor from the server: one for quick
     * execution, and another one for a potentially long-running request. The two requests may be "entangled" regarding
     * their need to clear the fixes cache of a competitor when the first of the two has received its result. As soon as
     * the first of the two has cleared fixes from the cache, the second one to receive its response will refrain from
     * doing so.
     * <p>
     * 
     * A request may be marked as <em>invalid</em> regarding zero or more competitors. This happens when a request is
     * made for a new disconnected track segment or a different detail type, where the response will clear the cache for
     * one or more competitors. In such a case, all "in-flight" requests at the time will be informed to drop their
     * position fixes for those competitors.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    class PositionRequest {
        /**
         * The <em>same</em> set for two entangled requests; when a request processes its response and
         * finds here that a competitor's cache must first be cleared before entering new position fixes,
         * that competitor is removed from this set, so an entangled request that receives its response
         * later will not clear the cache again.
         */
        private final Set<String> mustClearCacheForTheseCompetitorIdsAsString;
        
        private final Set<String> ignoreFixesForTheseCompetitorIdsAsString;
        
        private final Map<String, TimeRange> timeRangesByCompetitorIdAsString;
        
        private final long transitionTimeInMillis;
        
        private final DetailType detailTypeForFixes;
        
        PositionRequest(Map<String, TimeRange> timeRanges, Set<String> mustClearCacheForTheseCompetitorIdsAsString, DetailType detailTypeForFixes, long transitionTimeInMillis) {
            ignoreFixesForTheseCompetitorIdsAsString = new HashSet<>();
            this.mustClearCacheForTheseCompetitorIdsAsString = mustClearCacheForTheseCompetitorIdsAsString;
            this.timeRangesByCompetitorIdAsString = Collections.unmodifiableMap(timeRanges);
            this.detailTypeForFixes = detailTypeForFixes;
            this.transitionTimeInMillis = transitionTimeInMillis;
        }
        
        /**
         * Creates a position request that is entangled with the {@code entangledWith} request. It uses the
         * same {@link DetailType} and {@code transitionTimeInMillis} but has its own time ranges. The first of the
         * two requests---this new one or the {@code entagledWith} request---that starts processing its response
         * makes sure to clear the cache for the competitors for which this was requested; the other request
         * is told to not clear those caches anymore.
         */
        PositionRequest(Map<String, TimeRange> timeRanges, PositionRequest entangleWith) {
            mustClearCacheForTheseCompetitorIdsAsString = entangleWith.mustClearCacheForTheseCompetitorIdsAsString;
            ignoreFixesForTheseCompetitorIdsAsString = new HashSet<>();
            this.timeRangesByCompetitorIdAsString = Collections.unmodifiableMap(timeRanges);
            this.detailTypeForFixes = entangleWith.detailTypeForFixes;
            this.transitionTimeInMillis = entangleWith.transitionTimeInMillis;
        }
        
        boolean isMustClearCacheForCompetitor(CompetitorDTO competitor) {
            return mustClearCacheForTheseCompetitorIdsAsString.contains(competitor.getIdAsString());
        }
        
        Map<String, Date> getFromByCompetitorIdAsString() {
            return getFromOrToByCompetitorIdAsString(TimeRange::from);
        }
        
        Map<String, Date> getToByCompetitorIdAsString() {
            return getFromOrToByCompetitorIdAsString(TimeRange::to);
        }
        
        private Map<String, Date> getFromOrToByCompetitorIdAsString(Function<TimeRange, TimePoint> dateFetcher) {
            final Map<String, Date> result = new HashMap<>();
            for (final Entry<String, TimeRange> e : timeRangesByCompetitorIdAsString.entrySet()) {
                if (!ignoreFixesForTheseCompetitorIdsAsString.contains(e.getKey())) {
                    result.put(e.getKey(), dateFetcher.apply(e.getValue()).asDate());
                }
            }
            return result;
        }
        
        /**
         * Informs this request that when it receives position fixes for {@code competitor} it shall not
         * store them into the cache. The reason is probably that a newer request has been formed that will
         * have to clear that competitor's cache when processing its response, e.g., because the time range
         * requested is disconnected from the track segment cached for that competitor so far.
         */
        void ignoreFixesFor(CompetitorDTO competitor) {
            ignoreFixesForTheseCompetitorIdsAsString.add(competitor.getIdAsString());
        }
        
        void processResponse(Map<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> boatPositions) {
            if (!inFlightRequests.remove(this)) {
                GWT.log("WARNING: processing response for a request that does not seem to have been sent or that has already been processed: "+this);
            }
            for (final Entry<CompetitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable> e : boatPositions.entrySet()) {
                if (!ignoreFixesForTheseCompetitorIdsAsString.contains(e.getKey().getIdAsString())) {
                    final boolean mustClearCache = mustClearCacheForTheseCompetitorIdsAsString.remove(e.getKey().getIdAsString());
                    updateFixes(e.getKey(), e.getValue(), mustClearCache, transitionTimeInMillis, detailTypeForFixes);
                }
            }
        }

        @Override
        public String toString() {
            return "PositionRequest [mustClearCacheForTheseCompetitors=" + mustClearCacheForTheseCompetitorIdsAsString
                    + ", ignoreFixesForTheseCompetitors=" + ignoreFixesForTheseCompetitorIdsAsString + ", timeRanges="
                    + timeRangesByCompetitorIdAsString + ", transitionTimeInMillis=" + transitionTimeInMillis + ", detailTypeForFixes="
                    + detailTypeForFixes + "]";
        }

        public TimePoint getToTimepoint(CompetitorDTO competitor) {
            final TimePoint result;
            if (ignoreFixesForTheseCompetitorIdsAsString.contains(competitor.getIdAsString())) {
                result = null;
            } else {
                final TimeRange competitorTimeRange = timeRangesByCompetitorIdAsString.get(competitor.getIdAsString());
                if (competitorTimeRange == null) {
                    result = null;
                } else {
                    result = competitorTimeRange.to();
                }
            }
            return result;
        }
    }

    public FixesAndTails(CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
        detailTypesRequestedByCompetitorIdsAsStrings = new HashMap<>();
        fixesByCompetitorIdsAsStrings = new HashMap<>();
        tailsByCompetitorIdsAsStrings = new HashMap<>();
        firstShownFixByCompetitorIdsAsStrings = new HashMap<>();
        lastShownFixByCompetitorIdsAsStrings = new HashMap<>();
        minDetailValueFixByCompetitorIdsAsStrings = new HashMap<>();
        maxDetailValueFixByCompetitorIdsAsStrings = new HashMap<>();
        lastSearchedFixByCompetitorIdsAsStrings = new HashMap<>();
        inFlightRequests = new HashSet<>();
        timeRangesRequestedByCompetitorIdAsString = new HashMap<>();
    }

    /**
     * @return the list of fixes cached for the competitor; if a fix is {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated}, it
     *         must be the last fix in the list. <code>null</code> may be returned in no fixes are cached for
     *         <code>competitor</code>. The list returned is unmodifiable for the caller.
     */
    public List<GPSFixDTOWithSpeedWindTackAndLegType> getFixes(CompetitorDTO competitor) {
        final List<GPSFixDTOWithSpeedWindTackAndLegType> competitorFixes = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString());
        return competitorFixes == null ? null : Collections.unmodifiableList(competitorFixes);
    }
    
    public Colorline getTail(CompetitorDTO competitor) {
        return tailsByCompetitorIdsAsStrings.get(competitor.getIdAsString());
    }

    public Integer getFirstShownFix(CompetitorDTO competitor) {
        return firstShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
    }
    
    /**
     * The set of all competitors for which this object maintains tails. The collection is unmodifiable for the caller.
     */
    public Set<String> getCompetitorIdsAsStringWithTails() {
        return Collections.unmodifiableSet(tailsByCompetitorIdsAsStrings.keySet());
    }

    public boolean hasFixesFor(CompetitorDTO competitor) {
        return fixesByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString());
    }
    
    /**
     * When for a competitor the last fix obtained from the server is {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated}, the quality
     * of this fix depends on the time difference between the extrapolated fix's time point and the last non-extrapolated
     * fix's time point. This time difference in milliseconds is returned by this method, or <code>0</code> in case
     * the last fix for <code>competitor</code> is not extrapolated.
     */
    protected long getMillisecondsBetweenExtrapolatedAndLastNonExtrapolatedFix(CompetitorDTO competitor) {
        List<GPSFixDTOWithSpeedWindTackAndLegType> competitorFixes = getFixes(competitor);
        final long result;
        if (competitorFixes == null || competitorFixes.size() < 2 || !competitorFixes.get(competitorFixes.size()-1).extrapolated) {
            result = 0;
        } else {
            // last fix is extrapolated and another fix present; check time difference
            GPSFixDTOWithSpeedWindTackAndLegType extrapolatedFix = competitorFixes.get(competitorFixes.size()-1);
            GPSFixDTOWithSpeedWindTackAndLegType fixBeforeExtrapolated = competitorFixes.get(competitorFixes.size()-2);
            result = extrapolatedFix.timepoint.getTime() - fixBeforeExtrapolated.timepoint.getTime();
        }
        return result;
    }

    /**
     * Creates a polyline for the competitor represented by <code>competitorDTO</code>, taking the fixes from
     * {@link #fixesByCompetitorIdsAsStrings fixes.get(competitorDTO)} and using the fixes starting at time point <code>from</code> (inclusive)
     * up to the last fix with time point before <code>to</code>. The polyline is added to the map and returned. Updates
     * are applied to {@link #lastShownFixByCompetitorIdsAsStrings}, {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #tailsByCompetitorIdsAsStrings}.
     * <p>
     * 
     * The {@link #fixesByCompetitorIdsAsStrings} map must hold an entry for {@code competitorDTO}, but the fixes it holds for that competitor
     * do not need to cover or even touch the time range described by {@code from} and {@code to}. As a result, the
     * color-line returned may be empty or contain fewer fixes than desired. Later calls to
     * {@link #updateFixes(Map, Map, long, boolean, DetailType)} may then extend the tail accordingly.
     * 
     * Precondition: <code>tails.containsKey(competitorDTO) == false</code>
     * 
     * @param detailTypeToShow
     *            the detail type the caller expects the fixes of {@code competitorDTO} to contain
     */
    protected Colorline createTailAndUpdateIndices(final CompetitorDTO competitorDTO, Date from, Date to, TailFactory tailFactory, DetailType detailTypeToShow) {
        if (detailTypeToShow != null && detailTypesRequestedByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) != detailTypeToShow) {
            GWT.log("WARNING: Detail type mismatch in createTailAndUpdateIndices: have "+detailTypesRequestedByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())+" but caller expected "+detailTypeToShow);
        }
        final List<LatLng> points = new ArrayList<LatLng>();
        final List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = getFixes(competitorDTO);
        int indexOfFirst = -1;
        int indexOfLast = -1;
        int i = 0;
        // TODO consider binary search to find beginning of interesting segment faster
        for (Iterator<GPSFixDTOWithSpeedWindTackAndLegType> fixIter = fixesForCompetitor.iterator(); fixIter.hasNext() && indexOfLast == -1;) {
            final GPSFixDTOWithSpeedWindTackAndLegType fix = fixIter.next();
            if (!fix.timepoint.before(to)) {
                indexOfLast = i-1;
            } else {
                final LatLng point;
                if (indexOfFirst == -1) {
                    if (!fix.timepoint.before(from)) {
                        indexOfFirst = i;
                        point = coordinateSystem.toLatLng(fix.position);
                    } else {
                        point = null;
                    }
                } else {
                    point = coordinateSystem.toLatLng(fix.position);
                }
                if (point != null) {
                    points.add(point);
                }
            }
            i++;
        }
        if (indexOfLast == -1) { // all fixes consumed; all were before "to"
            indexOfLast = i - 1;
        }
        if (indexOfFirst != -1 && indexOfLast != -1) {
            firstShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfFirst);
            lastShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfLast);
        }
        final Colorline result = tailFactory.createTail(competitorDTO, points);
        tailsByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), result);
        return result;
    }

    /**
     * Adds the fixes received in <code>fixesToAddForCompetitor</code> to {@link #fixesByCompetitorIdsAsStrings} and ensures they are still
     * contiguous for each competitor. If <code>overlapsWithKnownFixes</code> indicates that the fixes received in
     * <code>result</code> overlap with those already known, the fixes are merged into the list of already known fixes
     * for the competitor. Otherwise, the fixes received in <code>result</code> replace those known so far for the
     * respective competitor. The {@link #tailsByCompetitorIdsAsStrings} affected by these fixes are updated accordingly when modifications fall
     * inside the interval shown by the tail, as defined by {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings}. The tails
     * are, however, not trimmed according to the specification for the tail length. This has to happen elsewhere (see
     * also {@link #updateTail}).
     * 
     * @param fixesForCompetitors
     *            For each list the invariant must hold that an {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated
     *            extrapolated} fix must be the last one in the list
     * @param overlapsWithKnownFixes
     *            if for a competitor whose fixes are provided in <code>fixesForCompetitors</code> this holds
     *            <code>false</code>, any fixes previously stored for that competitor are removed, and the tail is
     *            deleted from the map (see {@link #removeTail(CompetitorWithBoatDTO)}); the new fixes are then added to the
     *            {@link #fixesByCompetitorIdsAsStrings} map, and a new tail will have to be constructed as needed (does not happen here). If
     *            this map holds <code>true</code>, {@link #mergeFixes(CompetitorWithBoatDTO, List, long)} is used to merge the
     *            new fixes from <code>fixesForCompetitors</code> into the {@link #fixesByCompetitorIdsAsStrings} collection, and the tail is
     *            left unchanged. <b>NOTE:</b> When a non-overlapping set of fixes is updated (<code>false</code>), this
     *            map's record for the competitor is <b>UPDATED</b> to <code>true</code> after the tail deletion and
     *            {@link #fixesByCompetitorIdsAsStrings} replacement has taken place. This helps in cases where this update is only one of two
     *            into which an original request was split (one quick update of the tail's head and another one for the
     *            longer tail itself), such that the second request that uses the <em>same</em> map will be considered
     *            having an overlap now, not leading to a replacement of the previous update originating from the same
     *            request.
     * @param detailTypeForFixes used to update {@link #detailTypesRequestedByCompetitorIdsAsStrings}
     * 
     */
    private void updateFixes(final CompetitorDTO competitor,
            final GPSFixDTOWithSpeedWindTackAndLegTypeIterable fixesToAddForCompetitor, final boolean mustClearCache,
            long timeForPositionTransitionMillis, DetailType detailTypeForFixes) {
        final List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = fixesByCompetitorIdsAsStrings.computeIfAbsent(competitor.getIdAsString(),
                c->{
                      final List<GPSFixDTOWithSpeedWindTackAndLegType> f = new ArrayList<>();
                      fixesByCompetitorIdsAsStrings.put(c, f);
                      return f;
                   });
        if (mustClearCache) {
            // clearing and then re-populating establishes the invariant that an extrapolated fix must be the last
            fixesForCompetitor.clear();
            detailTypesRequestedByCompetitorIdsAsStrings.put(competitor.getIdAsString(), detailTypeForFixes);
            // to re-establish the invariants for tails, firstShownFix and lastShownFix, we now need to remove
            // all points from the competitor's polyline and clear the entries in firstShownFix and lastShownFix
            clearTail(competitor);
            Util.addAll(fixesToAddForCompetitor, fixesForCompetitor);
            minDetailValueFixByCompetitorIdsAsStrings.remove(competitor.getIdAsString());
            maxDetailValueFixByCompetitorIdsAsStrings.remove(competitor.getIdAsString());
        } else {
            if (detailTypeForFixes != null && detailTypesRequestedByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != detailTypeForFixes) {
                GWT.log("WARNING: Inconsistent detail types when merging fixed for competitor "+competitor+
                        ". Got fixes with "+detailTypesRequestedByCompetitorIdsAsStrings.get(competitor.getIdAsString())+" so far but now received fixes with "+detailTypeForFixes);
            }
            mergeFixes(competitor, fixesToAddForCompetitor, timeForPositionTransitionMillis);
        }
    }

    /**
     * While updating the {@link #fixesByCompetitorIdsAsStrings} for <code>competitorDTO</code>, the invariants for {@link #tailsByCompetitorIdsAsStrings} and
     * {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings} are maintained: each time a fix is inserted and we have a tail
     * in {@link #tailsByCompetitorIdsAsStrings} for <code>competitorDTO</code>, the {@link #firstShownFixByCompetitorIdsAsStrings} record for
     * <code>competitorDTO</code> is incremented if it is greater than the insertion index, and the
     * {@link #lastShownFixByCompetitorIdsAsStrings} records for <code>competitorDTO</code> is incremented if is is greater than or equal to the
     * insertion index. This means, in particular, that when a fix is inserted exactly at the index that points to the
     * first fix shown so far, the fix inserted will become the new first fix shown. When inserting a fix exactly at
     * index {@link #lastShownFixByCompetitorIdsAsStrings}, the fix that so far was the last one shown remains the last one shown because in
     * this case, {@link #lastShownFixByCompetitorIdsAsStrings} will be incremented by one. If {@link #firstShownFixByCompetitorIdsAsStrings} &lt;=
     * <code>insertindex</code> &lt;= {@link #lastShownFixByCompetitorIdsAsStrings}, meaning that the fix is in the range of fixes shown in the
     * competitor's tail, the tail is adjusted by inserting the corresponding fix.
     * <p>
     * 
     * If the last fix so far was an {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated} fix, and the merge leads to a different
     * fix being the last one shown, the previously last fix that was extrapolated will be removed. This way, at most
     * one extrapolated fix is shown, avoiding jitter on the map as actual fixes are received that obsolete the
     * extrapolated ones.<p>
     * 
     * Precondition: {@link #hasFixesFor(CompetitorWithBoatDTO) hasFixesFor(competitorDTO)}<code>==true</code>
     * @param mergeThis
     *            If this list contains an {@link GPSFixDTOWithSpeedWindTackAndLegType#extrapolated extrapolated} fix, that fix must be the last in
     *            the list
     */
    private void mergeFixes(CompetitorDTO competitorDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable mergeThis, final long timeForPositionTransitionMillis) {
        List<GPSFixDTOWithSpeedWindTackAndLegType> intoThis = fixesByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
        final Integer firstShownFixForCompetitor = firstShownFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
        int indexOfFirstShownFix = firstShownFixForCompetitor == null ? -1 : firstShownFixForCompetitor;
        final Integer lastShownFixForCompetitor = lastShownFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
        int indexOfLastShownFix = lastShownFixForCompetitor == null ? -2 : lastShownFixForCompetitor;
        final Colorline tail = getTail(competitorDTO);
        int earliestMergeIndex = -1;
        final Comparator<GPSFixDTOWithSpeedWindTackAndLegType> fixByTimePointComparator = new Comparator<GPSFixDTOWithSpeedWindTackAndLegType>() {
            @Override
            public int compare(GPSFixDTOWithSpeedWindTackAndLegType o1, GPSFixDTOWithSpeedWindTackAndLegType o2) {
                return o1.timepoint.compareTo(o2.timepoint);
            }
        };
        for (GPSFixDTOWithSpeedWindTackAndLegType mergeThisFix : mergeThis) {
            int intoThisIndex = Collections.binarySearch(intoThis, mergeThisFix, fixByTimePointComparator);
            if (intoThisIndex < 0) {
                intoThisIndex = -intoThisIndex-1;
            }
            if (earliestMergeIndex == -1 || intoThisIndex < earliestMergeIndex) {
                earliestMergeIndex = intoThisIndex;
            }
            if (intoThisIndex < intoThis.size() && intoThis.get(intoThisIndex).timepoint.equals(mergeThisFix.timepoint)) {
                // exactly same time point; replace with fix from mergeThis unless the new fix is extrapolated and there is a later fix in intoThis;
                // in the (unlikely) case the existing non-extrapolated fix is replaced by an extrapolated one, the indices of the shown fixes
                // need according adjustments
                if (!mergeThisFix.extrapolated || intoThis.size() == intoThisIndex+1) {
                    intoThis.set(intoThisIndex, mergeThisFix);
                    if (tail != null && intoThisIndex >= indexOfFirstShownFix && intoThisIndex <= indexOfLastShownFix) { // false if first/last shown index is -1
                        tail.setAt(intoThisIndex - indexOfFirstShownFix, coordinateSystem.toLatLng(mergeThisFix.position));
                    }
                } else {
                    // extrapolated fix would be added one or more positions before the last fix in intoThis; instead,
                    // remove the fix at the respective index with the same time point and adjust indices:
                    intoThis.remove(intoThisIndex);
                    if (tail != null && intoThisIndex >= indexOfFirstShownFix && intoThisIndex <= indexOfLastShownFix) {
                        tail.removeAt(intoThisIndex - indexOfFirstShownFix);
                    }
                    {
                        boolean indicesChanged = false;
                        if (intoThisIndex < indexOfFirstShownFix) {
                            indexOfFirstShownFix--;
                            indicesChanged = true;
                        }
                        if (intoThisIndex <= indexOfLastShownFix) {
                            indexOfLastShownFix--;
                            indicesChanged = true;
                        }
                        if (indicesChanged) {
                            updateTailBoundaries(competitorDTO, indexOfFirstShownFix, indexOfLastShownFix);
                        }
                    }
                    // Make sure that minDetailValueFix and maxDetailValueFix still track the correct fixes
                    if (minDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString())) {
                        if (intoThisIndex < minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            minDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) - 1);
                        } else if (intoThisIndex == minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            // The fix with the highest value was removed so re-search
                            minDetailValueFixByCompetitorIdsAsStrings.remove(competitorDTO.getIdAsString());
                        }
                    }
                    if (maxDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString())) {
                        if (intoThisIndex < maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            maxDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) - 1);
                        } else if (intoThisIndex == maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            // The fix with the highest value was removed so re-search
                            maxDetailValueFixByCompetitorIdsAsStrings.remove(competitorDTO.getIdAsString());
                        }
                    }
                    intoThisIndex--;
                }
            } else {
                // insert the fix only if it is not extrapolated or if the extrapolated fix is to be appended to the end (including
                // being the only fix)
                if (!mergeThisFix.extrapolated || intoThisIndex == intoThis.size()) {
                    intoThis.add(intoThisIndex, mergeThisFix);
                    if (tail != null && intoThisIndex >= indexOfFirstShownFix && intoThisIndex <= indexOfLastShownFix) {
                        // fix inserted at a position currently visualized by tail
                        tail.insertAt(intoThisIndex - indexOfFirstShownFix, coordinateSystem.toLatLng(mergeThisFix.position));
                        // TODO bug5921 / bug5925: check if min/max value and adjust minDetailValueFix/maxDetailValueFix incrementally 
                    }
                    {
                        boolean indicesChanged = false;
                        if (intoThisIndex < indexOfFirstShownFix) {
                            indexOfFirstShownFix++;
                            indicesChanged = true;
                        }
                        if (intoThisIndex <= indexOfLastShownFix) {
                            indexOfLastShownFix++;
                            indicesChanged = true;
                        }
                        if (indicesChanged) {
                            updateTailBoundaries(competitorDTO, indexOfFirstShownFix, indexOfLastShownFix);
                        }
                    }
                    if (minDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString()) && intoThisIndex <= minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                        minDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) + 1);
                    }
                    if (maxDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString()) && intoThisIndex <= maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                        maxDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) + 1);
                    }
                    // If there is a fix prior to the one added and that prior fix was obtained by extrapolation, remove it now because
                    // extrapolated fixes can only be the last in the list
                    if (intoThisIndex > 0 && intoThis.get(intoThisIndex-1).extrapolated) {
                        intoThis.remove(intoThisIndex-1);
                        if (tail != null && intoThisIndex-1 >= indexOfFirstShownFix && intoThisIndex-1 <= indexOfLastShownFix) {
                            tail.removeAt(intoThisIndex-1 - indexOfFirstShownFix);
                        }
                        {
                            boolean indicesChanged = false;
                            if (intoThisIndex-1 < indexOfFirstShownFix) {
                                indexOfFirstShownFix--;
                                indicesChanged = true;
                            }
                            if (intoThisIndex-1 <= indexOfLastShownFix) {
                                indexOfLastShownFix--;
                                indicesChanged = true;
                            }
                            if (indicesChanged) {
                                updateTailBoundaries(competitorDTO, indexOfFirstShownFix, indexOfLastShownFix);
                            }
                        }
                        if (minDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString()) && intoThisIndex - 1 <= minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            minDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), minDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) - 1);
                        }
                        if (maxDetailValueFixByCompetitorIdsAsStrings.containsKey(competitorDTO.getIdAsString()) && intoThisIndex - 1 <= maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())) {
                            maxDetailValueFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), maxDetailValueFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) - 1);
                        }
                    }
                }
            }
        }
        if (earliestMergeIndex != -1) {
            lastSearchedFixByCompetitorIdsAsStrings.merge(competitorDTO.getIdAsString(), earliestMergeIndex, Math::min); // FIXME bug5921: this may end up being before the firstShownFix
        }
    }

    private void updateTailBoundaries(CompetitorDTO competitorDTO, int indexOfFirstShownFix, int indexOfLastShownFix) {
        if (indexOfFirstShownFix > indexOfLastShownFix ||
                (indexOfFirstShownFix < 0 && indexOfLastShownFix < 0)) {
            firstShownFixByCompetitorIdsAsStrings.remove(competitorDTO.getIdAsString());
            lastShownFixByCompetitorIdsAsStrings.remove(competitorDTO.getIdAsString());
        } else {
            firstShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfFirstShownFix);
            lastShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfLastShownFix);
        }
    }

    /**
     * If the tail starts before <code>from</code>, removes leading vertices from <code>tail</code> that are before
     * <code>from</code>. This is determined by using the {@link #firstShownFixByCompetitorIdsAsStrings} index which tells us where in
     * {@link #fixesByCompetitorIdsAsStrings} we find the sequence of fixes currently represented in the tail.
     * <p>
     * 
     * If the tail starts after <code>from</code>, vertices for those {@link #fixesByCompetitorIdsAsStrings} for <code>competitorDTO</code> at
     * or after time point <code>from</code> and before the time point of the first fix displayed so far in the tail and
     * before <code>to</code> are prepended to the tail.
     * <p>
     * 
     * Now to the end of the tail: if the existing tail's end exceeds <code>to</code>, the vertices in excess are
     * removed (aided by {@link #lastShownFixByCompetitorIdsAsStrings}). Otherwise, for the competitor's fixes starting at the tail's end up to
     * <code>to</code> are appended to the tail.
     * <p>
     * 
     * When this method returns, {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings} have been updated accordingly.
     * <p>
     * 
     * FIXME: I don't think this method handles empty tails correctly; firstShownFix/lastShownFix then won't hold a
     * record for competitorDTO, and nothing in here will properly fill the tail
     * <p>
     * FIXME: update to a non-overlapping time range probably won't work
     * <p>
     * 
     * Requirements:
     * <ul>
     * <li>handle a so far empty tail ({@code tail.getLength() == 0}, and {@link #firstShownFixByCompetitorIdsAsStrings}/{@link #lastShownFixByCompetitorIdsAsStrings}
     * not containing key {@code competitorDTO})</li>
     * <li>handle moving to a new {@code from/to} time range that does not overlap the current tail's time range</li>
     * <li>all fixes from {@link #getFixes(CompetitorDTO) getFixes(competitorDTO)} that are between {@code from/to}
     * (inclusive), and only those fixes, are visualized on the tail if tail exists</li>
     * <li>{@link #firstShownFixByCompetitorIdsAsStrings}/{@link #lastShownFixByCompetitorIdsAsStrings} afterwards reflect the new tail; in particular, if the tail is empty,
     * they both do not contain the key {@code competitorDTO}.</li>
     * </ul>
     * <p>
     * 
     * @param delayForTailChangeInMillis
     *            the time in milliseconds after which to actually draw the tail update, or <code>-1</code> to perform
     *            the update immediately
     * @param selectedDetailType
     *            for verifying against {@link #detailTypesRequestedByCompetitorIdsAsStrings}
     */
    protected void updateTail(final CompetitorDTO competitorDTO, final Date from, final Date to, final int delayForTailChangeInMillis, DetailType selectedDetailType) {
        Timer delayedOrImmediateExecutor = new Timer() {
            @Override
            public void run() {
                if (selectedDetailType != null && detailTypesRequestedByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString()) != selectedDetailType) {
                    GWT.log("WARNING: Detail type mismatch in updateTail: have "+detailTypesRequestedByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString())+" but caller expected "+selectedDetailType);
                }
                final Colorline tail = getTail(competitorDTO);
                if (tail != null) {
                    int vertexCount = tail.getLength();
                    final Integer firstShownFixForCompetitor = firstShownFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
                    final Integer lastShownFixForCompetitor = lastShownFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
                    if (firstShownFixForCompetitor == null) {
                        // empty tail; do a few consistency checks:
                        if (lastShownFixForCompetitor != null) {
                            GWT.log("Inconsistent lastShownFix for competitor "+competitorDTO+"; should have been null but was "+lastShownFixForCompetitor);
                        }
                        if (vertexCount != 0) {
                            throw new IllegalStateException("Inconsistent fistShownFix/lastShownFix for competitor "+competitorDTO+
                                    "; tail is empty, both should have been null but were "+firstShownFixForCompetitor+
                                    " and "+lastShownFixForCompetitor);
                        }
                        fillEmptyTail(competitorDTO, from, to);
                    } else {
                        if (lastShownFixForCompetitor == null) {
                            throw new IllegalStateException("Inconsistent lastShownFix for competitor "+competitorDTO+
                                    "; should have contained the competitor as key because firstShownFix did");
                        }
                        final List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = getFixes(competitorDTO);
                        // we have a non-empty tail, but there may be a gap between old and new; if so, clear tail and start over
                        if (!TimeRange.create(TimePoint.of(fixesForCompetitor.get(firstShownFixForCompetitor).timepoint),
                                             TimePoint.of(fixesForCompetitor.get(lastShownFixForCompetitor).timepoint)).
                                touches(TimeRange.create(TimePoint.of(from), TimePoint.of(to)))) {
                            clearTail(competitorDTO);
                            fillEmptyTail(competitorDTO, from, to);
                        } else {
                            // the time ranges of the non-empty tail and the desired time range from..to touch; adjust incrementally
                            int indexOfFirstShownFix = firstShownFixForCompetitor;
                            int indexOfLastShownFix = lastShownFixForCompetitor;
                            // remove fixes before what is now to be the beginning of the polyline:
                            while (vertexCount > 0 && fixesForCompetitor.get(indexOfFirstShownFix).timepoint.before(from)) {
                                tail.removeAt(0);
                                vertexCount--;
                                indexOfFirstShownFix++;
                                firstShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfFirstShownFix);
                            }
                            // now the polyline contains no more vertices representing fixes before "from";
                            // go back in time starting at indexOfFirstShownFix while the fixes are still at or after "from"
                            // and insert corresponding vertices into the polyline
                            while (indexOfFirstShownFix > 0
                                    && !fixesForCompetitor.get(indexOfFirstShownFix - 1).timepoint.before(from)) {
                                indexOfFirstShownFix--;
                                firstShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfFirstShownFix);
                                final GPSFixDTOWithSpeedWindTackAndLegType fix = fixesForCompetitor.get(indexOfFirstShownFix);
                                tail.insertAt(0, coordinateSystem.toLatLng(fix.position));
                                vertexCount++;
                            }
                            // now adjust the tail's end: remove excess vertices that are after "to"
                            while (vertexCount > 0 && fixesForCompetitor.get(indexOfLastShownFix).timepoint.after(to)) {
                                tail.removeAt(--vertexCount);
                                indexOfLastShownFix--;
                                lastShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfLastShownFix);
                            }
                            // now the polyline contains no more vertices representing fixes after "to";
                            // go forward in time starting at indexOfLastShownFix while the fixes are still at or before "to"
                            // and insert corresponding vertices into the polyline
                            while (indexOfLastShownFix < fixesForCompetitor.size() - 1
                                    && !fixesForCompetitor.get(indexOfLastShownFix + 1).timepoint.after(to)) {
                                indexOfLastShownFix++;
                                lastShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), indexOfLastShownFix);
                                final GPSFixDTOWithSpeedWindTackAndLegType fix = fixesForCompetitor.get(indexOfLastShownFix);
                                tail.insertAt(vertexCount++, coordinateSystem.toLatLng(fix.position));
                            }
                            // FIXME bug5921: what about lastSearchedFix?
                        }
                    }
                }
            }
        };
        runDelayedOrImmediately(delayedOrImmediateExecutor, delayForTailChangeInMillis);
    }

    /**
     * Assuming the {@link #getTail(CompetitorDTO) tail of competitorDTO} is empty, fills in all
     * fixes between {@code from} and {@code to} (inclusive) and adjusts {@link #firstShownFixByCompetitorIdsAsStrings}
     * and {@link #lastShownFixByCompetitorIdsAsStrings} accordingly. In particular, if no fix is inserted, the
     * {@code competitorDTO} key is removed from those two maps.
     */
    private void fillEmptyTail(CompetitorDTO competitorDTO, Date from, Date to) {
        int first = -1;
        int last = -1;
        final Colorline tail = getTail(competitorDTO);
        int vertexCount = tail.getLength();
        if (vertexCount != 0) {
            throw new IllegalStateException("Can call fillEmptyTail only for empty tails; the tail of competitor "+
                    competitorDTO+" contains "+vertexCount+" vertices");
        }
        final List<GPSFixDTOWithSpeedWindTackAndLegType> competitorFixes = getFixes(competitorDTO);
        if (competitorFixes != null) {
            GPSFixDTOWithSpeedWindTackAndLegType fix;
            int i;
            for (i=0; i<competitorFixes.size() && !(fix=competitorFixes.get(i)).timepoint.after(to); i++) {
                if (!fix.timepoint.before(from)) {
                    if (first == -1) {
                        first = i;
                        firstShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), first);
                    }
                    tail.insertAt(vertexCount++, coordinateSystem.toLatLng(fix.position));
                    // TODO bug5921: adjust min/max values as we visit all fixes constituting the new tail contents anyhow
                }
            }
            last = i-1;
        }
        if (last < 0 && first >= 0) {
            GWT.log("Inconsistency: last < 0 but first=="+first+" for competitor "+competitorDTO);
        }
        if (last != -1) {
            lastShownFixByCompetitorIdsAsStrings.put(competitorDTO.getIdAsString(), last);
        } else {
            tailRemoved(competitorDTO.getIdAsString());
        }
    }

    private void runDelayedOrImmediately(Timer runThis, final int delayForTailChangeInMillis) {
        if (delayForTailChangeInMillis == -1) {
            runThis.run();
        } else {
            runThis.schedule(delayForTailChangeInMillis);
        }
    }

    /**
     * Consistently removes the <code>competitor</code>'s tail from {@link #tailsByCompetitorIdsAsStrings} and from the map, and the corresponding position
     * data from {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings}.
     */
    protected void removeTail(String competitorIdAsString) {
        final Colorline removedTail = tailsByCompetitorIdsAsStrings.remove(competitorIdAsString);
        if (removedTail != null) {
            removedTail.setMap(null);
        }
        tailRemoved(competitorIdAsString);
    }

    /**
     * Removes the entry for {@code competitor} from {@link #firstShownFixByCompetitorIdsAsStrings}, {@link #lastShownFixByCompetitorIdsAsStrings},
     * {@link #minDetailValueFixByCompetitorIdsAsStrings}, and {@link #maxDetailValueFixByCompetitorIdsAsStrings}; to be called when the competitor's
     * tail has been cleared or entirely removed.
     */
    private void tailRemoved(String competitorIdAsString) {
        firstShownFixByCompetitorIdsAsStrings.remove(competitorIdAsString);
        lastShownFixByCompetitorIdsAsStrings.remove(competitorIdAsString);
        minDetailValueFixByCompetitorIdsAsStrings.remove(competitorIdAsString);
        maxDetailValueFixByCompetitorIdsAsStrings.remove(competitorIdAsString);
        lastSearchedFixByCompetitorIdsAsStrings.remove(competitorIdAsString);
    }
    
    /**
     * Leaves the tail on the map and empties its path {@link Polyline#getPath()}. Correspondingly, the
     * {@link #firstShownFixByCompetitorIdsAsStrings} and {@link #lastShownFixByCompetitorIdsAsStrings} entries for <code>competitor</code> are set to <code>-1</code>.
     */
    private void clearTail(CompetitorDTO competitor) {
        final Colorline tail = tailsByCompetitorIdsAsStrings.get(competitor.getIdAsString());
        if (tail != null) {
            tail.clear();
            tailRemoved(competitor.getIdAsString());
        }
    }

    /**
     * From {@link #earliestTimePointRequested} as well as the selection of {@code competitorsToShow}, computes the
     * from/to times for which to request GPS fixes from the server, per competitor. No update is performed here to
     * {@link #fixesByCompetitorIdsAsStrings}. The result guarantees that, when used in
     * {@link SailingServiceAsync#getBoatPositions(String, String, Map, Map, boolean, AsyncCallback)}, for each
     * competitor from {@code competitorsToShow} all fixes known by the server for that competitor starting at
     * <code>upTo-{@link #tailLengthInMilliSeconds}</code> and ending at <code>upTo</code> (exclusive) will be loaded
     * into the {@link #fixesByCompetitorIdsAsStrings} cache.
     * <p>
     * 
     * The {@link #earliestTimePointRequested} map is updated, assuming that the requests returned will be sent before
     * invoking this method again. The two {@link PositionRequest}s returned are added to the {@link #inFlightRequests}
     * set.
     * 
     * @return a pair of {@link PositionRequest} objects; the {@link Pair#getA() first} provides the parameters for a
     *         "quick" request, assuming that only a few positions (up to {@link #MAX_DURATION_FOR_QUICK_REQUESTS}) need
     *         to be fetched and where the call can be performed in a compound {@link GetRaceMapDataAction}; the
     *         {@link Pair#getB() second} is for a "slow" request that needs to fetch longer segments of tracks and that
     *         can be sent using {@link GetBoatPositionsAction}. None of the pair's components is {@code null}), but the
     *         {@link PositionRequest#getFromByCompetitorIdAsString()} and {@link PositionRequest#getToByCompetitorIdAsString()} results may be empty. The two
     *         requests are "entangled", in the sense that if the combined effect of the requests has to be that the
     *         cache for one or more competitors is to be cleared before updating the new positions, exactly the first
     *         of the two requests to process its response will carry out this clearing. It is important to reliably
     *         obtain the positions for both requests, so if the "quick" request should be dropped together with a
     *         compound {@link GetRaceMapDataAction}, at least the position-related part needs to be run, e.g., using
     *         a more specific {@link GetBoatPositionsAction} with the {@link TimeRangeActionsExecutor}. 
     */
    protected Pair<PositionRequest, PositionRequest> computeFromAndTo(Date upTo,
            Iterable<CompetitorDTO> competitorsToShow, long effectiveTailLengthInMilliseconds,
            long transitionTimeInMillis, DetailType detailType) {
        final TimePoint upToTimePoint = TimePoint.of(upTo);
        final TimePoint tailstart = upToTimePoint.minus(effectiveTailLengthInMilliseconds);
        final TimeRange quickTipTimeRange = TimeRange.create(upToTimePoint, upToTimePoint);
        final Set<String> mustClearCacheForTheseCompetitorIdsAsString = new HashSet<>();
        final TimeRange timeRangeNeeded = TimeRange.create(tailstart, upToTimePoint);
        final Map<String, TimeRange> timeRangesForQuickRequest = new HashMap<>();
        final Map<String, TimeRange> timeRangesForSlowRequest = new HashMap<>();
        for (final CompetitorDTO competitor : competitorsToShow) {
            final TimeRange timeRangeNotToRequestAgain = getTimeRangeNotToRequestAgain(competitor);
            // The cache must be cleared upon result processing if the detail type has changed to a different, non-null one,
            // or the timeRangeNeeded does not touch/overlap the timeRangeAlreadyRequested
            if (detailType != null && detailType != detailTypesRequestedByCompetitorIdsAsStrings.get(competitor.getIdAsString())
             || (timeRangeNotToRequestAgain != null && !timeRangeNeeded.touches(timeRangeNotToRequestAgain))) {
                mustClearCacheForTheseCompetitorIdsAsString.add(competitor.getIdAsString());
                ignoreResultsForCompetitorInPendingRequests(competitor);
                timeRangesForQuickRequest.put(competitor.getIdAsString(), quickTipTimeRange);
                timeRangesForSlowRequest.put(competitor.getIdAsString(), timeRangeNeeded);
                timeRangesRequestedByCompetitorIdAsString.put(competitor.getIdAsString(), timeRangeNeeded);
            } else {
                if (timeRangeNotToRequestAgain == null) {
                    // we need to ask for the full time range needed because there is no data in the cache yet; cache clearing not necessary
                    timeRangesForQuickRequest.put(competitor.getIdAsString(), quickTipTimeRange);
                    timeRangesForSlowRequest.put(competitor.getIdAsString(), timeRangeNeeded);
                    timeRangesRequestedByCompetitorIdAsString.put(competitor.getIdAsString(), timeRangeNeeded);
                } else {
                    final MultiTimeRange timeRangesToRequest = timeRangeNeeded.subtract(timeRangeNotToRequestAgain);
                    if (Util.size(timeRangesToRequest) > 1) {
                        GWT.log("Request for "+competitor+" exceeds cached region at start and end; requesting the full tail");
                        // We would need to ask for data on both ends of the cached contiguous track segment; for now we
                        // have no support for querying multiple segments for the same competitor in one round trip, and we
                        // don't want to make another round trip for this infrequent case (probably occurs only when tail length
                        // is extended in play/live mode beyond cached fixes). We will instead ask for the full segment and merge its
                        // fixes
                        timeRangesForQuickRequest.put(competitor.getIdAsString(), quickTipTimeRange);
                        timeRangesForSlowRequest.put(competitor.getIdAsString(), timeRangeNeeded);
                        timeRangesRequestedByCompetitorIdAsString.put(competitor.getIdAsString(), timeRangeNeeded);
                    } else {
                        if (!Util.isEmpty(timeRangesToRequest)) {
                            final TimeRange timeRangeToRequest = timeRangesToRequest.iterator().next();
                            // The single time range that is missing could be before or after the cached fixes.
                            // Anything before the cached fixes shall be loaded with the slow request:
                            if (timeRangeToRequest.startsBefore(timeRangeNotToRequestAgain)) {
                                timeRangesForSlowRequest.put(competitor.getIdAsString(), timeRangeToRequest);
                            } else {
                                // A segment to load after the cached fixes may need to be split into two:
                                // a quick segment with maximum length MAX_DURATION_FOR_QUICK_REQUESTS, and
                                // a slow segment with the remaining part of the time range to request.
                                if (timeRangeToRequest.getDuration().compareTo(MAX_DURATION_FOR_QUICK_REQUESTS) <= 0) {
                                    timeRangesForQuickRequest.put(competitor.getIdAsString(), timeRangeToRequest);
                                } else {
                                    timeRangesForQuickRequest.put(competitor.getIdAsString(), TimeRange.create(timeRangeToRequest.to().minus(MAX_DURATION_FOR_QUICK_REQUESTS), timeRangeToRequest.to()));
                                    timeRangesForSlowRequest.put(competitor.getIdAsString(), TimeRange.create(timeRangeToRequest.from(), timeRangeToRequest.to().minus(MAX_DURATION_FOR_QUICK_REQUESTS)));
                                }
                            }
                            timeRangesRequestedByCompetitorIdAsString.put(competitor.getIdAsString(), timeRangeToRequest.extend(timeRangesRequestedByCompetitorIdAsString.get(competitor.getIdAsString())));
                        }
                    }
                }
            }
            detailTypesRequestedByCompetitorIdsAsStrings.put(competitor.getIdAsString(), detailType);
        }
        final PositionRequest quick = new PositionRequest(timeRangesForQuickRequest, mustClearCacheForTheseCompetitorIdsAsString, detailType, transitionTimeInMillis);
        final PositionRequest slow = new PositionRequest(timeRangesForSlowRequest, quick); // entangle with quick request
        inFlightRequests.add(quick);
        inFlightRequests.add(slow);
        return new Pair<>(quick, slow);
    }

    /**
     * Roughly speaking, this method finds the time range represented by the contiguous track segment cached for
     * {@code competitor}.
     * <p>
     * 
     * This first looks at what has been <em>requested</em> through
     * {@link #computeFromAndTo(Date, Iterable, long, long, DetailType)} with the {@link PositionRequest} objects
     * returned from it. These time ranges are stored in {@link #timeRangesRequestedByCompetitorIdAsString}, and this is what will be returned
     * if any in-flight request will clear the {@code competitor}'s cache.
     * <p>
     * 
     * If no in-flight request clears the {@code competitor}'s cache, and if no {@link #inFlightRequests in-flight
     * request} ends at the end of the time range requested overall for {@code competitor} and doesn't ignore the fixes
     * for that competitor then we take into account the possibility that new fixes may have been delivered late to the
     * server after we last asked for them. In this case, this method will return a time range starting at the beginning
     * of the time range requested so far for {@code competitor}, ending at the last fix received for that competitor
     * (instead of the last time point <em>requested</em>). This way, fixes delivered late can be expected to still make
     * it into the cache.
     */
    private TimeRange getTimeRangeNotToRequestAgain(CompetitorDTO competitor) {
        final TimeRange timeRangeRequestedForCompetitor = timeRangesRequestedByCompetitorIdAsString.get(competitor.getIdAsString());
        boolean inFlightRequestWillClearCacheForCompetitor = false;
        for (final PositionRequest inFlightRequest : inFlightRequests) {
            if (inFlightRequest.isMustClearCacheForCompetitor(competitor)) {
                inFlightRequestWillClearCacheForCompetitor = true;
            }
            final TimePoint competitorTimeRangeEnd = inFlightRequest.getToTimepoint(competitor);
            if (competitorTimeRangeEnd != null && competitorTimeRangeEnd.equals(timeRangeRequestedForCompetitor.to())) {
                return timeRangeRequestedForCompetitor; // found an in-flight request ending at the last time point requested; we hope it delivers fixes up to there
            }
        }
        final TimeRange result;
        if (inFlightRequestWillClearCacheForCompetitor) {
            result = timeRangeRequestedForCompetitor;
        } else {
            final List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = getFixes(competitor);
            if (fixesForCompetitor == null) {
                result = timeRangeRequestedForCompetitor;
            } else {
                final TimePoint timePointOfLastFix = TimePoint.of(getTimepointOfLastNonExtrapolated(fixesForCompetitor));
                if (timePointOfLastFix == null) {
                    result = timeRangeRequestedForCompetitor;
                } else {
                    result = TimeRange.create(timeRangeRequestedForCompetitor.from(), timePointOfLastFix);
                }
            }
        }
        return result;
    }

    private void ignoreResultsForCompetitorInPendingRequests(CompetitorDTO competitor) {
        for (final PositionRequest inFlightRequest : inFlightRequests) {
            inFlightRequest.ignoreFixesFor(competitor);
        }
    }

    private Date getTimepointOfLastNonExtrapolated(List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor) {
        if (!fixesForCompetitor.isEmpty()) {
            for (ListIterator<GPSFixDTOWithSpeedWindTackAndLegType> fixIter = fixesForCompetitor.listIterator(fixesForCompetitor.size() - 1); fixIter
                    .hasPrevious();) {
                GPSFixDTOWithSpeedWindTackAndLegType fix = fixIter.previous();
                if (!fix.extrapolated) {
                    return fix.timepoint;
                }
            }
        }
        return null;
    }


    /**
     * Determines whether an index for a competitor lies in [{@link #firstShownFixByCompetitorIdsAsStrings}, {@link #lastShownFixByCompetitorIdsAsStrings}], i.e. the
     * index is included in the competitors tail.
     * 
     * @param competitor
     *            {@link CompetitorDTO} specifying the tail.
     * @param index
     *            {@code int} the index in question.
     * @return {@code boolean} {@code true} if {@code index} is shown.
     */
    protected boolean isIndexShown(CompetitorDTO competitor, int index) {
        final boolean result;
        if (getFirstShownFix(competitor) == null || lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) == null) {
            result = false;
        } else {
            final int first = firstShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
            final int last  = lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
            result = index >= first && index <= last;
        }
        return result;
    }

    /**
     * Searches a competitor's shown fixes (firstShownFix to lastShownFix but usually a smaller range since many fixes
     * have already been searched by a previous iteration) for the smallest and largest detailValue. Only when the
     * fix that previously had the minimum/maximum value is no longer shown on the tail, a new search across the
     * entire visible tail is required.
     * <p>
     * 
     * FIXME bug5921: this seems to assume that if additional fixes have been loaded then they are newer than all fixes
     * loaded before; see the use of lastSearchedFix for the initialization of startIndex below; but this may be false
     * as we may move backwards in time... We would instead need to know the TimeRange searched for min/max values per
     * competitor, subtract that from the shown tail's TimeRange and scan whatever remains, which may be before and/or
     * after the interval searched so far.
     * 
     * @param competitor
     *            {@link CompetitorDTO} competitor whose tail to search in.
     */
    protected void searchMinMaxDetailValue(CompetitorDTO competitor) {
        Integer startIndex = null;
        double min = 0;
        boolean minSet = false;
        int minIndex = -1;
        // Check if the previously found minimum is still shown and use its value
        if (minDetailValueFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString())) {
            int minFix = minDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
            if (minFix < 0 || !isIndexShown(competitor, minFix)) {
                minDetailValueFixByCompetitorIdsAsStrings.remove(competitor.getIdAsString());
                startIndex = getFirstShownFix(competitor);
            } else if (minFix < fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size() && fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(minFix).detailValue != null) {
                min = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(minFix).detailValue;
                minSet = true;
            }
        }
        double max = 0;
        boolean maxSet = false;
        int maxIndex = -1;
        // Check if the previously found maximum is still shown and use its value
        if (maxDetailValueFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString())) {
            int maxFix = maxDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
            if (maxFix < 0 || !isIndexShown(competitor, maxFix)) {
                maxDetailValueFixByCompetitorIdsAsStrings.remove(competitor.getIdAsString());
                if (startIndex == null) {
                    startIndex = getFirstShownFix(competitor);
                } else if (getFirstShownFix(competitor) != null) {
                    startIndex = Math.min(startIndex, getFirstShownFix(competitor));
                }
            } else if (maxFix < fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size() && fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(maxFix).detailValue != null) {
                max = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(maxFix).detailValue;
                maxSet = true;
            }
        }
        // If the startIndex has not been reset to the beginning of the shown range because the min/max value has just
        // left shown range it will now be set to the first not already searched index
        if (startIndex == null) {
            if (lastSearchedFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString())) {
                startIndex = lastSearchedFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) + 1; // FIXME bug5921: what if, e.g., the tail length was increased and only older fixes were added to the tail? Wouldn't we then have to search in those older fixes? mergeFixes(...) may set lastSearchedFix to an index less than firstShownFix
            }
            if (startIndex == null || !isIndexShown(competitor, startIndex)) {
                startIndex = getFirstShownFix(competitor) != null && getFirstShownFix(competitor) != -1
                        ? getFirstShownFix(competitor) : 0;
            }
        }
        if (startIndex < 0) {
            // If a tail is present but has no path getFirstShownFix will return -1
            if (fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size() == 0) return;
            startIndex = 0;
        }
        int endIndex = lastShownFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString()) && lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != null
                    && lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != null && lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != -1 ?
                    lastShownFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) : fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size() - 1;
        List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString());
        for (int i = startIndex; i <= endIndex; i++) {
            final Double value = fixesForCompetitor.get(i).detailValue;
            if (value != null) {
                if (!minSet) {
                    min = value;
                    minIndex = i;
                    minSet = true;
                }
                if (!maxSet) {
                    max = value;
                    maxIndex = i;
                    maxSet = true;
                }
                if (value <= min) {
                    min = value;
                    minIndex = i;
                }
                if (value >= max) {
                    max = value;
                    maxIndex = i;
                }
            }
        }
        if (minIndex > -1) {
            minDetailValueFixByCompetitorIdsAsStrings.put(competitor.getIdAsString(), minIndex);
        }
        if (maxIndex > -1) {
            maxDetailValueFixByCompetitorIdsAsStrings.put(competitor.getIdAsString(), maxIndex);
        }
        lastSearchedFixByCompetitorIdsAsStrings.put(competitor.getIdAsString(), endIndex); // FIXME bug5921: this makes lastSearchedFix inclusive; however, mergeFixes(...) seems to assume it's exclusive, setting it to the minimum insert index for new merged fixes
    }

    /**
     * Resets the search so that the next iteration will start from the beginning.
     */
    protected void resetDetailValueSearch() {
        lastSearchedFixByCompetitorIdsAsStrings.clear();
        minDetailValueFixByCompetitorIdsAsStrings.clear();
        maxDetailValueFixByCompetitorIdsAsStrings.clear();
    }

    /**
     * Updates the fleet wide {@link #detailValueBoundaries} with the current maximum and minimum detailValues. To do
     * so, each competitor's (in parameter {@code competitors}) tail will be searched and then the maximum and minimum
     * search results will be collected. The findings are recorded as a side effect into {@link #minDetailValueFixByCompetitorIdsAsStrings} and
     * {@link #maxDetailValueFixByCompetitorIdsAsStrings}. Finally {@link #detailValueBoundaries} will be
     * {@link ValueRangeFlexibleBoundaries#setMinMax(double, double) updated}.
     * 
     * @param competitors
     *            {@link Iterable}{@code <}{@link CompetitorDTO}{@code >} containing all competitors to include in the
     *            search.
     */
    protected void updateDetailValueBoundaries(Iterable<CompetitorDTO> competitors) {
        try {
        double min = 0;
        boolean minSet = false;
        double max = 0;
        boolean maxSet = false;
        for (CompetitorDTO competitor : competitors) {
            searchMinMaxDetailValue(competitor);
            // Find minimum value across all boats
            if (minDetailValueFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString()) && minDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != null && minDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != -1) {
                int index = minDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
                if (!fixesByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString()) || fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()) == null || index >= fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size()) {
                    minDetailValueFixByCompetitorIdsAsStrings.put(competitor.getIdAsString(), -1);
                } else {
                    final GPSFixDTOWithSpeedWindTackAndLegType competitorFix = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(index);
                    if (!minSet || competitorFix.detailValue != null && competitorFix.detailValue < min) {
                        min = competitorFix.detailValue;
                        minSet = true;
                    }
                }
            }
            // Find maximum value across all boats
            if (maxDetailValueFixByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString()) && maxDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != null && maxDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString()) != -1) {
                int index = maxDetailValueFixByCompetitorIdsAsStrings.get(competitor.getIdAsString());
                if (!fixesByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString()) || fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()) == null || index >= fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).size()) {
                    maxDetailValueFixByCompetitorIdsAsStrings.put(competitor.getIdAsString(), -1);
                } else {
                    final GPSFixDTOWithSpeedWindTackAndLegType competitorFix = fixesByCompetitorIdsAsStrings.get(competitor.getIdAsString()).get(index);
                    if (!maxSet || competitorFix.detailValue != null && competitorFix.detailValue > max) {
                        max = competitorFix.detailValue;
                        maxSet = true;
                    }
                }
            }
        }
        // If possible update detailValueBoundaries
        if (minSet && maxSet) {
            detailValueBoundaries.setMinMax(min, max);
        }
        } catch (NullPointerException npe) {
            GWT.log("NPE: "+npe.getMessage());
        }
    }
    
    protected ValueRangeFlexibleBoundaries getDetailValueBoundaries() {
        return detailValueBoundaries;
    }

    /**
     * Sets the {@link ValueRangeFlexibleBoundaries} to use for tracking the minimum and maximum detail values.
     * @param boundaries {@link ValueRangeFlexibleBoundaries} to set {@link #detailValueBoundaries} to.
     */
    protected void setDetailValueBoundaries(ValueRangeFlexibleBoundaries boundaries) {
        detailValueBoundaries = boundaries;
    }

    /**
     * Gets the detail value at a specific index in a competitors tail.
     * 
     * @param competitorDTO
     *            {@link CompetitorDTO} specifying the competitor.
     * @param fixIndexIntoTail
     *            {@code int} specifying the index, relative to the start of the visual tail
     * @return {@code null} if {@code index} is out of bounds or if a detail value cannot be found. Otherwise returns a
     *         {@link Double} of the respective value.
     */
    protected Double getDetailValueAt(CompetitorDTO competitorDTO, int fixIndexIntoTail) {
        final Integer firstShownFixForCompetitor = firstShownFixByCompetitorIdsAsStrings.get(competitorDTO.getIdAsString());
        int indexOfFirstShownFix = firstShownFixForCompetitor == null ? -1 : firstShownFixForCompetitor;
        try {
            return getFixes(competitorDTO).get(indexOfFirstShownFix + fixIndexIntoTail).detailValue;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Clears all tail data, removing them from the map and from this object's internal structures. GPS fixes remain
     * cached. Immediately after this call, {@link #getTail(CompetitorWithBoatDTO)} will return <code>null</code> for all
     * competitors. Tails will need to be created (again) using
     * {@link #createTailAndUpdateIndices(CompetitorWithBoatDTO, Date, Date, TailFactory, DetailType)}.
     */
    protected void clearTails() {
        for (final Colorline tail : tailsByCompetitorIdsAsStrings.values()) {
            tail.clear();
        }
        tailsByCompetitorIdsAsStrings.clear();
        firstShownFixByCompetitorIdsAsStrings.clear();
        lastShownFixByCompetitorIdsAsStrings.clear();
        resetDetailValueSearch();
    }

    /**
     * Tells whether a tail currently exists for the {@code competitor}.
     */
    public boolean hasTail(CompetitorDTO competitor) {
        return tailsByCompetitorIdsAsStrings.containsKey(competitor.getIdAsString());
    }
}
