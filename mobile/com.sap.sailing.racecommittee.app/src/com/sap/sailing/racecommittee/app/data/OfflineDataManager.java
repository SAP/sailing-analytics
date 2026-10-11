package com.sap.sailing.racecommittee.app.data;

import android.content.Context;
import androidx.loader.app.LoaderManager.LoaderCallbacks;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceColumnFactorImpl;
import com.sap.sailing.domain.base.impl.StrippedEventImpl;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.base.racegroup.SeriesWithRows;
import com.sap.sailing.domain.base.racegroup.impl.RaceGroupImpl;
import com.sap.sailing.domain.base.racegroup.impl.SeriesWithRowsImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.data.loaders.ImmediateDataLoaderCallbacks;
import com.sap.sailing.racecommittee.app.domain.CoursePosition;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.configuration.impl.PreferencesRegattaConfigurationLoader;
import com.sap.sailing.racecommittee.app.domain.impl.LeaderboardResult;
import com.sap.sailing.racecommittee.app.domain.impl.ManagedRaceIdentifierImpl;
import com.sap.sailing.racecommittee.app.domain.impl.ManagedRaceImpl;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.PositionListFragment;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class OfflineDataManager extends DataManager {

    private static boolean isInitialized = false;
    private final Context context;

    protected OfflineDataManager(Context context, DataStore dataStore, SharedDomainFactory domainFactory) {
        super(context, dataStore, domainFactory);
        this.context = context;
        if (!isInitialized) {
            isInitialized = true;
            fillDataStore(dataStore);
        }
    }

    private void fillDataStore(DataStore dataStore) {
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 12, 1);
        final TimePoint startDate = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2012, 12, 5);
        final TimePoint endDate = new MillisecondsTimePoint(cal.getTimeInMillis());

        dataStore.addEvent(new StrippedEventImpl("Extreme Sailing Series 2012 (Cardiff)", startDate, endDate, "Cardiff",
                true, UUID.randomUUID(), Collections.<LeaderboardGroupBase>emptySet(), Collections.emptySet()));
        dataStore.addEvent(new StrippedEventImpl("Extreme Sailing Series 2012 (Nice)", startDate, endDate, "Nice", true,
                UUID.randomUUID(), Collections.<LeaderboardGroupBase>emptySet(), Collections.emptySet()));
        dataStore.addEvent(new StrippedEventImpl("Extreme Sailing Series 2012 (Rio)", startDate, endDate, "Rio", true,
                UUID.randomUUID(), Collections.<LeaderboardGroupBase>emptySet(), Collections.emptySet()));
        EventBase newEvent = new StrippedEventImpl("Extreme Sailing Series 2013 (Muscat)", startDate, endDate, "Muscat",
                true, UUID.randomUUID(), Collections.<LeaderboardGroupBase>emptySet(), Collections.emptySet());
        final CourseAreaImpl offshore = new CourseAreaImpl("Offshore", UUID.randomUUID(), /* centerPoint */ null, /* radius */ null);
        newEvent.getVenue().addCourseArea(offshore);
        final CourseAreaImpl stadium = new CourseAreaImpl("Stadium", UUID.randomUUID(), /* centerPoint */ null, /* radius */ null);
        newEvent.getVenue().addCourseArea(stadium);
        dataStore.addEvent(newEvent);

        SeriesWithRows qualifying = new SeriesWithRowsImpl("Qualifying", false, /* isFleetsCanRunInParallel */ true,
                null);
        SeriesWithRows medal = new SeriesWithRowsImpl("Medal", true, /* isFleetsCanRunInParallel */ true, null);
        RaceGroup raceGroup = new RaceGroupImpl("ESS", /* displayName */ null, new BoatClassImpl("X40", false), false,
                Arrays.asList(qualifying, medal), new EmptyRegattaConfiguration());

        List<Competitor> competitors = new ArrayList<Competitor>();
        competitors.add(new CompetitorImpl(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP", Color.BLUE, null, null,
                null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null));
        competitors.add(new CompetitorImpl(UUID.randomUUID(), "The Wave Muscat", "Muscat", Color.LIGHT_GRAY, null, null,
                null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null));
        competitors.add(new CompetitorImpl(UUID.randomUUID(), "Red Bull Extreme Sailing Team", "Red  Bull", Color.RED,
                null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null,
                null));
        competitors.add(new CompetitorImpl(UUID.randomUUID(), "Team Korea", "Korea", Color.GREEN, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null));
        competitors.add(new CompetitorImpl(UUID.randomUUID(), "Realteam", "Realteam", Color.BLACK, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null));

        RaceLog log = new RaceLogImpl(UUID.randomUUID());
        final AbstractLogEventAuthor author = AppPreferences.on(context).getAuthor();
        ConfigurationLoader<RegattaConfiguration> configuration = PreferencesRegattaConfigurationLoader
                .loadFromPreferences(preferences);
        log.add(new RaceLogStartTimeEventImpl(new MillisecondsTimePoint(new Date().getTime() - 2000), author, 1,
                new MillisecondsTimePoint(new Date().getTime() - 1000), /* course area ID */ stadium.getId()));
        log.add(new RaceLogRaceStatusEventImpl(new MillisecondsTimePoint(new Date().getTime()),
                AppPreferences.on(context).getAuthor(), 1, RaceLogRaceStatus.FINISHING));
        ManagedRace q1 = new ManagedRaceImpl(
                new ManagedRaceIdentifierImpl("A.B", new FleetImpl("A"), qualifying, raceGroup),
                RaceStateImpl.create(new AndroidRaceLogResolver(), log, AppPreferences.on(context).getAuthor(),
                        configuration),
                /* zeroBasedIndexInFleet */ 0);
        log = new RaceLogImpl(UUID.randomUUID());
        /*
         * log.add(factory.createStartTimeEvent( new MillisecondsTimePoint(new Date()), 1, RaceLogRaceStatus.SCHEDULED,
         * new MillisecondsTimePoint(new Date().getTime() + 100000)));
         */
        ManagedRace q2 = new ManagedRaceImpl(
                new ManagedRaceIdentifierImpl("B", new FleetImpl("A.A"), qualifying, raceGroup),
                RaceStateImpl.create(new AndroidRaceLogResolver(), log, AppPreferences.on(context).getAuthor(),
                        configuration),
                /* zeroBasedIndexInFleet */ 1);
        log = new RaceLogImpl(UUID.randomUUID());
        /*
         * log.add(factory.createRaceStatusEvent( new MillisecondsTimePoint(new Date()), 5,
         * RaceLogRaceStatus.FINISHED));
         */
        ManagedRace q3 = new ManagedRaceImpl(
                new ManagedRaceIdentifierImpl("Q3", new FleetImpl("Default"), qualifying, raceGroup),
                RaceStateImpl.create(new AndroidRaceLogResolver(), log, AppPreferences.on(context).getAuthor(),
                        configuration),
                /* zeroBasedIndexInFleet */ 2);
        /*
         * ManagedRace m1 = new ManagedRaceImpl( new ManagedRaceIdentifierImpl( "M1", new FleetImpl("Default"), medal,
         * raceGroup), null);
         */
        dataStore.addRace(q1);
        dataStore.addRace(q2);
        dataStore.addRace(q3);
        // dataStore.addRace(m1);
        Mark m1 = new MarkImpl("Red");
        Mark m2 = new MarkImpl("Green");
        Mark m3 = new MarkImpl("White");
        dataStore.addMark(raceGroup, m1);
        dataStore.addMark(raceGroup, m2);
        dataStore.addMark(raceGroup, m3);
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<EventBase>>> createEventsLoader(
            LoadClient<Collection<EventBase>> callback,
            UUID... eventIds
    ) {
        return new ImmediateDataLoaderCallbacks<>(context, callback, () -> {
            if (eventIds.length == 0) {
                return dataStore.getEvents();
            }
            final List<EventBase> events = new ArrayList<>();
            for (UUID eventId : eventIds) {
                events.add(dataStore.getEvent(eventId));
            }
            return events;
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            final Serializable parentEventId, LoadClient<Collection<CourseArea>> callback) {
        return new ImmediateDataLoaderCallbacks<Collection<CourseArea>>(context, callback,
                new Callable<Collection<CourseArea>>() {
                    @Override
                    public Collection<CourseArea> call() throws Exception {
                        return dataStore.getCourseAreas(dataStore.getEvent(parentEventId));
                    }
                });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            final EventBase parentEvent, LoadClient<Collection<CourseArea>> callback) {
        // TODO Auto-generated method stub
        return new ImmediateDataLoaderCallbacks<Collection<CourseArea>>(context, callback,
                new Callable<Collection<CourseArea>>() {
                    @Override
                    public Collection<CourseArea> call() throws Exception {
                        return dataStore.getCourseAreas(parentEvent);
                    }
                });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<ManagedRace>>> createRacesLoader(
            Serializable courseAreaId,
            LoadClient<Collection<ManagedRace>> callback
    ) {
        return new ImmediateDataLoaderCallbacks<Collection<ManagedRace>>(context, callback,
                new Callable<Collection<ManagedRace>>() {
                    @Override
                    public Collection<ManagedRace> call() throws Exception {
                        return dataStore.getRaces();
                    }
                });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<Mark>>> createMarksLoader(
            ManagedRace managedRace,
            LoadClient<Collection<Mark>> callback
    ) {
        return new ImmediateDataLoaderCallbacks<Collection<Mark>>(context, callback, new Callable<Collection<Mark>>() {
            @Override
            public Collection<Mark> call() throws Exception {
                return dataStore.getMarks(managedRace.getRaceGroup());
            }
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<CourseBase>> createCourseLoader(
            final ManagedRace managedRace,
            LoadClient<CourseBase> callback
    ) {
        return new ImmediateDataLoaderCallbacks<CourseBase>(context, callback, new Callable<CourseBase>() {
            @Override
            public CourseBase call() throws Exception {
                return managedRace.getCourseOnServer();
            }
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createCompetitorsLoader(
            final ManagedRace managedRace, LoadClient<Map<Competitor, Boat>> callback) {
        return new ImmediateDataLoaderCallbacks<Map<Competitor, Boat>>(context, callback,
                new Callable<Map<Competitor, Boat>>() {
                    @Override
                    public Map<Competitor, Boat> call() throws Exception {
                        return managedRace.getCompetitorsAndBoats();
                    }
                });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createStartOrderLoader(
            final ManagedRace managedRace, LoadClient<Map<Competitor, Boat>> callback) {
        return new ImmediateDataLoaderCallbacks<Map<Competitor, Boat>>(context, callback,
                new Callable<Map<Competitor, Boat>>() {
                    @Override
                    public Map<Competitor, Boat> call() throws Exception {
                        return managedRace.getCompetitorsAndBoats();
                    }
                });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<LeaderboardResult>> createLeaderboardLoader(
            ManagedRace managedRace,
            LoadClient<LeaderboardResult> callback
    ) {
        return new ImmediateDataLoaderCallbacks<>(context, callback, new Callable<LeaderboardResult>() {
            @Override
            public LeaderboardResult call() throws Exception {
                return null;
            }
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<DeviceConfiguration>> createConfigurationLoader(
            String deviceConfigurationName, UUID deviceConfigurationUuid, LoadClient<DeviceConfiguration> callback) {
        return new ImmediateDataLoaderCallbacks<DeviceConfiguration>(context, callback,
                new Callable<DeviceConfiguration>() {
                    @Override
                    public DeviceConfiguration call() throws Exception {
                        throw new IllegalStateException("No remote configuration in offline mode.");
                    }
                });
    }

    @Override
    public String getMapUrl(String baseUrl, ManagedRace race, String eventId, boolean showWindCharts,
                            boolean showStreamlets, boolean showSimulation, boolean showMapControls) {
        throw new IllegalStateException("No wind map in offline mode.");
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CoursePosition>>> createPositionLoader(
            PositionListFragment positionListFragment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<RaceColumnFactorImpl>> createRaceColumnFactorLoader(
            LoadClient<RaceColumnFactorImpl> callback) {
        return null;
    }
}
