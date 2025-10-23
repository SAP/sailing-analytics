package com.sap.sailing.domain.tractracadapter.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.tractrac.model.lib.api.ModelLocator;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;
import com.tractrac.util.lib.api.exceptions.TimeOutException;

public class RaceTrackingConnectivityParametersImpl extends AbstractRaceTrackingConnectivityParameters {
    private static final long serialVersionUID = 5088282956033068149L;
    private static final Logger logger = Logger.getLogger(RaceTrackingConnectivityParametersImpl.class.getName());
    public static final String TYPE = "TRAC_TRAC";
    
    private final URL paramURL;
    private final URI liveURI;
    private final URI liveURIFromConfiguration;
    private final URI storedURI;
    private final URI storedURIFromConfiguration;
    private final URI updateURI;
    private final TimePoint startOfTracking;
    private final TimePoint endOfTracking;
    private final transient RaceLogStore raceLogStore;
    private final transient RegattaLogStore regattaLogStore;
    private final transient DomainFactory domainFactory;
    private final long delayToLiveInMillis;
    private final Duration offsetToStartTimeOfSimulatedRace;
    private final String tracTracApiToken;
    private final String raceStatus;
    private final String raceVisibility;
    private final boolean useInternalMarkPassingAlgorithm;
    private final boolean preferReplayIfAvailable;
    private final int timeoutInMillis;
    private final boolean useOfficialEventsToUpdateRaceLog;

    /**
     * @param preferReplayIfAvailable
     *            when a non-{@code null} {@code storedURI} and/or {@code liveURI} are provided and the {@link IRace}
     *            specifies something different and claims to be in replay mode ({@link IRace#getConnectionType} is
     *            {@code File}) then if this parameter is {@code true} the race will be loaded from the replay file
     *            instead of the {@code storedURI}/{@code liveURI} specified. This is particularly useful for restoring
     *            races if since the last connection the race was migrated to a replay file format.
     * @param timeoutInMillis
     *            -1 means no timeout; otherwise, this is the timeout for waiting for the {@link IRace} to be obtained
     *            from the {@code paramURL} document. A {@link TimeoutException} will result if the timeout applied.
     * @param useOfficialEventsToUpdateRaceLog
     *            whether to use race and competitor status to create according race log entries that for official
     *            competitor results can then also lead to leaderboard updates
     */
    public RaceTrackingConnectivityParametersImpl(URL paramURL, URI liveURI, URI storedURI, URI updateURI,
            TimePoint startOfTracking, TimePoint endOfTracking, long delayToLiveInMillis,
            Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm,
            RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, DomainFactory domainFactory,
            String tracTracApiToken, String raceStatus, String raceVisibility,
            boolean trackWind, boolean correctWindDirectionByMagneticDeclination, boolean preferReplayIfAvailable,
            int timeoutInMillis, boolean useOfficialEventsToUpdateRaceLog,
            URI liveURIFromConfiguration, URI storedURIFromConfiguration) throws Exception {
        super(trackWind, correctWindDirectionByMagneticDeclination);
        this.useOfficialEventsToUpdateRaceLog = useOfficialEventsToUpdateRaceLog;
        this.paramURL = paramURL;
        this.timeoutInMillis = timeoutInMillis;
        final IRace tractracRace = getTractracRace();
        if (preferReplayIfAvailable && isReplayRace(tractracRace) &&
                (!Util.equalsWithNull(liveURI, tractracRace.getLiveURI()) || !Util.equalsWithNull(storedURI, tractracRace.getStoredURI()))) {
            logger.info("Replay format available and preferred for race " + tractracRace.getName()
                    + "; using storedURI " + tractracRace.getStoredURI() + " instead of " + storedURI
                    + " and liveURI " + tractracRace.getLiveURI() + " instead of " + liveURI);
            this.liveURI = tractracRace.getLiveURI();
            this.storedURI = tractracRace.getStoredURI();
        } else {
            this.liveURI = liveURI;
            this.storedURI = storedURI;
        }
        this.updateURI = updateURI;
        this.startOfTracking = startOfTracking;
        this.endOfTracking = endOfTracking;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.domainFactory = domainFactory;
        this.offsetToStartTimeOfSimulatedRace = offsetToStartTimeOfSimulatedRace;
        this.raceLogStore = raceLogStore;
        this.regattaLogStore = regattaLogStore;
        this.tracTracApiToken = tracTracApiToken;
        this.raceStatus = raceStatus;
        this.raceVisibility = raceVisibility;
        this.useInternalMarkPassingAlgorithm = useInternalMarkPassingAlgorithm;
        this.preferReplayIfAvailable = preferReplayIfAvailable;
        this.liveURIFromConfiguration = liveURIFromConfiguration;
        this.storedURIFromConfiguration = storedURIFromConfiguration;
    }

