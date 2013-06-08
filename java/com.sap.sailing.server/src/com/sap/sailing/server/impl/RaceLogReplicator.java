package com.sap.sailing.server.impl;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogIdentifierTemplateResolver;
import com.sap.sailing.domain.racelog.impl.RaceLogOnLeaderboardIdentifier;
import com.sap.sailing.domain.racelog.impl.RaceLogOnRegattaIdentifier;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.Replicator;
import com.sap.sailing.server.operationaltransformation.RecordRaceLogEventOnLeaderboard;
import com.sap.sailing.server.operationaltransformation.RecordRaceLogEventOnRegatta;

public class RaceLogReplicator implements RaceColumnListener {
    private static final long serialVersionUID = 7190510926643574068L;
    
    private final Replicator service;
    
    public RaceLogReplicator(Replicator service) {
        this.service = service;
    }

    @Override
    public void raceLogEventAdded(final RaceColumn raceColumn, final RaceLogIdentifier identifier, final RaceLogEvent event) {
        identifier.getTemplate().resolve(new RaceLogIdentifierTemplateResolver() {
            
            @Override
            public void resolveOnRegattaIdentifierAndReplicate(RaceLogOnRegattaIdentifier identifierTemplate) {
                RacingEventServiceOperation<?> operation = new RecordRaceLogEventOnRegatta(
                        identifierTemplate.getParentObjectName(), 
                        raceColumn.getName(), 
                        identifier.getFleetName(), 
                        event);
                service.replicate(operation);
            }
            
            @Override
            public void resolveOnLeaderboardIdentifierAndReplicate(RaceLogOnLeaderboardIdentifier identifierTemplate) {
                RacingEventServiceOperation<?> operation = new RecordRaceLogEventOnLeaderboard(
                        identifierTemplate.getParentObjectName(), 
                        raceColumn.getName(), 
                        identifier.getFleetName(), 
                        event);
                service.replicate(operation);
            }
        });
    }

    @Override
    public boolean isTransient() {
        return true;
    }
    
    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
    }

    @Override
    public void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace) {
    }

    @Override
    public void isStartsWithZeroScoreChanged(RaceColumn raceColumn, boolean newIsStartsWithZeroScore) {
    }

    @Override
    public boolean canAddRaceColumnToContainer(RaceColumn raceColumn) {
        return true;
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
    }

    @Override
    public void raceColumnMoved(RaceColumn raceColumn, int newIndex) {
    }

    @Override
    public void factorChanged(RaceColumn raceColumn, Double oldFactor, Double newFactor) {
    }

    @Override
    public void competitorDisplayNameChanged(Competitor competitor, String oldDisplayName, String displayName) {
    }

    @Override
    public void resultDiscardingRuleChanged(ResultDiscardingRule oldDiscardingRule, ResultDiscardingRule newDiscardingRule) {
    }

    
}
