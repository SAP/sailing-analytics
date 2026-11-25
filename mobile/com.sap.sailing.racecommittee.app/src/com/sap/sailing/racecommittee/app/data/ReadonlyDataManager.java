package com.sap.sailing.racecommittee.app.data;

import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.impl.RaceColumnFactorImpl;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.domain.CoursePosition;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.impl.LeaderboardResult;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.PositionListFragment;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * Interface for everything read related when working with data.
 * </p>
 *
 * <p>
 * The result values of this interface's methods are meant to be used in conjunction with the Android's
 * {@link LoaderManager} facility. Everything that is loaded by this interface is stored and cached in the underlying
 * {@link DataStore}.
 * </p>
 *
 * <p>
 * If there are cached results your {@link LoadClient}'s callback methods might be called twice (first cached results;
 * afterwards remote results). See {@link LoadClient} for restrictions that apply to your data handling code.
 * </p>
 */
public interface ReadonlyDataManager {

    /**
     * Gets the underlying data store.
     *
     * @return the {@link DataStore}.
     */
    DataStore getDataStore();

    /**
     * Creates a new {@link LoaderCallbacks} object for loading {@link EventBase}s.
     *
     * @param callback {@link LoadClient} implementing your data handling code.
     * @param eventIds Event IDs to filter the results.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Collection<EventBase>>> createEventsLoader(
            LoadClient<Collection<EventBase>> callback,
            UUID... eventIds
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading {@link CourseArea}s.
     *
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            Serializable parentEventId,
            LoadClient<Collection<CourseArea>> callback
    );

    LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            EventBase parentEvent,
            LoadClient<Collection<CourseArea>> callback
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading racing referee positions.
     *
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Collection<CoursePosition>>> createPositionLoader(
            PositionListFragment positionListFragment);

    /**
     * Creates a new {@link LoaderCallbacks} object for loading {@link ManagedRace}s of a specific {@link CourseArea}.
     *
     * @param courseAreaId the {@link CourseArea}'s Identifier.
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Collection<ManagedRace>>> createRacesLoader(
            Serializable courseAreaId,
            LoadClient<Collection<ManagedRace>> callback
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading a race's {@link Mark}s.
     *
     * @param managedRace the {@link ManagedRace}.
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Collection<Mark>>> createMarksLoader(
            ManagedRace managedRace,
            LoadClient<Collection<Mark>> callback
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading a race's {@link CourseBase}.
     *
     * @param managedRace the {@link ManagedRace}.
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<CourseBase>> createCourseLoader(
            ManagedRace managedRace,
            LoadClient<CourseBase> callback
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading {@link Competitor}s.
     *
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createCompetitorsLoader(
            ManagedRace managedRace,
            LoadClient<Map<Competitor, Boat>> callback
    );

    /**
     * Create a new {@link LoaderCallbacks} object for loading {@link Competitor}
     *
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createStartOrderLoader(
            ManagedRace managedRace,
            LoadClient<Map<Competitor, Boat>> callback
    );

    /**
     * Create a new {@link LoaderCallbacks} object for loading {@link LeaderboardResult}
     *
     * @param managedRace the {@link ManagedRace}
     * @param callback {@link LoadClient} implementing your data handling code
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<LeaderboardResult>> createLeaderboardLoader(
            ManagedRace managedRace,
            LoadClient<LeaderboardResult> callback
    );

    /**
     * Creates a new {@link LoaderCallbacks} object for loading a client's configuration.
     *
     * @param deviceConfigurationUuid
     * @param callback {@link LoadClient} implementing your data handling code.
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<DeviceConfiguration>> createConfigurationLoader(
            String deviceConfigurationName, UUID deviceConfigurationUuid, LoadClient<DeviceConfiguration> callback);

    String getMapUrl(String baseUrl, ManagedRace race, String eventId, boolean showWindCharts, boolean showStreamlets,
                     boolean showSimulation, boolean showMapControls);

    /**
     * Create a new {@link LoaderCallbacks} object for loading the race columns (e.g. factors)
     *
     * @param leaderboard
     * @param race_column
     * @param callback
     * @return {@link LoaderCallbacks} to be used in
     * {@link LoaderManager#initLoader(int, android.os.Bundle, LoaderCallbacks)} or
     * {@link LoaderManager#restartLoader(int, android.os.Bundle, LoaderCallbacks)}.
     */
    LoaderCallbacks<DataLoaderResult<RaceColumnFactorImpl>> createRaceColumnFactorLoader(
            LoadClient<RaceColumnFactorImpl> callback);
}
