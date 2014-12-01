package com.sap.sailing.domain.base;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sse.common.Named;
import com.sap.sse.common.WithID;

/**
 * The name shall be unique across all regattas tracked concurrently. In particular, if you want to
 * keep apart regattas in different boat classes, make sure the boat class name becomes part of the
 * regatta name.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Regatta extends Named, WithID {
    ScoringScheme getScoringScheme();
    
    /**
     * Gets the course area for all races of this {@link Regatta}.
     * @return the {@link CourseArea} object.
     */
    CourseArea getDefaultCourseArea();

    /**
     * Sets the course area for all races of this {@link Regatta}.
     * @param newCourseArea {@link CourseArea} to be set.
     */
    void setDefaultCourseArea(CourseArea newCourseArea);

    /**
     * Gets the {@link RegattaConfiguration} associated with this {@link Regatta}'s races.
     */
    RegattaConfiguration getRegattaConfiguration();
    
    /**
     * Sets the {@link RegattaConfiguration} associated with this {@link Regatta}'s races.
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
     * {@link TrackedRace}s of the {@link TrackedRegatta} whose {@link TrackedRegatta#getRegatta() regatta} is this regatta.
     * For example, it may be the case that a {@link RaceDefinition} is returned by this method for which no
     * {@link TrackedRace} exists in the corresponding {@link TrackedRegatta}. This could be the case, e.g., during
     * the initialization of the tracker as well as during removing a race from the server.<p>
     */
    Iterable<RaceDefinition> getAllRaces();
    
    /**
     * Please note that the set of {@link RaceDefinition}s contained by this regatta may not match up with the 
     * {@link TrackedRace}s of the {@link TrackedRegatta} corresponding to this regatta. See also {@link #getAllRaces()}.
     * 
     * @return <code>null</code>, if this regatta does not contain a race (see {@link #getAllRaces}) whose
     * {@link RaceDefinition#getName()} equals <code>raceName</code>
     */
    RaceDefinition getRaceByName(String raceName);
    
    BoatClass getBoatClass();
    
    Iterable<Competitor> getCompetitors();
    
    /**
     * Will remove the series from this regatta. Will also call {@link RaceColumn#removeRaceIdentifier(Fleet)} to
     * make sure that all raceLogs and race associations get removed for all race columns in this series.
     */
    void removeSeries(Series series);

    void addRace(RaceDefinition race);

    void removeRace(RaceDefinition raceDefinition);
 
    void addRegattaListener(RegattaListener listener);
    
    void removeRegattaListener(RegattaListener listener);

    RegattaIdentifier getRegattaIdentifier();

    /**
     * Regattas may be constructed as implicit default regattas in which case they won't need to be stored
     * durably and don't contain valuable information worth being preserved; or they are constructed explicitly
     * with series and race columns in which case this data needs to be protected. This flag indicates whether
     * the data of this regatta needs to be maintained persistently.
     */
    boolean isPersistent();

    void addRaceColumnListener(RaceColumnListener listener);

    void removeRaceColumnListener(RaceColumnListener listener);
    
    /**
     * @return whether this regatta defines its local per-series result discarding rules; if so, any leaderboard based
     *         on the regatta has to respect this and has to use a result discarding rule implementation that
     *         keeps discards local to each series rather than spreading them across the entire leaderboard.
     */
    boolean definesSeriesDiscardThresholds();

    RegattaAndRaceIdentifier getRaceIdentifier(RaceDefinition race);
    
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

    /**
     * @return The RegattaLog associated with this regatta.
     */
    RegattaLog getRegattaLog();
}
