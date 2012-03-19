package com.sap.sailing.gwt.ui.actions;

import java.util.Collection;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;

public class GetWindInfoAction extends DefaultAsyncAction<WindInfoForRaceDTO> {
    private final SailingServiceAsync sailingService;
    private final RaceIdentifier raceIdentifier;
    private Date from;
    private long millisecondsStepWidth;
    private int numberOfFixes;
    private Collection<String> windSourceTypeNames;
    
    private long resolutionInMilliseconds;
    private Date fromDate;
    private Date toDate;
    
    private enum CallVariants { Variant1, Variant2 };
    private final CallVariants callVariant;

    public GetWindInfoAction(SailingServiceAsync sailingService, RaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, Collection<String> windSourceTypeNames, AsyncCallback<WindInfoForRaceDTO> callback) {
        super(callback);
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.from = from;
        this.millisecondsStepWidth = millisecondsStepWidth;
        this.numberOfFixes = numberOfFixes;
        this.windSourceTypeNames = windSourceTypeNames;
        callVariant = CallVariants.Variant1;
    }

    public GetWindInfoAction(SailingServiceAsync sailingService, RaceIdentifier raceIdentifier, Date fromDate,
            Date toDate, long resolutionInMilliseconds, Collection<String> windSourceTypeNames,
            AsyncCallback<WindInfoForRaceDTO> callback) {
        super(callback);
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.resolutionInMilliseconds = resolutionInMilliseconds;
        this.windSourceTypeNames = windSourceTypeNames;
        callVariant = CallVariants.Variant2;
    }

    @Override
    public void execute() {
        switch (callVariant) {
        case Variant1:
            sailingService.getWindInfo(raceIdentifier, from, millisecondsStepWidth, numberOfFixes, windSourceTypeNames, (AsyncCallback<WindInfoForRaceDTO>) getWrapperCallback());
            break;
        case Variant2:
            sailingService.getWindInfo(raceIdentifier, fromDate, toDate, resolutionInMilliseconds, windSourceTypeNames, (AsyncCallback<WindInfoForRaceDTO>) getWrapperCallback());
            break;
        }
    }
}