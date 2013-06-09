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
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class AddSpecificRegatta extends AbstractAddRegattaOperation {
    private static final long serialVersionUID = -8018855620167669352L;
    private final Map<String, Triple<List<Triple<String, Integer, Color>>, Pair<Boolean, Boolean>, int[]>> seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds;
    private final boolean persistent;
    private final ScoringScheme scoringScheme;
    private final Serializable defaultCourseAreaId;
    
    public AddSpecificRegatta(String regattaName, String boatClassName, Serializable id,
            Map<String, Triple<List<Triple<String, Integer, Color>>, Pair<Boolean, Boolean>, int[]>> seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds,
            boolean persistent, ScoringScheme scoringScheme, Serializable defaultCourseAreaId) {
        super(regattaName, boatClassName, id);
        this.seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds = seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndDiscardingThresholds;
        this.persistent = persistent;
        this.scoringScheme = scoringScheme;
        this.defaultCourseAreaId = defaultCourseAreaId;
    }

    @Override
    public Regatta internalApplyTo(RacingEventService toState) throws Exception {
        return toState.createRegatta(getBaseRegattaName(), getBoatClassName(), getId(), createSeries(toState),
                persistent, scoringScheme, defaultCourseAreaId);
    }

    private Iterable<? extends Series> createSeries(TrackedRegattaRegistry trackedRegattaRegistry) {
        List<Series> result = new ArrayList<Series>();
        for (Map.Entry<String, Triple<List<Triple<String, Integer, Color>>, Pair<Boolean, Boolean>, int[]>> e : seriesNamesWithFleetNamesAndFleetOrderingAndMedalAndStartsWithZeroScoreAndDiscardingThresholds.entrySet()) {
            final List<String> emptyRaceColumnNamesList = Collections.emptyList();
            Series s = new SeriesImpl(e.getKey(), /* isMedal */e.getValue().getB().getA(), createFleets(e.getValue().getA()),
                    emptyRaceColumnNamesList, trackedRegattaRegistry);
            if (e.getValue().getC() != null) {
                s.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(e.getValue().getC()));
            }
            s.setStartsWithZeroScore(e.getValue().getB().getB());
            result.add(s);
        }
        return result;
    }

    private Iterable<? extends Fleet> createFleets(List<Triple<String, Integer, Color>> fleetNamesAndOrderingAndColor) {
        List<Fleet> result = new ArrayList<Fleet>();
        for (Triple<String, Integer, Color> fleetNameAndOrderingAndColor : fleetNamesAndOrderingAndColor) {
            Fleet fleet = new FleetImpl(fleetNameAndOrderingAndColor.getA(), fleetNameAndOrderingAndColor.getB(), fleetNameAndOrderingAndColor.getC());
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
