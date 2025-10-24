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
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.RankingMetricsFactory;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class AddSpecificRegatta extends AbstractAddRegattaOperation {
    private static final long serialVersionUID = -8018855620167669352L;
    private final RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds;
    private final boolean persistent;
    private final ScoringScheme scoringScheme;
    private final List<Serializable> courseAreaIds;
    private final Double buoyZoneRadiusInHullLengths;
    private final boolean useStartTimeInference;
    private final boolean controlTrackingFromStartAndFinishTimes;
    private final boolean autoRestartTrackingUponCompetitorSetChange;
    private final boolean canBoatsOfCompetitorsChangePerRace;
    private final CompetitorRegistrationType competitorRegistrationType;
    private final RankingMetrics rankingMetricType;
    private final String registrationLinkSecret;
    
    /**
     * @param courseAreaIds
     *            may be {@code null}, meaning an empty set of course areas; no worries about serializability; a local
     *            serializable collection will be used, and the elements of this parameter, if not {@code null}, will be
     *            copied into it
     */
    public AddSpecificRegatta(String regattaName, String boatClassName, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret, TimePoint startDate, TimePoint endDate, Serializable id,
            RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds,
            boolean persistent, ScoringScheme scoringScheme, Iterable<? extends Serializable> courseAreaIds, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange, RankingMetrics rankingMetricType) {
        super(regattaName, boatClassName, startDate, endDate, id);
        this.canBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace;
        this.competitorRegistrationType = competitorRegistrationType;
        this.seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds = seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds;
        this.persistent = persistent;
        this.scoringScheme = scoringScheme;
        this.courseAreaIds = new ArrayList<>();
        if (courseAreaIds != null) {
            Util.addAll(courseAreaIds, this.courseAreaIds);
        }
        this.useStartTimeInference = useStartTimeInference;
        this.controlTrackingFromStartAndFinishTimes = controlTrackingFromStartAndFinishTimes;
        this.autoRestartTrackingUponCompetitorSetChange = autoRestartTrackingUponCompetitorSetChange;
        this.rankingMetricType = rankingMetricType;
        this.buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengths;
        this.registrationLinkSecret = registrationLinkSecret;
    }

    @Override
    public Regatta internalApplyTo(RacingEventService toState) throws Exception {
        final Regatta regatta = toState.createRegatta(getRegattaName(), getBoatClassName(),
                canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, registrationLinkSecret, getStartDate(),
                getEndDate(), getId(), createSeries(toState), persistent, scoringScheme, courseAreaIds,
                buoyZoneRadiusInHullLengths, useStartTimeInference, controlTrackingFromStartAndFinishTimes,
                autoRestartTrackingUponCompetitorSetChange, RankingMetricsFactory.getRankingMetricConstructor(rankingMetricType));
        return regatta;
    }

    private Iterable<? extends Series> createSeries(TrackedRegattaRegistry trackedRegattaRegistry) {
        List<Series> result = new ArrayList<Series>();
        for (Map.Entry<String, SeriesCreationParametersDTO> e : seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds.getSeriesCreationParameters().entrySet()) {
            final List<String> emptyRaceColumnNamesList = Collections.emptyList();
            Series s = new SeriesImpl(e.getKey(), e.getValue().isMedal(), e.getValue().isFleetsCanRunInParallel(), createFleets(e.getValue().getFleets()),
                    emptyRaceColumnNamesList, trackedRegattaRegistry);
            if (e.getValue().getDiscardingThresholds() != null) {
                s.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(e.getValue().getDiscardingThresholds()));
            }
            s.setStartsWithZeroScore(e.getValue().isStartsWithZero());
            s.setSplitFleetContiguousScoring(e.getValue().hasSplitFleetContiguousScoring());
            s.setFirstColumnIsNonDiscardableCarryForward(e.getValue().isFirstColumnIsNonDiscardableCarryForward());
            s.setMaximumNumberOfDiscards(e.getValue().getMaximumNumberOfDiscards());
            s.setOneAlwaysStaysOne(e.getValue().isOneAlwaysStaysOne());
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
