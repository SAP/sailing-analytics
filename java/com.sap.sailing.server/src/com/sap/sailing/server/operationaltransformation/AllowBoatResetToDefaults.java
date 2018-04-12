package com.sap.sailing.server.operationaltransformation;

import java.util.ArrayList;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sse.common.Util;

public class AllowBoatResetToDefaults extends AbstractRacingEventServiceOperation<Void> {
    private static final long serialVersionUID = -3698113910122095903L;
    private final Iterable<String> boatIdsAsStrings;
    
    public AllowBoatResetToDefaults(Iterable<String> boatIdsAsStrings) {
        super();
        final ArrayList<String> arrayList = new ArrayList<>(); // to guarantee serializability, even if an unmodifiable or singleton is passed
        this.boatIdsAsStrings = arrayList;
        Util.addAll(boatIdsAsStrings, arrayList);
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        final CompetitorAndBoatStore competitorAndBoatStore = toState.getBaseDomainFactory().getCompetitorStore();
        for (String boatIdAsString : boatIdsAsStrings) {
            Boat boat = competitorAndBoatStore.getExistingBoatByIdAsString(boatIdAsString);
            if (boat != null) {
                competitorAndBoatStore.allowBoatResetToDefaults(boat);
            }
        }
        return null;
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
