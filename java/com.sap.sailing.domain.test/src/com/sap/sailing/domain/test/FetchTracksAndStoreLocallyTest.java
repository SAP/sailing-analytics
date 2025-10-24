package com.sap.sailing.domain.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;

/**
 * Receives GPS tracks from a race. One test (if not ignored) stores these tracks in the resources/
 * folder for later (fast, off-line and reproducible) use by other tests. The other tests apply
 * smoothening and ensure that smoothening filters out serious outliers but doesn't alter good
 * tracks that don't have outliers.
 * 
 * @author Axel Uhl (D043530)
 *
 */
@Disabled("Un-ignore when you need to fetch new tracks")
public class FetchTracksAndStoreLocallyTest extends OnlineTracTracBasedTest {
    private final Map<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> tracks;
    private TrackedRace trackedRace;

    public FetchTracksAndStoreLocallyTest() throws URISyntaxException, MalformedURLException {
        tracks = new HashMap<Competitor, DynamicGPSFixTrack<Competitor,GPSFixMoving>>();
    }
    
    /**
     * Sets up a single listener so that the rather time-consuming race setup is received only once, and all
     * tests in this class share a single feed execution. The listener fills in the first event received
     * into {@link #firstTracked} and {@link #firstData}. All events are converted into {@link GPSFixMovingImpl}
     * objects and appended to the {@link DynamicTrackedRace}s.
     * @throws SubscriberInitializationException 
     */
    private void setUp(String regattaName, String raceId) throws MalformedURLException, IOException, InterruptedException, URISyntaxException, SubscriberInitializationException, CreateModelException {
        super.setUpWithoutLaunchingController(regattaName, raceId);
        final RaceChangeListener positionListener = new AbstractRaceChangeListener() {
            @Override
            public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor, AddResult addedOrReplaced) {
                DynamicGPSFixTrack<Competitor, GPSFixMoving> track = tracks.get(competitor);
                if (track == null) {
                    track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor, /* millisecondsOverWhichToAverage */ 40000);
                    tracks.put(competitor, track);
                }
                track.addGPSFix((GPSFixMoving) fix);
            }
        };
        DynamicTrackedRegatta trackedRegatta = getTrackedRegatta();
        trackedRegatta.addRaceListener(new RaceListener() {
            @Override
            public void raceAdded(TrackedRace trackedRace) {
                System.out.println("Subscribing raw position listener for race "+trackedRace);
                FetchTracksAndStoreLocallyTest.this.trackedRace = trackedRace;
                ((DynamicTrackedRace) trackedRace).addListener(positionListener);
            }
            @Override
            public void raceRemoved(TrackedRace trackedRace) {
            }
        }, Optional.empty(), /* synchronous */ false);
        super.completeSetupLaunchingControllerAndWaitForRaceDefinition(ReceiverType.RACECOURSE,
                ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS);
    }

    @Test
    public void storeEntireKielWeek() throws InterruptedException, FileNotFoundException, IOException, URISyntaxException, SubscriberInitializationException, CreateModelException {
        for (String raceId : new String[] {
                "7dce540a-98e4-11e0-85be-406186cbf87c", "6659efe8-98f1-11e0-85be-406186cbf87c",
                "be1dc2fe-98fb-11e0-85be-406186cbf87c", "c3ec93e4-98fc-11e0-85be-406186cbf87c",
                "bb214cfc-996d-11e0-85be-406186cbf87c", "648cf310-9930-11e0-85be-406186cbf87c",
                "4074674e-9933-11e0-85be-406186cbf87c", "ba251890-9933-11e0-85be-406186cbf87c",
                "5b08a9ee-9933-11e0-85be-406186cbf87c", "d6e18ba8-9933-11e0-85be-406186cbf87c",
                "b9962c58-9932-11e0-85be-406186cbf87c", "d287c316-9932-11e0-85be-406186cbf87c",
                "774b7942-9933-11e0-85be-406186cbf87c", "06c1e6d8-9934-11e0-85be-406186cbf87c",
                "9cd8ce58-9933-11e0-85be-406186cbf87c", "1e68237e-9934-11e0-85be-406186cbf87c",
                "047248b0-9933-11e0-85be-406186cbf87c", "1ecdea8e-9933-11e0-85be-406186cbf87c",
                "367ae104-9934-11e0-85be-406186cbf87c", "5291b3ea-9934-11e0-85be-406186cbf87c",
                "46b8c1c0-99ee-11e0-85be-406186cbf87c", "d0f23cdc-99ed-11e0-85be-406186cbf87c",
                "65f5ea4a-99ee-11e0-85be-406186cbf87c", "a7405cce-99ee-11e0-85be-406186cbf87c",
                "f5f531ec-99ed-11e0-85be-406186cbf87c", "bfe5f5cc-99ee-11e0-85be-406186cbf87c",
                "7a4e6206-99ee-11e0-85be-406186cbf87c", "92abc794-99ee-11e0-85be-406186cbf87c",
                "e9557aea-99ee-11e0-85be-406186cbf87c", "d5924a4c-99ee-11e0-85be-406186cbf87c",
                "eaca658e-9a39-11e0-85be-406186cbf87c", "413b43f8-9a8e-11e0-85be-406186cbf87c",
                "b3f37dee-9a8d-11e0-85be-406186cbf87c", "ca9e510e-9a8d-11e0-85be-406186cbf87c",
                "0d21dc62-9a8e-11e0-85be-406186cbf87c", "2be15c2c-9a8e-11e0-85be-406186cbf87c",
                "5b71b374-9a8e-11e0-85be-406186cbf87c", "a0679eda-9a8e-11e0-85be-406186cbf87c",
                "7837359c-9a8e-11e0-85be-406186cbf87c", "8dc9feee-9a8e-11e0-85be-406186cbf87c",
                "9690d0ee-9b69-11e0-85be-406186cbf87c", "77db2f88-9b7c-11e0-85be-406186cbf87c",
                "65f746fa-9b7f-11e0-85be-406186cbf87c", "7d52a25e-9b7f-11e0-85be-406186cbf87c",
                "962d13fe-9b7f-11e0-85be-406186cbf87c", "2aca1908-9c34-11e0-85be-406186cbf87c",
                "8981ee52-9b7c-11e0-85be-406186cbf87c", "70acfbe2-9b81-11e0-85be-406186cbf87c",
                "85c505d8-9b81-11e0-85be-406186cbf87c", "ffd33b76-9b7f-11e0-85be-406186cbf87c",
                "2036421a-9a8f-11e0-85be-406186cbf87c", "2b0d3990-9b80-11e0-85be-406186cbf87c",
                "49a95a00-9b80-11e0-85be-406186cbf87c", "9d686a32-9c48-11e0-85be-406186cbf87c",
                "b0f20cf2-9c48-11e0-85be-406186cbf87c", "c489a63a-9c48-11e0-85be-406186cbf87c",
                "d591d808-9c48-11e0-85be-406186cbf87c", "07a91aca-9cdc-11e0-85be-406186cbf87c",
                "c5053bba-9c98-11e0-85be-406186cbf87c", "be3e0862-9d14-11e0-85be-406186cbf87c",
                "357c700a-9d9a-11e0-85be-406186cbf87c", "71c9c288-9d9f-11e0-85be-406186cbf87c",
                "5e11c318-9d76-11e0-85be-406186cbf87c", "16edbf04-9d77-11e0-85be-406186cbf87c",
                "78991504-9da5-11e0-85be-406186cbf87c", "e876c3a0-9da8-11e0-85be-406186cbf87c",
                "7b8423e4-9d91-11e0-85be-406186cbf87c", "b3e9082e-9d11-11e0-85be-406186cbf87c",
                "fe8d9dc6-9dda-11e0-85be-406186cbf87c", "56a61922-9da7-11e0-85be-406186cbf87c",
                "bf0f3188-9db1-11e0-85be-406186cbf87c", "68f793e6-9d91-11e0-85be-406186cbf87c",
                "7c666e50-9dde-11e0-85be-406186cbf87c", "88152c2e-9ddd-11e0-85be-406186cbf87c",
                "92d1755e-9dde-11e0-85be-406186cbf87c", "97eaedaa-9ddd-11e0-85be-406186cbf87c",
                "a7893608-9dde-11e0-85be-406186cbf87c", "c31b099c-9ddd-11e0-85be-406186cbf87c",
                "ee09d080-9e59-11e0-85be-406186cbf87c", "081307ee-9e5a-11e0-85be-406186cbf87c",
                "20382b9c-9e5a-11e0-85be-406186cbf87c", "eb4ef344-9e67-11e0-85be-406186cbf87c",
                "f54d922c-9ddd-11e0-85be-406186cbf87c", "04687b2a-9e68-11e0-85be-406186cbf87c",
                "dffb64de-9e6a-11e0-85be-406186cbf87c", "797f28de-9e8e-11e0-85be-406186cbf87c",
                "af33e03c-9e93-11e0-85be-406186cbf87c", "cb043bb4-9e92-11e0-85be-406186cbf87c",
                "10a4757c-9e92-11e0-85be-406186cbf87c", "de822656-9e92-11e0-85be-406186cbf87c",
                "c071401a-9e93-11e0-85be-406186cbf87c", "36ed21ca-9e92-11e0-85be-406186cbf87c",
                "33851320-9e93-11e0-85be-406186cbf87c", "d9fd357a-9e93-11e0-85be-406186cbf87c",
                "496a5458-9e92-11e0-85be-406186cbf87c", "a75dca04-9f55-11e0-85be-406186cbf87c",
                "c9b627b4-9f54-11e0-85be-406186cbf87c", "829bd366-9f53-11e0-85be-406186cbf87c",
                "bc78290c-9f55-11e0-85be-406186cbf87c", "d62d4288-9f55-11e0-85be-406186cbf87c",
                "972c4d74-9f53-11e0-85be-406186cbf87c", "c3ab8bcc-a17d-11e0-aeec-406186cbf87c" }) {
            System.out.println("Loading and storing race "+raceId);
            storeRace("event_20110609_KielerWoch", raceId);
        }
    }
    
    @Disabled
    public void store505Race2() throws InterruptedException, FileNotFoundException, IOException, URISyntaxException, SubscriberInitializationException, CreateModelException {
        storeRace("event_20110609_KielerWoch", "357c700a-9d9a-11e0-85be-406186cbf87c");
    }

    private void storeRace(String regattaName, String raceId) throws MalformedURLException, IOException, InterruptedException,
            URISyntaxException, FileNotFoundException, SubscriberInitializationException, CreateModelException {
        setUp(regattaName, raceId);
        storeTracks();
    }
    
    public static void main(String[] args) throws URISyntaxException, FileNotFoundException, IOException, InterruptedException, SubscriberInitializationException, CreateModelException {
        FetchTracksAndStoreLocallyTest thiz = new FetchTracksAndStoreLocallyTest();
        for (int i=1; i<args.length; i++) {
            thiz.storeRace(args[0], args[i]);
        }
    }
    
    private void storeTracks() throws FileNotFoundException, IOException {
        for (Map.Entry<Competitor, DynamicGPSFixTrack<Competitor, GPSFixMoving>> competitorAndTrack : tracks.entrySet()) {
            Competitor competitor = competitorAndTrack.getKey();
            Boat boatOfCompetitor = trackedRace.getRace().getBoatOfCompetitor(competitor);
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = competitorAndTrack.getValue();
            storeTrack(competitor, boatOfCompetitor, track, getTracTracEvent().getName()+"-"+trackedRace.getRace().getName());
        }
    }

}
