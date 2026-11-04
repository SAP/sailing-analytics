package com.sap.sailing.domain.common.i18n;

/** Labels for master data import phases, independent of the UI translation bundle. */
public interface DataImportProgressMessages {
    String init();
    String connectionSetup();
    String connectionEstablish();
    String transferStarted();
    String transferCompleted();
    String importInit();
    String importWait();
    String importLeaderboardGroups();
    String importWindTracks();
    String importSensorFixes();
    String updateEventLeaderboardGroupLinks();
    String importTrackedRaces();
    String waitingForTrackedRacesToFinishLoading();
}
