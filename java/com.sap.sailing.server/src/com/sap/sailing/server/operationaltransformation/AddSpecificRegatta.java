package com.sap.sailing.server.operationaltransformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.RankingMetrics;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sse.common.TimePoint;

public class AddSpecificRegatta extends AbstractAddRegattaOperation {
    private static final long serialVersionUID = -8018855620167669352L;
    private final RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds;
    private final boolean persistent;
    private final ScoringScheme scoringScheme;
    private final Serializable defaultCourseAreaId;
    private final boolean useStartTimeInference;
    private final RankingMetrics rankingMetricType;
    
    public AddSpecificRegatta(String regattaName, String boatClassName, TimePoint startDate, TimePoint endDate, Serializable id,
            RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds,
            boolean persistent, ScoringScheme scoringScheme, Serializable defaultCourseAreaId, boolean useStartTimeInference, RankingMetrics rankingMetricType) {
        super(regattaName, boatClassName, startDate, endDate, id);
        this.seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds = seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds;
        this.persistent = persistent;
        this.scoringScheme = scoringScheme;
        this.defaultCourseAreaId = defaultCourseAreaId;
        this.useStartTimeInference = useStartTimeInference;
        this.rankingMetricType = rankingMetricType;
    }

    @Override
    public Regatta internalApplyTo(RacingEventService toState) throws Exception {
        Regatta regatta = toState.createRegatta(getRegattaName(), getBoatClassName(), getStartDate(), getEndDate(), getId(), createSeries(toState),
                persistent, scoringScheme, defaultCourseAreaId, useStartTimeInference, rankingMetricType.getRankingMetricConstructor());
        return regatta;
    }

    private Iterable<? extends Series> createSeries(TrackedRegattaRegistry trackedRegattaRegistry) {
        List<Series> result = new ArrayList<Series>();
        for (Map.Entry<String, SeriesCreationParametersDTO> e : seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds.getSeriesCreationParameters().entrySet()) {
            final List<String> emptyRaceColumnNamesList = Collections.emptyList();
            Series s = new SeriesImpl(e.getKey(), e.getValue().isMedal(), createFleets(e.getValue().getFleets()),
                    emptyRaceColumnNamesList, trackedRegattaRegistry);
            if (e.getValue().getDiscardingThresholds() != null) {
                s.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(e.getValue().getDiscardingThresholds()));
            }
            s.setStartsWithZeroScore(e.getValue().isStartsWithZero());
            s.setSplitFleetContiguousScoring(e.getValue().hasSplitFleetContiguousScoring());
            s.setFirstColumnIsNonDiscardableCarryForward(e.getValue().isFirstColumnIsNonDiscardableCarryForward());
            result.add(s);
        }
        return result;
    }

    private Iterable<? extends Fleet> createFleets(List<FleetDTO> fleetNamesAndOrderingAndColor) {
        List<Fleet> result = new ArrayList<Fleet>();
        for (FleetDTO fleetNameAndOrderingAndColor : fleetNamesAndOrderingAndColor) {
            Fleet fleet = new FleetImpl(fleetNameAndOrderingAndColor.getName(), fleetNameAndOrderingAndColor.getOrderNo(), fleetNameAndOrderingAndColor.getColor());
            result.add(fleet);
        }
        return result;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        // TODO Auto-generated method stub
        return null;
    }

}
