package com.sap.sailing.racecommittee.app.data;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collection;

import android.content.Context;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.handlers.CompetitorsDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.CourseDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.DataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.EventsDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.ManagedRacesDataHandler;
import com.sap.sailing.racecommittee.app.data.handlers.MarksDataHandler;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoader;
import com.sap.sailing.racecommittee.app.data.parsers.CompetitorsDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.CourseDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.DataParser;
import com.sap.sailing.racecommittee.app.data.parsers.EventsDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.ManagedRacesDataParser;
import com.sap.sailing.racecommittee.app.data.parsers.MarksDataParser;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.ManagedRaceIdentifier;
import com.sap.sailing.racecommittee.app.domain.impl.DomainFactoryImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.ControlPointDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.CourseDataDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.GateDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.MarkDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.WaypointDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.BoatClassJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.ColorDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.FleetDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racegroup.impl.RaceCellDeserializer;
import com.sap.sailing.server.gateway.deserialization.racegroup.impl.RaceGroupDeserializer;
import com.sap.sailing.server.gateway.deserialization.racegroup.impl.RaceRowDeserializer;
import com.sap.sailing.server.gateway.deserialization.racegroup.impl.SeriesWithRowsDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogEventDeserializer;

/**
 * Enables accessing of data.
 */
public class OnlineDataManager extends DataManager {
    // private static final String TAG = OnlineDataManager.class.getName();

    private Context context;

