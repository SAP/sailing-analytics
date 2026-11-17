package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.DomainObjectFactory.ConnectivityParametersLoadingResult;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.media.MediaDB;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;
import com.sap.sailing.server.impl.RacingEventServiceImpl;

/** Exercises the server's startup wiring with the real gated polar service and no database. */
public class RacingEventServicePolarStartupTest {
    private final DomainObjectFactory domainObjectFactory = mock(DomainObjectFactory.class, RETURNS_MOCKS);
    private final MongoObjectFactory mongoObjectFactory = mock(MongoObjectFactory.class);

    private RacingEventServiceImpl createService(final boolean restoreTrackedRaces) {
        return new RacingEventServiceImpl(domainObjectFactory, mongoObjectFactory, mock(MediaDB.class),
                mock(WindStore.class), mock(SensorFixStore.class), restoreTrackedRaces);
    }

    private TrackedRace createRace() {
        final RaceDefinition definition = mock(RaceDefinition.class);
        when(definition.getName()).thenReturn("Polar startup regression");
        when(definition.getCompetitors()).thenReturn(Collections.emptyList());
        final TrackedRace race = mock(TrackedRace.class);
        when(race.getRace()).thenReturn(definition);
        return race;
    }

    private void await(final CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(/* timeout */ 5, TimeUnit.SECONDS), "Startup callback did not complete");
    }

    @Test
    public void testNoRestoreReleasesCallbackRegisteredBeforeServiceArrival() throws InterruptedException {
        final RacingEventServiceImpl service = createService(/* restoreTrackedRaces */ false);
        final PolarDataServiceImpl polars = new PolarDataServiceImpl(/* waitForLoadingOfAllRacesToRestoreToBeStarted */ true);
        final TrackedRace race = createRace();
        final CountDownLatch completed = new CountDownLatch(/* count */ 1);
        try {
            polars.runWhenPolarLoadingFinishedFor(race, completed::countDown);
            polars.raceFinishedLoading(race, /* callbackWhenRaceChangingToTrackingOfFinishedStatus */ null);
            assertEquals(/* expected */ 1, completed.getCount());
            service.setPolarDataService(polars);
            await(completed);
            verify(domainObjectFactory, never()).loadConnectivityParametersForRacesToRestore(any());
            verify(mongoObjectFactory).removeAllConnectivityParametersForRacesToRestore();
        } finally {
            polars.raceRemoved(race);
        }
    }

    @Test
    public void testNoRestoreStillWaitsForSubsequentRaceIngestion() throws InterruptedException {
        final RacingEventServiceImpl service = createService(/* restoreTrackedRaces */ false);
        final PolarDataServiceImpl polars = new PolarDataServiceImpl(/* waitForLoadingOfAllRacesToRestoreToBeStarted */ true);
        final TrackedRace race = createRace();
        final CountDownLatch completed = new CountDownLatch(/* count */ 1);
        try {
            service.setPolarDataService(polars);
            polars.runWhenPolarLoadingFinishedFor(race, completed::countDown);
            assertEquals(/* expected */ 1, completed.getCount(), "Service arrival must not bypass race ingestion");
            polars.raceFinishedLoading(race, /* callbackWhenRaceChangingToTrackingOfFinishedStatus */ null);
            await(completed);
        } finally {
            polars.raceRemoved(race);
        }
    }

    @Test
    public void testRestoreEnabledKeepsSignalInRestorationTask() throws InterruptedException {
        final CountDownLatch restorationStarted = new CountDownLatch(/* count */ 1);
        final CountDownLatch releaseRestoration = new CountDownLatch(/* count */ 1);
        final ConnectivityParametersLoadingResult loadingResult = mock(ConnectivityParametersLoadingResult.class);
        when(domainObjectFactory.loadConnectivityParametersForRacesToRestore(any())).thenAnswer((final InvocationOnMock invocation) -> {
            restorationStarted.countDown();
            await(releaseRestoration);
            return loadingResult;
        });
        final RacingEventServiceImpl service = createService(/* restoreTrackedRaces */ true);
        final PolarDataServiceImpl polars = spy(new PolarDataServiceImpl(/* waitForLoadingOfAllRacesToRestoreToBeStarted */ true));
        final TrackedRace race = createRace();
        final CountDownLatch completed = new CountDownLatch(/* count */ 1);
        try {
            verify(domainObjectFactory, never()).loadConnectivityParametersForRacesToRestore(any());
            service.setPolarDataService(polars);
            await(restorationStarted);
            polars.runWhenPolarLoadingFinishedFor(race, completed::countDown);
            polars.raceFinishedLoading(race, /* callbackWhenRaceChangingToTrackingOfFinishedStatus */ null);
            verify(polars, never()).markLoadingOfAllRacesToRestoreStarted();
            assertEquals(/* expected */ 1, completed.getCount());
            releaseRestoration.countDown();
            await(completed);
            verify(polars).markLoadingOfAllRacesToRestoreStarted();
            verify(mongoObjectFactory, never()).removeAllConnectivityParametersForRacesToRestore();
        } finally {
            releaseRestoration.countDown();
            polars.raceRemoved(race);
        }
    }

    @Test
    public void testNullAndDuplicateServiceNotificationsDoNotRepeatSignal() {
        final RacingEventServiceImpl service = createService(/* restoreTrackedRaces */ false);
        final PolarDataService polars = mock(PolarDataService.class);
        service.setPolarDataService(null);
        service.setPolarDataService(polars);
        service.setPolarDataService(polars);
        service.setPolarDataService(null);
        assertSame(polars, service.getPolarDataService());
        final InOrder initialization = inOrder(polars);
        initialization.verify(polars).registerDomainFactory(service.getBaseDomainFactory());
        initialization.verify(polars).markLoadingOfAllRacesToRestoreStarted();
        initialization.verifyNoMoreInteractions();
    }
}
