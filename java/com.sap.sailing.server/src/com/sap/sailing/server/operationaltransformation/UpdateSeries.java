package com.sap.sailing.server.operationaltransformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class UpdateSeries extends AbstractSeriesOperation<Void> {
    private static final long serialVersionUID = 6356089749049112710L;
    
    private final List<FleetDTO> fleets;
    private final boolean isMedal;
    private final int[] resultDiscardingThresholds;
    private final boolean startsWithZeroScore;
    private final boolean firstColumnIsNonDiscardableCarryForward;
    private final boolean hasSplitFleetContiguousScoring;

    public UpdateSeries(RegattaIdentifier regattaIdentifier, String seriesName, boolean isMedal,
            int[] resultDiscardingThresholds, boolean startsWithZeroScore,
            boolean firstColumnIsNonDiscardableCarryForward, boolean hasSplitFleetContiguousScoring,
            List<FleetDTO> fleets) {
        super(regattaIdentifier, seriesName);
        this.isMedal = isMedal;
        this.resultDiscardingThresholds = resultDiscardingThresholds;
        this.startsWithZeroScore = startsWithZeroScore;
        this.firstColumnIsNonDiscardableCarryForward = firstColumnIsNonDiscardableCarryForward;
        this.hasSplitFleetContiguousScoring = hasSplitFleetContiguousScoring;
        this.fleets = fleets;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        Series series = getSeries(toState);
        if (series == null) {
            series = createSeries(toState);
        } 
        series.setIsMedal(isMedal);
        series.setResultDiscardingRule(resultDiscardingThresholds == null ?
                null : new ThresholdBasedResultDiscardingRuleImpl(resultDiscardingThresholds));
        series.setStartsWithZeroScore(startsWithZeroScore);
        series.setFirstColumnIsNonDiscardableCarryForward(firstColumnIsNonDiscardableCarryForward);
        series.setSplitFleetContiguousScoring(hasSplitFleetContiguousScoring);
        if (series.getRegatta().isPersistent()) {
            toState.updateStoredRegatta(series.getRegatta());
        }
        return null;
    }
    
    private Series createSeries(RacingEventService toState) {
        Regatta regatta = toState.getRegatta(getRegattaIdentifier());
        final List<String> emptyRaceColumnNames = Collections.emptyList();
        List<Fleet> result = new ArrayList<Fleet>();
        for (FleetDTO fleetNameAndOrderingAndColor : fleets) {
            Fleet fleet = new FleetImpl(fleetNameAndOrderingAndColor.getName(), fleetNameAndOrderingAndColor.getOrderNo(), fleetNameAndOrderingAndColor.getColor());
            result.add(fleet);
        }
        regatta.addSeries(new SeriesImpl(getSeriesName(), isMedal, result, emptyRaceColumnNames, (TrackedRegattaRegistry)toState));
        return regatta.getSeriesByName(getSeriesName());
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
