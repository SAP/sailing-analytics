package com.sap.sailing.domain.base;

import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.HasCourseAreas;
import com.sap.sailing.domain.leaderboard.HasRaceColumnsAndRegattaLike;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.ranking.RankingMetricsFactory;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.util.RegattaUtil;
import com.sap.sse.common.NamedWithID;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.metering.HasCPUMeter;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * The name shall be unique across all regattas tracked concurrently. In particular, if you want to keep apart regattas
 * in different boat classes, make sure the boat class name becomes part of the regatta name.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Regatta
        extends NamedWithID, IsRegattaLike, HasRaceColumnsAndRegattaLike, WithQualifiedObjectIdentifier, HasCPUMeter, HasCourseAreas {

    /**
     * As taken from the Racing Rules of Sailing:
     * <p>
     * <em>Zone</em> The area around a mark within a distance of three hull lengths of the boat nearer to it. A boat is
     * in the zone when any part of her hull is in the zone.
     */
    static final double DEFAULT_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS = 3;

    ScoringScheme getScoringScheme();

    /**
     *  @return the (optional) start date of this regatta 
     */
    TimePoint getStartDate();

    void setStartDate(TimePoint startDate);

    /**
     *  @return the (optional) end date of this regatta 
     */
    TimePoint getEndDate();

    void setEndDate(TimePoint startDate);

    /**
     * Gets the course areas that the races of this {@link Regatta} are expected to be run on. This can, e.g., be used
     * to implement a filter when retrieving regattas from an event.
     * 
     * @return the {@link CourseArea} objects on which races of this regatta may run; always valid, never
     *         {@code null}, but may be empty; callers need to {@code synchronize} on the object returned
     *         if they want to iterate.
     */
    @Override
    Iterable<CourseArea> getCourseAreas();

    /**
     * Sets the course areas, telling where races in this regatta can be sailed. Replaces the course areas set
     * so far.
     */
    void setCourseAreas(Iterable<CourseArea> newCourseAreas);

    /**
     * Gets the {@link RegattaConfiguration} associated with this {@link Regatta}'s races.
     */
    RegattaConfiguration getRegattaConfiguration();

    /**
     * Sets the {@link RegattaConfiguration} associated with this {@link Regatta}'s races.
     * 
     * @param configuration
     */
    void setRegattaConfiguration(RegattaConfiguration configuration);

    /**
     * A regatta consists of one or more series.
     * 
     * @return an unmodifiable iterable sequence of the series of which this regatta consists.
     */
    Iterable<? extends Series> getSeries();

    /**
     * Adds the provided series to this regatta if a series with the name does not exist already.
     */
    void addSeries(Series series);

    /**
     * @return the first series from {@link #getSeries} whose {@link Series#getName() name} equals
     *         <code>seriesName<code>,
     * or <code>null</code> if no such series exists
     */
    Series getSeriesByName(String seriesName);

    /**
     * Please note that the {@link RaceDefinition}s of the {@link Regatta} are not necessarily in sync with the
     * {@link TrackedRace}s of the {@link TrackedRegatta} whose {@link TrackedRegatta#getRegatta() regatta} is this
     * regatta. For example, it may be the case that a {@link RaceDefinition} is returned by this method for which no
     * {@link TrackedRace} exists in the corresponding {@link TrackedRegatta}. This could be the case, e.g., during the
     * initialization of the tracker as well as during removing a race from the server.
     * <p>
     */
    Iterable<RaceDefinition> getAllRaces();

    /**
     * Please note that the set of {@link RaceDefinition}s contained by this regatta may not match up with the
     * {@link TrackedRace}s of the {@link TrackedRegatta} corresponding to this regatta. See also {@link #getAllRaces()}
     * .
     * 
     * @return <code>null</code>, if this regatta does not contain a race (see {@link #getAllRaces}) whose
     *         {@link RaceDefinition#getName()} equals <code>raceName</code>
     */
    RaceDefinition getRaceByName(String raceName);

    BoatClass getBoatClass();

    Iterable<Competitor> getAllCompetitors();

    /**
     * Same as {@link #getAllCompetitors()}, only that additionally the method returns as a first element of a pair
     * which {@link RaceDefinition}s' {@link RaceDefinition#getCompetitors() competitors} were used in assembling the
     * result.
     */
    Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered();

    Iterable<Boat> getAllBoats();

    /**
     * Will remove the series from this regatta. Will also call {@link RaceColumn#removeRaceIdentifier(Fleet)} to make
     * sure that all raceLogs and race associations get removed for all race columns in this series.
     */
    void removeSeries(Series series);

    void addRace(RaceDefinition race);

    void removeRace(RaceDefinition raceDefinition);

    void addRegattaListener(RegattaListener listener);

    void removeRegattaListener(RegattaListener listener);

    RegattaIdentifier getRegattaIdentifier();

    /**
     * Regattas may be constructed as implicit default regattas in which case they won't need to be stored durably and
     * don't contain valuable information worth being preserved; or they are constructed explicitly with series and race
     * columns in which case this data needs to be protected. This flag indicates whether the data of this regatta needs
     * to be maintained persistently.
     */
    boolean isPersistent();

    void addRaceColumnListener(RaceColumnListener listener);

    void removeRaceColumnListener(RaceColumnListener listener);

    /**
     * @return whether this regatta defines its local per-series result discarding rules; if so, any leaderboard based
     *         on the regatta has to respect this and has to use a result discarding rule implementation that keeps
     *         discards local to each series rather than spreading them across the entire leaderboard.
     */
    boolean definesSeriesDiscardThresholds();
    
    /**
     * Defines how this regatta is to be ranked. By default, a regatta would be ranked by one-design rules (first boat across
     * the line wins). Other ranking metrics could define handicaps such as time factors ("time-on-time") or time allowances
     * per distance sailed ("time-on-distance). This method returns a function that can, given a tracked race, construct the
     * ranking metric in the context of that race.
     */
    RankingMetricConstructor getRankingMetricConstructor();

    RegattaAndRaceIdentifier getRaceIdentifier(RaceDefinition race);

    /**
     * Define the value which would be multipled by hull length from {@link BoatClass}. 
     * Next the calculated value {@link RegattaUtil} would be used to fill out radius of buoy on race map setting.
     */
    public Double getBuoyZoneRadiusInHullLengths();

    public void setBuoyZoneRadiusInHullLengths(Double buoyZoneRadiusInHullLengths);

    /**
     * When there is no race committee app in place and no operator is managing the race start times for this regatta,
     * start times can optionally be inferred from the start mark passings by the {@link TrackedRace}s in this regatta.
     * The default for this is <code>true</code>, particularly because the race committee app has not been used for all
     * races, and start times need to be inferred for those.
     * <p>
     * 
     * This option should only be set to <code>false</code> if the race log is maintained thorougly for this regatta,
     * either by a well-working race committee app or by an on-shore operator using the administration console to enter
     * start times for all races of this regatta consistently.
     * <p>
     * 
     * Note that the effects of setting this option to <code>false</code> are severe when no start times are maintained.
     * The races for which then no start time is entered will not be scored at all.
     * <p>
     * 
     * A discussion of this subject can also be found at <a
     * href="http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=1994"
     * >http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=1994</a>.
     */
    boolean useStartTimeInference();

    /**
     * Updates the flag deciding whether to use start time inference. See {@link #useStartTimeInference()}.
     */
    void setUseStartTimeInference(boolean useStartTimeInference);

    RaceExecutionOrderProvider getRaceExecutionOrderProvider();

    /**
     * All races in this regatta use this same ranking metric
     */
    default RankingMetrics getRankingMetricType() {
        return RankingMetricsFactory.getForClass(getRankingMetricConstructor().apply(/* trackedRace */ null).getClass());
    }

    /**
     * When a regatta has well-managed {@link TrackedRace#getStartOfRace() start} and
     * {@link TrackedRace#getFinishedTime() finish times} it can make sense to drive the tracking infrastructure based
     * on these times. This may include automatically starting the tracking for a race a certain number of minutes
     * before the race starts, and finishing the tracking some time after the race has finished. This capability can be
     * activated using this flag. Tracking connectors can optionally evaluate it and take measures to drive their
     * {@link RaceTracker} and adjust start and end of tracking times accordingly.
     * <p>
     * 
     * See also
     * <a href="https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3588">https://bugzilla.sapsailing.com/bugzilla/
     * show_bug.cgi?id=3588</a>.
     */
    boolean isControlTrackingFromStartAndFinishTimes();
    
    /**
     * @see #isControlTrackingFromStartAndFinishTimes()
     */
    void setControlTrackingFromStartAndFinishTimes(boolean controlTrackingFromStartAndFinishTimes);
    
    /**
     * Tells whether in the scope of this regatta tracking of a race shall automatically be re-started if the competitor
     * registrations change. Removing the {@link TrackedRace tracked races} and {@link RaceDefinition}s affected is then
     * required because the {@link RaceDefinition#getCompetitors() set of competitors of a race} is modeled to be
     * immutable. As the set of competitors that are part of a tracked race is provided by the tracking connector, the
     * responsibility for adhering to this flag, which rather expresses a request than describes a fact, lies with the
     * respective connectors.
     */
    boolean isAutoRestartTrackingUponCompetitorSetChange();
    
    /**
     * @see #isAutoRestartTrackingUponCompetitorSetChange()
     */
    void setAutoRestartTrackingUponCompetitorSetChange(boolean autoRestartTrackingUponCompetitorSetChange);

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getName());
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String regattaName) {
        return new TypeRelativeObjectIdentifier(regattaName);
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(RegattaName regattaName) {
        return new TypeRelativeObjectIdentifier(regattaName.getRegattaName());
    }

    @Override
    default HasPermissions getPermissionType() {
        return SecuredDomainType.REGATTA;
    }

    /**
     * get secret for registration link of open regattas.
     * @return secret to append on regisration URL
     */
    String getRegistrationLinkSecret();

    /**
     * set secret for registration link for a regatta to be appended to the URL.
     * 
     * @param registrationLinkSecret
     *            secret string
     */
    void setRegistrationLinkSecret(String registrationLinkSecret);

    void setCompetitorRegistrationType(CompetitorRegistrationType competitorRegistrationType);

}
