package com.sap.sailing.domain.yellowbrickadapter;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParameters;
import com.sap.sailing.domain.yellowbrickadapter.impl.YellowBrickRaceTrackerImpl;
import com.sap.sse.common.Duration;

public class YellowBrickRaceTrackingConnectivityParams extends AbstractRaceTrackingConnectivityParameters {
    private static final long serialVersionUID = -81948107186932864L;
    public static final String TYPE = "YELLOW_BRICK";
    private static final Duration DEFAULT_DELAY_TO_LIVE = Duration.ONE_MINUTE.times(20);

    private final String raceUrl;
    private final String username;
    private final String password;
    private final String trackerId;
    private final transient RaceLogStore raceLogStore;
    private final transient RegattaLogStore regattaLogStore;
    private final transient DomainFactory baseDomainFactory;
    private final transient YellowBrickTrackingAdapter yellowBrickTrackingAdapter;

    public YellowBrickRaceTrackingConnectivityParams(String raceUrl, String username, String password,
            boolean trackWind, boolean correctWindDirectionByMagneticDeclination, RaceLogStore raceLogStore,
            RegattaLogStore regattaLogStore, DomainFactory baseDomainFactory, YellowBrickTrackingAdapter yellowBrickTrackingAdapter) {
        super(trackWind, correctWindDirectionByMagneticDeclination);
        this.raceUrl = raceUrl;
        this.username = username;
        this.password = password;
        this.trackerId = TYPE + "-" + raceUrl;
        this.raceLogStore = raceLogStore;
        this.regattaLogStore = regattaLogStore;
        this.baseDomainFactory = baseDomainFactory;
        this.yellowBrickTrackingAdapter = yellowBrickTrackingAdapter;
    }

    @Override
    public RaceTracker createRaceTracker(TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            long timeoutInMilliseconds, RaceTrackingHandler raceTrackingHandler,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) throws Exception {
        return new YellowBrickRaceTrackerImpl(this, /* regatta */ null, trackedRegattaRegistry, windStore,
                raceLogResolver, leaderboardGroupResolver, timeoutInMilliseconds, raceTrackingHandler, raceLogStore,
                regattaLogStore, baseDomainFactory, yellowBrickTrackingAdapter, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry);
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry,
            WindStore windStore, RaceLogAndTrackedRaceResolver raceLogResolver,
            LeaderboardGroupResolver leaderboardGroupResolver, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry,
            ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) throws Exception {
        return new YellowBrickRaceTrackerImpl(this, regatta, trackedRegattaRegistry,
                windStore, raceLogResolver,
                leaderboardGroupResolver, timeoutInMilliseconds, raceTrackingHandler, raceLogStore, regattaLogStore,
                baseDomainFactory, yellowBrickTrackingAdapter, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry);
    }

    @Override
    public String getTrackerID() {
        return trackerId;
    }

    @Override
    public long getDelayToLiveInMillis() {
        return DEFAULT_DELAY_TO_LIVE.asMillis();
    }

    @Override
    public String getTypeIdentifier() {
        return TYPE;
    }

    public String getRaceUrl() {
        return raceUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public RaceLogStore getRaceLogStore() {
        return raceLogStore;
    }

    public RegattaLogStore getRegattaLogStore() {
        return regattaLogStore;
    }

    public DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }
}