    OnlineDataManager(Context context, DataStore dataStore) {
        super(dataStore);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void loadEvents(LoadClient<Collection<EventBase>> client) {
        if (dataStore.getEvents().isEmpty()) {
            reloadEvents(client);
        } else {
            client.onLoadSucceded(dataStore.getEvents());
        }
    }

    public void addEvents(Collection<EventBase> events) {
        for (EventBase event : events) {
            dataStore.addEvent(event);
        }
    }

    protected void reloadEvents(LoadClient<Collection<EventBase>> client) {
        SharedDomainFactory domainFactory = DomainFactoryImpl.INSTANCE;
        DataParser<Collection<EventBase>> parser = new EventsDataParser(new EventBaseJsonDeserializer(
                new VenueJsonDeserializer(new CourseAreaJsonDeserializer(domainFactory))));
        DataHandler<Collection<EventBase>> handler = new EventsDataHandler(this, client);

        try {
            new DataLoader<Collection<EventBase>>(context, URI.create(AppPreferences.getServerBaseURL(context)
                    + "/sailingserver/events"), parser, handler).forceLoad();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadCourseAreas(final Serializable parentEventId, final LoadClient<Collection<CourseArea>> client) {

        if (dataStore.hasEvent(parentEventId)) {
            EventBase event = dataStore.getEvent(parentEventId);
            client.onLoadSucceded(dataStore.getCourseAreas(event));
        } else {
            reloadEvents(new LoadClient<Collection<EventBase>>() {
                public void onLoadSucceded(Collection<EventBase> data) {
                    if (dataStore.hasEvent(parentEventId)) {
                        EventBase event = dataStore.getEvent(parentEventId);
                        client.onLoadSucceded(dataStore.getCourseAreas(event));
                    } else {
                        client.onLoadFailed(new DataLoadingException(String.format(
                                "There was no event object found for id %s.", parentEventId)));
                    }
                }

                public void onLoadFailed(Exception reason) {
                    client.onLoadFailed(new DataLoadingException(
                            String.format(
                                    "There was no event object found for id %s. While reloading the events an error occured: %s",
                                    parentEventId, reason), reason));
                }
            });
        }
    }

    public void addRaces(Collection<ManagedRace> data) {
        for (ManagedRace race : data) {
            dataStore.addRace(race);
        }
    }

    public void loadRaces(Serializable courseAreaId, LoadClient<Collection<ManagedRace>> client) {
        if (!dataStore.hasCourseArea(courseAreaId)) {
            client.onLoadFailed(new DataLoadingException(String.format("No course area found with id %s", courseAreaId)));
            return;
        }
        SharedDomainFactory domainFactory = DomainFactoryImpl.INSTANCE;
        JsonDeserializer<BoatClass> boatClassDeserializer = new BoatClassJsonDeserializer(domainFactory);
        DataParser<Collection<ManagedRace>> parser = new ManagedRacesDataParser(new RaceGroupDeserializer(
                boatClassDeserializer, new SeriesWithRowsDeserializer(new RaceRowDeserializer(new FleetDeserializer(
                        new ColorDeserializer()), new RaceCellDeserializer(
                                new RaceLogDeserializer(RaceLogEventDeserializer.create(domainFactory)))))));
        DataHandler<Collection<ManagedRace>> handler = new ManagedRacesDataHandler(this, client);
        try {
            new DataLoader<Collection<ManagedRace>>(context, URI.create(AppPreferences.getServerBaseURL(context)
                    + "/sailingserver/rc/racegroups?courseArea=" + courseAreaId.toString()), parser, handler)
                    .forceLoad();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void addMarks(Collection<Mark> marks) {
        for (Mark mark : marks) {
            dataStore.addMark(mark);
        }
    }
    
    public void loadMarks(ManagedRace managedRace, LoadClient<Collection<Mark>> client) {
        SharedDomainFactory domainFactory = DomainFactoryImpl.INSTANCE;
        JsonDeserializer<Mark> markDeserializer = new MarkDeserializer(domainFactory);
        DataParser<Collection<Mark>> parser = new MarksDataParser(markDeserializer);
        DataHandler<Collection<Mark>> handler = new MarksDataHandler(this, client);
        
        ManagedRaceIdentifier identifier = managedRace.getIdentifier();
        
        String raceGroupName = URLEncoder.encode(identifier.getRaceGroup().getName());
        String raceColumnName = URLEncoder.encode(identifier.getRaceName());
        String fleetName = URLEncoder.encode(identifier.getFleet().getName());
        
        try {
            new DataLoader<Collection<Mark>>(context, URI.create(AppPreferences.getServerBaseURL(context)
                    + "/sailingserver/rc/marks?leaderboard=" + raceGroupName + "&raceColumn=" + raceColumnName 
                    + "&fleet=" + fleetName), parser, handler)
                    .forceLoad();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadCourse(ManagedRace managedRace, LoadClient<CourseBase> client) {
        SharedDomainFactory domainFactory = DomainFactoryImpl.INSTANCE;
        JsonDeserializer<CourseBase> courseBaseDeserializer = new CourseDataDeserializer(
                new WaypointDeserializer(
                        new ControlPointDeserializer(
                                new MarkDeserializer(domainFactory), 
                                new GateDeserializer(domainFactory, 
                                        new MarkDeserializer(domainFactory)))));
        DataParser<CourseBase> parser = new CourseDataParser(courseBaseDeserializer);
        DataHandler<CourseBase> handler = new CourseDataHandler(this, client, managedRace);
        
        ManagedRaceIdentifier identifier = managedRace.getIdentifier();
        
        String raceGroupName = URLEncoder.encode(identifier.getRaceGroup().getName());
        String raceColumnName = URLEncoder.encode(identifier.getRaceName());
        String fleetName = URLEncoder.encode(identifier.getFleet().getName());
        
        try {
            new DataLoader<CourseBase>(context, URI.create(AppPreferences.getServerBaseURL(context)
                    + "/sailingserver/rc/currentcourse?leaderboard=" + raceGroupName + "&raceColumn=" + raceColumnName 
                    + "&fleet=" + fleetName), parser, handler)
                    .forceLoad();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadCompetitors(ManagedRace managedRace, LoadClient<Collection<Competitor>> client) {
        SharedDomainFactory domainFactory = DomainFactoryImpl.INSTANCE;
        JsonDeserializer<Competitor> competitorDeserializer = new CompetitorDeserializer(domainFactory);
        DataParser<Collection<Competitor>> parser = new CompetitorsDataParser(competitorDeserializer);
        DataHandler<Collection<Competitor>> handler = new CompetitorsDataHandler(this, client, managedRace);
        
        ManagedRaceIdentifier identifier = managedRace.getIdentifier();
        
        String raceGroupName = URLEncoder.encode(identifier.getRaceGroup().getName());
        String raceColumnName = URLEncoder.encode(identifier.getRaceName());
        String fleetName = URLEncoder.encode(identifier.getFleet().getName());
        
        try {
            new DataLoader<Collection<Competitor>>(context, URI.create(AppPreferences.getServerBaseURL(context)
                    + "/sailingserver/rc/competitors?leaderboard=" + raceGroupName + "&raceColumn=" + raceColumnName 
                    + "&fleet=" + fleetName), parser, handler)
                    .forceLoad();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
