package com.sap.sailing.racecommittee.app.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import android.util.Log;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.impl.RaceColumnFactorImpl;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.common.racelog.RaceLogServletConstants;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.deserializer.LeaderboardDeserializer;
import com.sap.sailing.racecommittee.app.data.handlers.CompetitorsDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.CourseBaseHandler;
import com.sap.sailing.racecommittee.app.data.handlers.DataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.EventsDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.LeaderboardResultDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.ManagedRacesDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.MarksDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.NullDataHandler;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderCallbacks;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderCallbacks.LoaderCreator;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.data.loaders.ImmediateDataLoaderCallbacks;
import com.sap.sailing.racecommittee.app.data.loaders.OnlineDataLoader;
import com.sap.sailing.racecommittee.app.data.parsers.CompetitorsDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.CourseBaseParser;
import com.sap.sailing.racecommittee.app.data.parsers.DataParser;
import com.sap.sailing.racecommittee.app.data.parsers.DeviceConfigurationParser;
import com.sap.sailing.racecommittee.app.data.parsers.EventsDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.LeaderboardDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.ManagedRacesDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.MarksDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.RaceColumnsParser;
import com.sap.sailing.racecommittee.app.domain.CoursePosition;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.ManagedRaceIdentifier;
import com.sap.sailing.racecommittee.app.domain.configuration.impl.PreferencesRegattaConfigurationLoader;
import com.sap.sailing.racecommittee.app.domain.impl.FleetIdentifierImpl;
import com.sap.sailing.racecommittee.app.domain.impl.LeaderboardResult;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.PositionListFragment;
import com.sap.sailing.racecommittee.app.utils.UrlHelper;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.ControlPointDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.CourseBaseDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.GateDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.MarkDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.WaypointDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorAndBoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceConfigurationJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.RaceColumnFactorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.RegattaConfigurationJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racegroup.impl.RaceGroupDeserializer;
import com.sap.sse.common.Util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Enables accessing of data.
 */
public class OnlineDataManager extends DataManager {
    private static final String TAG = OnlineDataManager.class.getName();

    public static final String LEADERBOARD = "leaderboard";
    public static final String RACE_COLUMN = "raceColumn";

    protected OnlineDataManager(Context context, DataStore dataStore, SharedDomainFactory domainFactory) {
        super(context, dataStore, domainFactory);
    }

    public void addEvents(Collection<EventBase> events) {
        for (EventBase event : events) {
            dataStore.addEvent(event);
        }
    }

    public void addRaces(Collection<ManagedRace> data) {
        int index = 0;
        for (ManagedRace race : data) {
            dataStore.addRace(index++, race);
        }
    }

