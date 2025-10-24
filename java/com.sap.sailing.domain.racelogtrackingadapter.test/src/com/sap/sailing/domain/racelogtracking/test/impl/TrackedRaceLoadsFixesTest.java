package com.sap.sailing.domain.racelogtracking.test.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.FixLoaderAndTracker;
import com.sap.sailing.domain.racelogtracking.test.AbstractGPSFixStoreTest;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

@Timeout(value=3, unit=TimeUnit.MINUTES)
public class TrackedRaceLoadsFixesTest extends AbstractGPSFixStoreTest {
    private static final Logger logger = Logger.getLogger(TrackedRaceLoadsFixesTest.class.getName());
    
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
    private final Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
    private final Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null,
            null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
    private final Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
    private final Course course =  new CourseImpl("course", Arrays.asList(new WaypointImpl(mark), new WaypointImpl(mark2)));

    private RaceDefinition raceDefinition;
    
    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException {
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        this.raceDefinition = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
    }

    @Test
    public void doesRaceLoadOnlyBetweenStartAndEndOfTracking() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        DeviceIdentifier markDevice = new SmartphoneImeiIdentifierImpl("imei2");
        defineMarksOnRegattaLog(mark, mark2);
        map(comp, device, 0, 10000);
        map(mark, markDevice, 0, 10000);
        store.storeFix(device, createFix(100, 10, 20, 30, 40)); // before
        store.storeFix(device, createFix(1100, 10, 20, 30, 40)); // in
        store.storeFix(device, createFix(2100, 10, 20, 30, 40)); // after
        store.storeFix(markDevice, createFix(100, 10, 20));
        store.storeFix(markDevice, createFix(1100, 10, 20));
        store.storeFix(markDevice, createFix(2100, 10, 20));
        final DynamicTrackedRaceImpl trackedRace = createDynamicTrackedRace(boatClass, raceDefinition);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(1000));
        trackedRace.setEndOfTrackingReceived(new MillisecondsTimePoint(2000));
        new FixLoaderAndTracker(trackedRace, store, null, /* removeOutliersFromCompetitorTracks */ false);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getTrack(comp), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        // now extend the tracking interval of the tracked race and assert that the additional fixes are loaded
        trackedRace.setEndOfTrackingReceived(new MillisecondsTimePoint(2500), /* wait for fixes to load */ true);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getTrack(comp), 2);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(0), /* wait for fixes to load */ true);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getTrack(comp), 3);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 3);
    }
    
    @Test
    public void areFixesStoredInDb() throws TransformationException, NoCorrespondingServiceRegisteredException,
            InterruptedException {
        defineMarksOnRegattaLog(mark, mark2);
        DeviceIdentifier device2 = new SmartphoneImeiIdentifierImpl("imei2");
        DeviceIdentifier device3 = new SmartphoneImeiIdentifierImpl("imei3");
        map(comp, device, 0, 20000);
        map(comp2, device2, 0, 600);
        // reuse device for two marks
        map(mark, device3, 0, 600);
        map(mark2, device3, 0, 600);
        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(200, 10, 20, 30, 40));
        store.storeFix(device2, createFix(100, 10, 20, 30, 40));
        store.storeFix(device3, createFix(100, 10, 20));
        store.storeFix(device3, createFix(100, 10, 20));
        final List<GPSFixMoving> fixes = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            fixes.add(createFix(i + 1000, 10, 20, 30, 40));
        }
        store.storeFixes(device, fixes, /* returnManeuverUpdate */ false, /* returnLiveDelay */ false);
        DynamicTrackedRace trackedRace = createDynamicTrackedRace(boatClass, raceDefinition);
        new FixLoaderAndTracker(trackedRace, store, null, /* removeOutliersFromCompetitorTracks */ false);
        raceLog.add(new RaceLogStartOfTrackingEventImpl(TimePoint.BeginningOfTime, author, 0));
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getTrack(comp), 10002);
        testNumberOfRawFixes(trackedRace.getTrack(comp2), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark2), 1);
    }
    
    @Test
    public void testFixesForCompetitorsAreNotLoadedIfMappingDoesNotIntersectWithTrackingInterval()
            throws InterruptedException {
        testFixes(/* start of tracking */ 200, /* end of tracking */ 300, /* mappings and fixes */ () -> {
            map(comp, device, 0, 100);
            store.storeFix(device, createFix(10, 10, 20, 30, 40));
            store.storeFix(device, createFix(20, 10, 20, 30, 40));
        }, /* tests and expectations */ trackedRace -> testNumberOfRawFixes(trackedRace.getTrack(comp), 0));
    }
    
    @Test
    public void testFixesForCompetitorsAreLoadedIfMappingDoesIntersectWithTrackingIntervalFixesWithinIntersectionOnly()
            throws InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings and fixes */ () -> {
            map(comp, device, 0, 200);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));
            store.storeFix(device, createFix(150, 10, 20, 30, 40));
            store.storeFix(device, createFix(250, 10, 20, 30, 40));
            map(comp, device, 350, 500);
            store.storeFix(device, createFix(400, 10, 20, 30, 40));
            store.storeFix(device, createFix(450, 10, 20, 30, 40));
        }, /* tests and expectations */ trackedRace -> testNumberOfRawFixes(trackedRace.getTrack(comp), 1));
    }

    @Test
    public void testAddingMappingLoadsFixesPreviouslyNotCovered() throws InterruptedException {
        testFixes(/* start of tracking */ 10, /* end of tracking */ 500, /* mappings and fixes */ () -> {
            map(comp, device, 0, 200);
            map(comp, device, 300, 400);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));  // in first mapping
            store.storeFix(device, createFix(150, 10, 20, 30, 40)); // in first mapping
            store.storeFix(device, createFix(250, 10, 20, 30, 40)); // outside both mappings
            store.storeFix(device, createFix(350, 10, 20, 30, 40)); // in second mapping
        }, /* tests and expectations */ trackedRace -> {
            final int[] fixInsertionCount = new int[1];
            trackedRace.getTrack(comp).addListener(new GPSTrackListener<Competitor, GPSFixMoving>() {
                private static final long serialVersionUID = 1933664210766279523L;

                @Override
                public boolean isTransient() {
                    return true;
                }

                @Override
                public void gpsFixReceived(GPSFixMoving fix, Competitor item, boolean firstFixInTrack, AddResult addedOrReplaced) {
                    fixInsertionCount[0]++;
                }

                @Override
                public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage,
                        long newMillisecondsOverWhichToAverage) {
                    // no-op
                }
            });
            testNumberOfRawFixes(trackedRace.getTrack(comp), 3);
            map(comp, device, 0, 500);                              // covers fix at 250ms; assert that only that fix is loaded
            try {
                trackedRace.waitForLoadingToFinish();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception waiting for loading to finish", e);
            }
            testNumberOfRawFixes(trackedRace.getTrack(comp), 4);
            assertEquals(1, fixInsertionCount[0]);
        });
    }
    
    @Test
    public void testAddingMappingDoesNotLoadFixesAlreadyCoveredByExistingMapping() throws InterruptedException {
        testFixes(/* start of tracking */ 10, /* end of tracking */ 500, /* mappings and fixes */ () -> {
            map(comp, device, 0, 200);
            map(comp, device, 300, 400);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));  // in first mapping
            store.storeFix(device, createFix(150, 10, 20, 30, 40)); // in first mapping
            store.storeFix(device, createFix(250, 10, 20, 30, 40)); // outside both mappings
            store.storeFix(device, createFix(350, 10, 20, 30, 40)); // in second mapping
        }, /* tests and expectations */ trackedRace -> {
            map(comp, device, 0, 500);                              // covers fix at 250ms
            try {
                trackedRace.waitForLoadingToFinish();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception waiting for loading to finish", e);
            }
            store.storeFix(device, createFix(400, 10, 20, 30, 40)); // in third mapping
            testNumberOfRawFixes(trackedRace.getTrack(comp), 5);
        });
    }
    
    /** 
     * Regression test for bug 4008 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4008 <br>
     * Updated for bug 4009 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4009
     */
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesNotIntersectWithTrackingInterval() throws InterruptedException {
        final long startOfTracking = 200;
        final long bestFixOffset = 20;
        testFixes(/* start of tracking */ startOfTracking, /* end of tracking */ 300, /* mappings and fixes */ () -> {
            map(mark, device, 0, 100);
            store.storeFix(device, createFix(10, 10, 20));
            store.storeFix(device, createFix(bestFixOffset, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
            assertEquals(bestFixOffset, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
        });
    }
    
    /** Regression test for bug 4008 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4008 */
    @Test
    public void testFixesForTwoMarksAreLoadedIfMappingsDoNotIntersectWithTrackingInterval()
            throws InterruptedException {
        testFixes(/* start of tracking */ 400, /* end of tracking */ 500, /* mappings and fixes */ () -> {
            map(mark, device, 0, 100);
            store.storeFix(device, createFix(50, 10, 20));
            map(mark2, device, 200, 300);
            store.storeFix(device, createFix(250, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark2), 1);
        });
    }
    
    /** Regression test for bug 4008 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4008 */
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesIntersectWithTrackingIntervalFixesWithinIntersectionOnly()
            throws InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings and fixes */ () -> {
            map(mark, device, 0, 200);
            store.storeFix(device, createFix(50, 10, 20));
            store.storeFix(device, createFix(150, 10, 20));
            store.storeFix(device, createFix(250, 10, 20));
            
            map(mark, device, 350, 500);
            store.storeFix(device, createFix(400, 10, 20));
            store.storeFix(device, createFix(450, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    /**
     * Regression test for bug 4145: when fixes are mapped while startOfTracking is still null, fixes are expected
     * to not make it into the race. However, when startOfTracking is then set to a value after the fix's time point,
     * that fix is expected to be loaded if there is no better fix within the tracking interval, just like it already
     * happens when starting the tracker for the race or adding pings while startOfTracking is already set to a
     * non-null value.
     */
    @Test
    public void testFixesBeforeStartOfTrackingForMarkAreLoadedWhenSettingStartOfTracking() throws InterruptedException {
        testFixesInternal(/* start of tracking */ null, /* end of tracking */ null, /* mappings and fixes */ () -> {
            map(mark, device, 0, 200);
            store.storeFix(device, createFix(50, 10, 20));
            store.storeFix(device, createFix(150, 10, 20));
            store.storeFix(device, createFix(250, 10, 20));
            map(mark, device, 350, 500);
            store.storeFix(device, createFix(400, 10, 20));
            store.storeFix(device, createFix(450, 10, 20));
        }, /* tests track is initially empty */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 0); // still no start of tracking set
        }, /* now set start of tracking */ trackedRace -> trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(600)),
           /* now check the last fix has arrived */ trackedRace -> {
               testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1); // one fix
               trackedRace.getOrCreateTrack(mark).lockForRead();
               try {
                   final GPSFix fix = trackedRace.getOrCreateTrack(mark).getRawFixes().iterator().next();
                   assertEquals(new MillisecondsTimePoint(450), fix.getTimePoint());
               } finally {
                   trackedRace.getOrCreateTrack(mark).unlockAfterRead();
               }
        });
    }
    
    /** 
     * Regression test for bug 4008 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4008 <br>
     * Updated for bug 4009 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4009
     */
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesIntersectWithTrackingIntervalAllFixesBecauseOfNoFixesWithIntersection() 
        throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        final long bestFixBeforeStartOfTrackingOffset = 50;
        final long bestFixAfterEndOfTrackingOffset = 400;
        final long startOfTracking = 100;
        final long endOfTracking = 300;
        testFixes(/* start of tracking */ startOfTracking, /* end of tracking */ endOfTracking, /* mappings and fixes */ () -> {
            map(mark, device, 0, 200);
            store.storeFix(device, createFix(bestFixBeforeStartOfTrackingOffset, 10, 20));
            store.storeFix(device, createFix(250, 10, 20));
            map(mark, device, 350, 500);
            store.storeFix(device, createFix(bestFixAfterEndOfTrackingOffset, 10, 20));
            store.storeFix(device, createFix(450, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
            assertEquals(bestFixBeforeStartOfTrackingOffset, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
            assertEquals(bestFixAfterEndOfTrackingOffset, trackedRace.getOrCreateTrack(mark)
                    .getFirstFixAfter(new MillisecondsTimePoint(endOfTracking)).getTimePoint().asMillis());
        });
    }
    
    /** Test for changes of bug 4009 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4009 */
    @Test
    public void testThatOnlyTheBestFixesAreLoadedForMarksIfMultipleMappingsAreAvailable()
            throws InterruptedException {
        final long bestFixBeforeStartOfTrackingOffset = 140;
        final long bestFixAfterEndOfTrackingOffset = 360;
        final long startOfTracking = 200;
        final long endOfTracking = 300;
        testFixes(/* start of tracking */ startOfTracking, /* end of tracking */ endOfTracking, /* mappings and fixes */ () -> {
            map(mark, device, 0, 80);
            store.storeFix(device, createFix(50, 10, 20));
            store.storeFix(device, createFix(70, 10, 20));
            map(mark, device, 100, 150);
            store.storeFix(device, createFix(120, 10, 20));
            store.storeFix(device, createFix(bestFixBeforeStartOfTrackingOffset, 10, 20));
            
            map(mark, device, 400, 500);
            store.storeFix(device, createFix(420, 10, 20));
            store.storeFix(device, createFix(450, 10, 20));
            store.storeFix(device, createFix(480, 10, 20));
            map(mark, device, 350, 390);
            store.storeFix(device, createFix(bestFixAfterEndOfTrackingOffset, 10, 20));
            store.storeFix(device, createFix(380, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
            assertEquals(bestFixBeforeStartOfTrackingOffset, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
            assertEquals(bestFixAfterEndOfTrackingOffset, trackedRace.getOrCreateTrack(mark)
                    .getFirstFixAfter(new MillisecondsTimePoint(endOfTracking)).getTimePoint().asMillis());
        });
    }
    
    /** Test for changes of bug 4009 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4009 */
    @Test
    public void testYoungestFixIsAccurateOnSeveralAdditionsOfFixes() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        final long bestFix1 = 50;
        final long bestFix2 = 100;
        final long startOfTracking = 200;
        final long endOfTracking = 300;
        testFixes(/* start of tracking */ startOfTracking, /* end of tracking */ endOfTracking, /* mappings and fixes */ () -> {
            map(mark, device, bestFix1 - 20, bestFix1 + 10);
            store.storeFix(device, createFix(bestFix1 - 10, 10, 20));
            store.storeFix(device, createFix(bestFix1, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
            assertEquals(bestFix1, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
            store.storeFix(device, createFix(bestFix2 - 10, 10, 20));
            store.storeFix(device, createFix(bestFix2, 10, 20));
            map(mark, device, bestFix2 - 20, bestFix2 + 10);
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
            assertEquals(bestFix2, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
        });
    }
    
    /** 
     * Test for changes of bug 4009 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4009 <br>
     */
    @Test
    public void testLoadingOfFixesOutsideOfTrackingIntervalsEndsIfFixInIntervalIsFound() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        final long bestFix1 = 50;
        final long bestFix2 = 100;
        final long startOfTracking = 200;
        final long endOfTracking = 300;
        testFixes(/* start of tracking */ startOfTracking, /* end of tracking */ endOfTracking, /* mappings and fixes */ () -> {
            map(mark, device, bestFix1 - 10, bestFix1 + 10);
            store.storeFix(device, createFix(bestFix1, 10, 20));
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
            assertEquals(bestFix1, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
            store.storeFix(device, createFix(startOfTracking + 10, 10, 20));
            map(mark, device, startOfTracking, startOfTracking + 20);
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
            // would be the new best fix if there wasn't a fix in the tracking interval
            store.storeFix(device, createFix(bestFix2, 10, 20));
            map(mark, device, bestFix2 - 10, bestFix2 + 10);
        }, /* tests and expectations */ trackedRace -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
            assertEquals(bestFix1, trackedRace.getOrCreateTrack(mark)
                    .getLastFixBefore(new MillisecondsTimePoint(startOfTracking)).getTimePoint().asMillis());
        });
    }
    
    @Test
    public void testFixForMarkIsAddedIfBeforeTrackingIntervalAndNoOtherFixExists() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 200),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(50, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    @Test
    public void testFixForMarkIsAddedIfAfterTrackingIntervalAndNoOtherFixExists() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400), /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(350, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    @Test
    public void testFixForMarkIsAddedIfBeforeTrackingIntervalAndBetterThanAnExistingOne() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(50, 10, 20));
            store.storeFix(device, createFix(75, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
        });
    }
    
    @Test
    public void testFixForMarkIsNotAddedIfBeforeTrackingIntervalAndWorseThanAnExistingOne() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(75, 10, 20));
            store.storeFix(device, createFix(50, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    @Test
    public void testFixForMarkIsAddedIfAfterTrackingIntervalAndBetterThanAnExistingOne() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(375, 10, 20));
            store.storeFix(device, createFix(350, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
        });
    }
    
    @Test
    public void testFixForMarkIsNotAddedIfAfterTrackingIntervalAndWorseThanAnExistingOne() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(350, 10, 20));
            store.storeFix(device, createFix(375, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    @Test
    public void testFixForMarkIsNotAddedIfNotWithinTrackingIntervalAndFixWithinTrackingIntervalExists() 
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixes(/* start of tracking */ 100, /* end of tracking */ 300, /* mappings */ () -> map(mark, device, 0, 400),
                /* fixes and tests/expectations */ trackedRace -> {
            store.storeFix(device, createFix(200, 10, 20));
            store.storeFix(device, createFix(50, 10, 20));
            store.storeFix(device, createFix(350, 10, 20));
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }

    @SafeVarargs
    private final void testFixes(long startOfTracking, long endOfTracking, Runnable beforeTrackingStarted,
            Consumer<DynamicTrackedRace>... afterTrackingStartedConsumers) throws InterruptedException {
        testFixesInternal(Long.valueOf(startOfTracking), Long.valueOf(endOfTracking), beforeTrackingStarted, afterTrackingStartedConsumers);
    }
    
    @SafeVarargs
    private final void testFixesInternal(Long startOfTracking, Long endOfTracking, Runnable beforeTrackingStarted,
            Consumer<DynamicTrackedRace>... afterTrackingStartedConsumers) throws InterruptedException {
        defineMarksOnRegattaLog(mark, mark2);
        if (startOfTracking != null) {
            raceLog.add(new RaceLogStartOfTrackingEventImpl(new MillisecondsTimePoint(startOfTracking), author, 0));
        }
        if (endOfTracking != null) {
            raceLog.add(new RaceLogEndOfTrackingEventImpl(new MillisecondsTimePoint(endOfTracking), author, 0));
        }
        beforeTrackingStarted.run();
        DynamicTrackedRace trackedRace = createDynamicTrackedRace(boatClass, raceDefinition);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        new FixLoaderAndTracker(trackedRace, store, null, /* removeOutliersFromCompetitorTracks */ false);
        for(Consumer<DynamicTrackedRace> afterTrackingStarted : afterTrackingStartedConsumers) {
            trackedRace.waitForLoadingToFinish();
            afterTrackingStarted.accept(trackedRace);
        }
    }
    
    @Test
    public void metadataStoredInDb() throws TransformationException, NoCorrespondingServiceRegisteredException {
        assertEquals(0, store.getNumberOfFixes(device));
        assertEquals(null, store.getTimeRangeCoveredByFixes(device));
        map(comp, device, 0, 600);
        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(200, 10, 20, 30, 40));
        assertEquals(2, store.getNumberOfFixes(device));
        assertEquals(TimeRangeImpl.create(100, 200, /* toIsInclusive */ true), store.getTimeRangeCoveredByFixes(device));
        assertEquals(new DegreePosition(10, 20), ((GPSFix) store.getFixLastReceived(Collections.singleton(device)).get(device)).getPosition());
    }
    
    @Test
    public void testFindLatestFixForMapping() throws TransformationException, NoCorrespondingServiceRegisteredException {
        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(1100, 10, 20, 30, 40));
        store.storeFix(device, createFix(2100, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes = store.getFixLastReceived(Collections.singleton(device));
        assertEquals(1, lastFixes.size());
        Timed lastFix = lastFixes.get(device);
        assertEquals(2100, lastFix.getTimePoint().asMillis());
        store.storeFix(device, createFix(2000, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes2 = store.getFixLastReceived(Collections.singleton(device));
        assertEquals(1, lastFixes2.size());
        Timed lastFix2 = lastFixes2.get(device);
        assertEquals(2000, lastFix2.getTimePoint().asMillis()); // not the fix with the latest time point but the fix that was last received by the store
        store.storeFix(device, createFix(2200, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes3 = store.getFixLastReceived(Collections.singleton(device));
        assertEquals(1, lastFixes3.size());
        Timed lastFix3 = lastFixes3.get(device);
        assertEquals(2200, lastFix3.getTimePoint().asMillis());
        final DeviceIdentifier device2 = new SmartphoneImeiIdentifierImpl("b");
        store.storeFix(device2, createFix(1200, 10, 20, 30, 40));
        store.storeFix(device2, createFix(1100, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes4 = store.getFixLastReceived(Arrays.asList(device, device2));
        assertEquals(2, lastFixes4.size());
        Timed lastFix4 = lastFixes4.get(device);
        assertEquals(2200, lastFix4.getTimePoint().asMillis());
        Timed lastFixDevice2 = lastFixes4.get(device2);
        assertEquals(1100, lastFixDevice2.getTimePoint().asMillis());  // not the fix with the latest time point but the fix that was last received by the store
    }
}