    public boolean isReplayRace(IRace tractracRace) {
        return tractracRace.getStoredURI() != null && tractracRace.getStoredURI().toString().toLowerCase().endsWith(".mtb");
    }
    
    public IRace getTractracRace() throws CreateModelException, URISyntaxException, TimeOutException {
        final IRace result;
        if (getTimeoutInMillis() == -1) {
            result = ModelLocator.getEventFactory().createRace(tracTracApiToken, new URI(paramURL.toString()));
        } else {
            result = ModelLocator.getEventFactory().createRace(tracTracApiToken, new URI(paramURL.toString()), getTimeoutInMillis());
        }
        return result;
    }

    @Override
    public String getTypeIdentifier() {
        return TYPE;
    }

    @Override
    public RaceTracker createRaceTracker(TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            long timeoutInMilliseconds, RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) throws URISyntaxException,
            CreateModelException, SubscriberInitializationException, IOException, InterruptedException, TimeOutException {
        RaceTracker tracker = domainFactory.createRaceTracker(raceLogStore, regattaLogStore, windStore,
                trackedRegattaRegistry, raceLogResolver, leaderboardGroupResolver, this, timeoutInMilliseconds,
                raceTrackingHandler, markPassingRaceFingerprintRegistry);
        return tracker;
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry,
            WindStore windStore, RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            long timeoutInMilliseconds, RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) throws Exception {
        RaceTracker tracker = domainFactory.createRaceTracker(regatta, raceLogStore, regattaLogStore, windStore,
                trackedRegattaRegistry, raceLogResolver, leaderboardGroupResolver, this, timeoutInMilliseconds,
                raceTrackingHandler, markPassingRaceFingerprintRegistry);
        return tracker;
    }

    @Override
    public Object getTrackerID() {
        return TracTracRaceTrackerImpl.createID(paramURL, liveURI, storedURI);
    }

    @Override
    public long getDelayToLiveInMillis() {
        return delayToLiveInMillis;
    }

    public URL getParamURL() {
        return paramURL;
    }

    public URI getLiveURI() {
        return liveURI;
    }

    public URI getStoredURI() {
        return storedURI;
    }

    public URI getLiveURIFromConfiguration() {
        return liveURIFromConfiguration;
    }

    public URI getStoredURIFromConfiguration() {
        return storedURIFromConfiguration;
    }

    public URI getUpdateURI() {
        return updateURI;
    }

    public TimePoint getStartOfTracking() {
        return startOfTracking;
    }

    public TimePoint getEndOfTracking() {
        return endOfTracking;
    }

    public Duration getOffsetToStartTimeOfSimulatedRace() {
        return offsetToStartTimeOfSimulatedRace;
    }

    public String getTracTracApiToken() {
        return tracTracApiToken;
    }

    public String getRaceStatus() {
        return raceStatus;
    }

    public String getRaceVisibility() {
        return raceVisibility;
    }

    public boolean isUseInternalMarkPassingAlgorithm() {
        return useInternalMarkPassingAlgorithm;
    }

    public boolean isPreferReplayIfAvailable() {
        return preferReplayIfAvailable;
    }
    
    public int getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public boolean isUseOfficialEventsToUpdateRaceLog() {
        return useOfficialEventsToUpdateRaceLog;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + paramURL + ", liveURI: " + liveURI + ", storedURI: " + storedURI
                + ", useOfficialEventsToUpdateRaceLog: " + useOfficialEventsToUpdateRaceLog;
    }
}