    public void addMarks(RaceGroup raceGroup, Collection<Mark> marks) {
        for (Mark mark : marks) {
            dataStore.addMark(raceGroup, mark);
        }
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<EventBase>>> createEventsLoader(
            LoadClient<Collection<EventBase>> callback,
            UUID... eventIds
    ) {
        return new DataLoaderCallbacks<>(callback, (id, args) -> {
            ExLog.i(context, TAG, "Creating Events-OnlineDataLoader " + id);
            final EventBaseJsonDeserializer serializer = new EventBaseJsonDeserializer(
                    new VenueJsonDeserializer(new CourseAreaJsonDeserializer(domainFactory)),
                    new LeaderboardGroupBaseJsonDeserializer(),
                    new TrackingConnectorInfoJsonDeserializer()
            );
            final DataParser<Collection<EventBase>> parser = new EventsDataParser(serializer);
            final DataHandler<Collection<EventBase>> handler = new EventsDataHandler(OnlineDataManager.this);
            final List<Util.Pair<String, Object>> params = new ArrayList<>();
            if (eventIds.length > 0) {
                params.add(new Util.Pair<>("showNonPublic", "true"));
                params.add(new Util.Pair<>("include", true));
                for (UUID eventId : eventIds) {
                    params.add(new Util.Pair<>("id", eventId));
                }
            } else {
                params.add(new Util.Pair<>("showNonPublic", AppPreferences.on(context).showNonPublic()));
            }
            URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/api/v1/events", params);
            return new OnlineDataLoader<>(context, url, parser, handler);
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            final Serializable parentEventId, LoadClient<Collection<CourseArea>> callback) {
        return new ImmediateDataLoaderCallbacks<>(context, callback, new Callable<Collection<CourseArea>>() {
            @Override
            public Collection<CourseArea> call() throws Exception {
                if (dataStore.hasEvent(parentEventId)) {
                    EventBase event = dataStore.getEvent(parentEventId);
                    return dataStore.getCourseAreas(event);
                } else {
                    // throw new DataLoadingException(String.format(
                    // "Unable to load course areas for unknown event %s.", parentEventId));
                    // TODO: Quickfix for #2889
                    return null;
                }
            }
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CourseArea>>> createCourseAreasLoader(
            final EventBase parentEvent, LoadClient<Collection<CourseArea>> callback) {
        // TODO Auto-generated method stub
        return new ImmediateDataLoaderCallbacks<>(context, callback, new Callable<Collection<CourseArea>>() {
            @Override
            public Collection<CourseArea> call() throws Exception {
                return dataStore.getCourseAreas(parentEvent);
            }
        });
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<ManagedRace>>> createRacesLoader(
            final Serializable courseAreaId,
            LoadClient<Collection<ManagedRace>> callback
    ) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<Collection<ManagedRace>>() {
            @Override
            public Loader<DataLoaderResult<Collection<ManagedRace>>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating races-OnlineDataLoader " + id);
                ConfigurationLoader<RegattaConfiguration> globalConfiguration = PreferencesRegattaConfigurationLoader
                        .loadFromPreferences(preferences);

                DataParser<Collection<ManagedRace>> parser = new ManagedRacesDataParser(preferences.getAuthor(),
                        globalConfiguration,
                        RaceGroupDeserializer.create(domainFactory, RegattaConfigurationJsonDeserializer.create()));
                DataHandler<Collection<ManagedRace>> handler = new ManagedRacesDataHandler(context,
                        OnlineDataManager.this);
                List<Util.Pair<String, Object>> params = new ArrayList<>();
                params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_COURSE_AREA_FILTER,
                        courseAreaId.toString()));
                params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_CLIENT_UUID,
                        MessageSendingService.uuid));
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/rc/racegroups", params);
                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<Mark>>> createMarksLoader(
            final ManagedRace managedRace,
            LoadClient<Collection<Mark>> callback
    ) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<Collection<Mark>>() {
            @Override
            public Loader<DataLoaderResult<Collection<Mark>>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating marks-OnlineDataLoader " + id);
                JsonDeserializer<Mark> markDeserializer = new MarkDeserializer(domainFactory);
                DataParser<Collection<Mark>> parser = new MarksDataParser(markDeserializer);
                DataHandler<Collection<Mark>> handler = new MarksDataHandler(OnlineDataManager.this, managedRace.getRaceGroup());

                ManagedRaceIdentifier identifier = managedRace.getIdentifier();
                // no parameter encoding required here; the UrlHelper.generateUrl call uses Url.Builder which handles
                // encoding
                String raceGroupName = identifier.getRaceGroup().getName();
                String raceColumnName = identifier.getRaceColumnName();
                String fleetName = identifier.getFleet().getName();

                List<Util.Pair<String, Object>> params = new ArrayList<>();
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME, raceGroupName));
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME, raceColumnName));
                params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME, fleetName));
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/rc/marks", params);
                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<CourseBase>> createCourseLoader(
            final ManagedRace managedRace,
            LoadClient<CourseBase> callback
    ) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<CourseBase>() {
            @Override
            public Loader<DataLoaderResult<CourseBase>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating Course-OnlineDataLoader " + id);
                JsonDeserializer<CourseBase> courseBaseDeserializer = new CourseBaseDeserializer(
                        new WaypointDeserializer(new ControlPointDeserializer(new MarkDeserializer(domainFactory),
                                new GateDeserializer(domainFactory, new MarkDeserializer(domainFactory)))));
                DataParser<CourseBase> parser = new CourseBaseParser(courseBaseDeserializer);
                DataHandler<CourseBase> handler = new CourseBaseHandler(OnlineDataManager.this, managedRace);

                ManagedRaceIdentifier identifier = managedRace.getIdentifier();

                String raceGroupName = identifier.getRaceGroup().getName();
                String raceColumnName = identifier.getRaceColumnName();
                String fleetName = identifier.getFleet().getName();

                List<Util.Pair<String, Object>> params = new ArrayList<>();
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME, raceGroupName));
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME, raceColumnName));
                params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME, fleetName));
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/rc/currentcourse",
                        params);

                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createCompetitorsLoader(
            final ManagedRace managedRace, LoadClient<Map<Competitor, Boat>> callback) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<Map<Competitor, Boat>>() {
            @Override
            public Loader<DataLoaderResult<Map<Competitor, Boat>>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating Competitor-OnlineDataLoader " + id);
                CompetitorAndBoatJsonDeserializer competitorAndBoatDeserializer = CompetitorAndBoatJsonDeserializer
                        .create(domainFactory);
                DataParser<Map<Competitor, Boat>> parser = new CompetitorsDataParser(competitorAndBoatDeserializer);
                DataHandler<Map<Competitor, Boat>> handler = new CompetitorsDataHandler(OnlineDataManager.this,
                        managedRace);
                ManagedRaceIdentifier identifier = managedRace.getIdentifier();
                String raceGroupName = identifier.getRaceGroup().getName();
                String raceColumnName = identifier.getRaceColumnName();
                String fleetName = identifier.getFleet().getName();
                List<Util.Pair<String, Object>> params = new ArrayList<>();
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME, raceGroupName));
                params.add(
                        new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME, raceColumnName));
                params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME, fleetName));
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/rc/competitorsAndBoats",
                        params);
                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Map<Competitor, Boat>>> createStartOrderLoader(
            final ManagedRace managedRace, LoadClient<Map<Competitor, Boat>> callback) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<Map<Competitor, Boat>>() {
            @Override
            public Loader<DataLoaderResult<Map<Competitor, Boat>>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating StartOrder-Competitor-OnlineDataLoader " + id);
                CompetitorAndBoatJsonDeserializer competitorAndBoatDeserializer = CompetitorAndBoatJsonDeserializer
                        .create(domainFactory);
                DataParser<Map<Competitor, Boat>> parser = new CompetitorsDataParser(competitorAndBoatDeserializer);
                DataHandler<Map<Competitor, Boat>> handler = new CompetitorsDataHandler(OnlineDataManager.this,
                        managedRace);
                Uri.Builder uri = Uri.parse(preferences.getServerBaseURL()).buildUpon();
                uri.appendPath("sailingserver");
                uri.appendPath("api");
                uri.appendPath("v1");
                uri.appendPath("leaderboards");
                uri.appendPath(managedRace.getIdentifier().getRaceGroup().getName());
                uri.appendPath("startorder");
                uri.appendQueryParameter(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME,
                        managedRace.getIdentifier().getRaceColumnName());
                uri.appendQueryParameter(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME,
                        managedRace.getIdentifier().getFleet().getName());
                return new OnlineDataLoader<>(context, new URL(uri.build().toString()), parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<LeaderboardResult>> createLeaderboardLoader(
            final ManagedRace managedRace,
            LoadClient<LeaderboardResult> callback
    ) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<LeaderboardResult>() {
            @Override
            public Loader<DataLoaderResult<LeaderboardResult>> create(int id, Bundle args) throws Exception {
                JsonDeserializer<LeaderboardResult> competitorDeserializer = new LeaderboardDeserializer();
                DataParser<LeaderboardResult> parser = new LeaderboardDataParser(competitorDeserializer);
                DataHandler<LeaderboardResult> handler = new LeaderboardResultDataHandler(OnlineDataManager.this);

                Uri.Builder uri = Uri.parse(preferences.getServerBaseURL()).buildUpon();
                uri.appendPath("sailingserver");
                uri.appendPath("api");
                uri.appendPath("v1");
                uri.appendPath("leaderboards");
                uri.appendPath(managedRace.getIdentifier().getRaceGroup().getName());
                return new OnlineDataLoader<>(context, new URL(uri.build().toString()), parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<DeviceConfiguration>> createConfigurationLoader(
            final String deviceConfigurationName, UUID deviceConfigurationUuid, LoadClient<DeviceConfiguration> callback) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<DeviceConfiguration>() {
            @Override
            public Loader<DataLoaderResult<DeviceConfiguration>> create(int id, Bundle args) throws Exception {
                ExLog.i(context, TAG, "Creating Configuration-OnlineDataLoader " + id);
                DataHandler<DeviceConfiguration> handler = new NullDataHandler<DeviceConfiguration>();
                DataParser<DeviceConfiguration> parser = new DeviceConfigurationParser(
                        DeviceConfigurationJsonDeserializer.create());
                List<Util.Pair<String, Object>> params = new ArrayList<>();
                if (deviceConfigurationName != null) {
                    params.add(new Util.Pair<String, Object>("client", deviceConfigurationName));
                }
                if (deviceConfigurationUuid != null) {
                    params.add(new Util.Pair<String, Object>("uuid", deviceConfigurationUuid.toString()));
                }
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(), "/sailingserver/rc/configuration", params);
                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<Collection<CoursePosition>>> createPositionLoader(
            PositionListFragment positionListFragment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMapUrl(String baseUrl, ManagedRace race, String eventId, boolean showWindCharts,
                            boolean showStreamlets, boolean showSimulation, boolean showMapControls) {
        String url = "";
        // get simple race log identifier
        Util.Triple<String, String, String> triple = FleetIdentifierImpl.unescape(race.getId());
        SimpleRaceLogIdentifier identifier = new SimpleRaceLogIdentifierImpl(triple.getA(), triple.getB(),
                triple.getC());
        List<Util.Pair<String, Object>> params = new ArrayList<>();
        params.add(new Util.Pair<String, Object>("regattaLikeName", identifier.getRegattaLikeParentName()));
        params.add(new Util.Pair<String, Object>("raceColumnName", identifier.getRaceColumnName()));
        params.add(new Util.Pair<String, Object>("fleetName", identifier.getFleetName()));
        params.add(new Util.Pair<String, Object>("eventId", eventId));
        params.add(new Util.Pair<String, Object>("viewShowWindChart", showWindCharts));
        params.add(new Util.Pair<String, Object>("viewShowStreamlets", showStreamlets));
        params.add(new Util.Pair<String, Object>("viewShowSimulation", showSimulation));
        params.add(new Util.Pair<String, Object>("viewShowMapControls", showMapControls));

        try {
            url = UrlHelper.generateUrl(AppPreferences.on(context).getServerBaseURL(),
                    AppConstants.GWT_MAP_AND_WIND_CHART_HTML, params).toString();
        } catch (MalformedURLException e) {
            Log.e(TAG, "An error occured while generating the map url: " + e.getMessage());
        }
        return url;
    }

    @Override
    public LoaderCallbacks<DataLoaderResult<RaceColumnFactorImpl>> createRaceColumnFactorLoader(
            LoadClient<RaceColumnFactorImpl> callback) {
        return new DataLoaderCallbacks<>(callback, new LoaderCreator<RaceColumnFactorImpl>() {
            @Override
            public Loader<DataLoaderResult<RaceColumnFactorImpl>> create(int id, Bundle args) throws Exception {
                if (args == null || args.getString(LEADERBOARD) == null) {
                    throw new IllegalArgumentException("You need an leaderboard as bundle arg (" + LEADERBOARD + ").");
                }
                ExLog.i(context, TAG, "Creating RaceColumnFactorLoader " + id);
                DataHandler<RaceColumnFactorImpl> handler = new NullDataHandler<>();
                DataParser<RaceColumnFactorImpl> parser = new RaceColumnsParser(new RaceColumnFactorJsonDeserializer());

                List<Util.Pair<String, Object>> params = new ArrayList<>();
                if (args.getString(RACE_COLUMN) != null) {
                    params.add(new Util.Pair<String, Object>(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME,
                            args.getString(RACE_COLUMN)));
                }
                URL url = UrlHelper.generateUrl(preferences.getServerBaseURL(),
                        "/sailingserver/api/v1/leaderboards/" + args.getString(LEADERBOARD) + "/racecolumnfactors",
                        params);
                return new OnlineDataLoader<>(context, url, parser, handler);
            }
        }, getContext());
    }
}
