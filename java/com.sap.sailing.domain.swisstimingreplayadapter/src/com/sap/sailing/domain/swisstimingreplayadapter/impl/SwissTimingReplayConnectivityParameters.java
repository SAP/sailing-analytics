package com.sap.sailing.domain.swisstimingreplayadapter.impl;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayService;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParameters;
import com.sap.sse.common.Util;

public class SwissTimingReplayConnectivityParameters extends AbstractRaceTrackingConnectivityParameters {

    private static final long serialVersionUID = -1380661620949638776L;

    public static final String TYPE = "SWISS_TIMING_REPLAY";
    
    private final boolean useInternalMarkPassingAlgorithm;
    private final transient DomainFactory domainFactory;
    private final String boatClassName;
    private final transient RaceLogStore raceLogStore;
    private final transient RegattaLogStore regattaLogStore;
    private final String raceName;
    private final String raceID;
    private final String link;
    private final String swissTimingUrl;
    private final transient SwissTimingReplayService replayService;
    
    class SwissTimingReplayRaceTracker extends AbstractRaceTrackerImpl<SwissTimingReplayConnectivityParameters> {
        private final WindStore windStore;
        private SwissTimingReplayToDomainAdapter listener;

        public SwissTimingReplayRaceTracker(WindStore windStore, SwissTimingReplayToDomainAdapter listener,
                SwissTimingReplayConnectivityParameters connectivityParams) {
            super(connectivityParams);
            this.windStore = windStore;
            this.listener = listener;
        }

        @Override
        public Regatta getRegatta() {
            return listener.getRegatta();
        }
        
        @Override
        public void notifyRaceCreationListeners() {
            super.notifyRaceCreationListeners();
        }

        @Override
        public RaceDefinition getRace() {
            RaceDefinition race;
            try {
                race = listener.getRaceDefinition(raceID, 1 /* very short timeout */);
                return race;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public DynamicTrackedRegatta getTrackedRegatta() {
            return listener.getTrackedRegatta();
        }

        @Override
        public RaceHandle getRaceHandle() {
            return new RaceHandle() {
                @Override
                public Regatta getRegatta() {
                    return listener.getRegatta();
                }

                @Override
                public RaceDefinition getRace() {
                    try {
                        return listener.getRaceDefinition(raceID);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public RaceDefinition getRace(long timeoutInMilliseconds) {
                    try {
                        return listener.getRaceDefinition(raceID, timeoutInMilliseconds);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public DynamicTrackedRegatta getTrackedRegatta() {
                    return SwissTimingReplayRaceTracker.this.getTrackedRegatta();
                }

                @Override
                public RaceTracker getRaceTracker() {
                    return SwissTimingReplayRaceTracker.this;
                }
            };
        }

        @Override
        public WindStore getWindStore() {
            return windStore;
        }

        @Override
        public Object getID() {
            return getTrackerID();
        }
    }

    public SwissTimingReplayConnectivityParameters(String link, String swissTimingUrl, String raceName, String raceID,
            String boatClassName, boolean useInternalMarkPassingAlgorithm, DomainFactory domainFactory,
            SwissTimingReplayService replayService, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore) {
        super(/* trackWind: never track live wind with a SwissTiming Replay race */ false, /* correctWindDirectionByMagneticDeclination */ false);
        this.link = link;
        this.swissTimingUrl = swissTimingUrl;
        this.raceName = raceName;
        this.raceID = raceID;
        this.boatClassName = boatClassName;
        this.useInternalMarkPassingAlgorithm = useInternalMarkPassingAlgorithm;
        this.domainFactory = domainFactory;
        this.replayService = replayService;
        this.raceLogStore = raceLogStore;
        this.regattaLogStore = regattaLogStore;
    }

    @Override
    public RaceTracker createRaceTracker(TrackedRegattaRegistry trackedRegattaRegistry, final WindStore windStore,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            long timeoutInMilliseconds, RaceTrackingHandler raceTrackingHandler,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) throws Exception {
        SwissTimingReplayToDomainAdapter listener = new SwissTimingReplayToDomainAdapter(/* regatta */ null, raceName,
                raceID, domainFactory.getBaseDomainFactory().getOrCreateBoatClass(boatClassName), domainFactory,
                trackedRegattaRegistry, useInternalMarkPassingAlgorithm, raceLogResolver, raceLogStore,
                regattaLogStore, l->new SwissTimingReplayRaceTracker(windStore, l, this),
                raceTrackingHandler, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry);
        replayService.loadRaceData(link, listener);
        return listener.getTracker();
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry,
            WindStore windStore, RaceLogAndTrackedRaceResolver raceLogResolver,
            LeaderboardGroupResolver leaderboardGroupResolver, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) throws Exception {
        SwissTimingReplayToDomainAdapter listener = new SwissTimingReplayToDomainAdapter(regatta, raceName,
                raceID, domainFactory.getBaseDomainFactory().getOrCreateBoatClass(boatClassName),
                domainFactory, trackedRegattaRegistry, useInternalMarkPassingAlgorithm, raceLogResolver,
                raceLogStore, regattaLogStore, l->new SwissTimingReplayRaceTracker(windStore, l, this),
                raceTrackingHandler, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry);
        replayService.loadRaceData(link, listener);
        return listener.getTracker();
    }

    @Override
    public Object getTrackerID() {
        return new Util.Pair<>(link, raceID);
    }

    /**
     * There is no live tracking with this SwissTiming replay connector 
     */
    @Override
    public long getDelayToLiveInMillis() {
        return 0;
    }

    @Override
    public String getTypeIdentifier() {
        return TYPE;
    }

    public boolean isUseInternalMarkPassingAlgorithm() {
        return useInternalMarkPassingAlgorithm;
    }

    public String getBoatClassName() {
        return boatClassName;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getRaceID() {
        return raceID;
    }

    public String getLink() {
        return link;
    }
    
    public String getSwissTimingUrl() {
        return swissTimingUrl;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+" for "+raceName+"/"+raceID+", link: "+link+", boatClassName: "+boatClassName;
    }
}
