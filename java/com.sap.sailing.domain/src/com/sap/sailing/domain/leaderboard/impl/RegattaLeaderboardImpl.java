package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.RaceColumnInSeriesImpl;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.leaderboard.HasCourseAreasListener;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.metering.CPUMeter;

/**
 * A leaderboard that is based on the definition of a {@link Regatta} with its {@link Series} and {@link Fleet}. The regatta
 * leaderboard listens to its {@link Regatta} as a {@link RaceColumnListener} and forwards all link/unlink events for
 * {@link TrackedRace}s being linked to / unlinked from race columns to all {@link RaceColumnListener}s subscribed with
 * this leaderboard.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RegattaLeaderboardImpl extends AbstractLeaderboardImpl implements RegattaLeaderboard {
    private static final long serialVersionUID = 2370461218294770084L;
    private final Regatta regatta;
    
    public RegattaLeaderboardImpl(Regatta regatta, ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        super(resultDiscardingRule);
        this.regatta = regatta;
        regatta.addRaceColumnListener(this);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }
    
    @Override
    public Regatta getRegatta() {
        return regatta;
    }
    
    @Override
    public CPUMeter getCPUMeter() {
        return getRegatta().getCPUMeter();
    }
    
    public static String getLeaderboardNameForRegatta(Regatta regatta) {
        return regatta.getName();
    }
    
    @Override
    public String getName() {
        return getLeaderboardNameForRegatta(getRegatta());
    }

    @Override
    public Iterable<RaceColumn> getRaceColumns() {
        @SuppressWarnings("unchecked") // the iterable is read-only, so no problem to case <? extends RaceColumn> to RaceColumn
        Iterable<RaceColumn> raceColumns = (Iterable<RaceColumn>) getRegatta().getRaceColumns();
        return raceColumns;
    }

    @Override
    public RaceColumnInSeries getRaceColumnByName(String columnName) {
        return (RaceColumnInSeriesImpl) super.getRaceColumnByName(columnName);
    }

    @Override
    public ScoringScheme getScoringScheme() {
        return regatta.getScoringScheme();
    }

    @Override
    public Iterable<CourseArea> getCourseAreas() {
        return regatta.getCourseAreas();
    }
    
    @Override
    public void addCourseAreaChangeListener(HasCourseAreasListener listener) {
        regatta.addCourseAreaChangeListener(listener);
    }

    @Override
    public void removeCourseAreaChangeListener(HasCourseAreasListener listener) {
        regatta.removeCourseAreaChangeListener(listener);
    }

    /**
     * If the regatta' series {@link Regatta#definesSeriesDiscardThresholds() define} their own result discarding rules, this leaderboard uses
     * a composite result discarding rule of type {@link PerSeriesResultDiscardingRuleImpl} that contains discards within each series according
     * to the series' discarding rules. Otherwise, the default cross-leaderboard result discarding rule is obtained from the super class
     * implementation.
     */
    @Override
    public ResultDiscardingRule getResultDiscardingRule() {
        if (regatta.definesSeriesDiscardThresholds()) {
            return new PerSeriesResultDiscardingRuleImpl(regatta);
        } else {
            return super.getResultDiscardingRule();
        }
    }
    
    /**
     * Delegates to {@link Regatta#getAllCompetitors()} which is expected to deliver all competitors from this
     * leaderboard's regatta which includes those belonging to {@link RaceDefinition races}
     * {@link Regatta#getAllRaces() belonging to the regatta} as well as competitors listed on the regatta's
     * {@link RegattaLog}.
     */
    @Override
    public Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered() {
        return regatta.getAllCompetitorsWithRaceDefinitionsConsidered();
    }
    
    @Override
    public IsRegattaLike getRegattaLike() {
        return regatta;
    }
    
    @Override
    public LeaderboardType getLeaderboardType() {
        return LeaderboardType.RegattaLeaderboard;
    }

    @Override
    public BoatClass getBoatClass() {
        return getRegatta().getBoatClass();
    }

    @Override
    public CompetitorProviderFromRaceColumnsAndRegattaLike getOrCreateCompetitorsProvider() {
        return getRegatta().getOrCreateCompetitorsProvider();
    }
    
    public void setFleetsCanRunInParallelToTrue() {
        this.getRegattaLike().setFleetsCanRunInParallelToTrue();
    }
}
