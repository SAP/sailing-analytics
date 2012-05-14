package com.sap.sailing.domain.tractracadapter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.maptrack.client.io.TypeController;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.tractrac.clientmodule.Race;
import com.tractrac.clientmodule.Route;
import com.tractrac.clientmodule.data.ICallbackData;
import com.tractrac.clientmodule.data.RouteData;

import difflib.PatchFailedException;

/**
 * The ordering of the {@link ControlPoint}s of a {@link Course} are received
 * dynamically through a callback interface. Therefore, when connected to an
 * {@link Regatta}, these orders are not yet defined. An instance of this class
 * can be used to create the listeners needed to receive this information and
 * set it on an {@link Regatta}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class RaceCourseReceiver extends AbstractReceiverWithQueue<Route, RouteData, Race>  {
    private final static Logger logger = Logger.getLogger(RaceCourseReceiver.class.getName());
    
    private final long millisecondsOverWhichToAverageWind;
    private final WindStore windStore;
    private final DynamicRaceDefinitionSet raceDefinitionSetToUpdate;
    
    public RaceCourseReceiver(DomainFactory domainFactory, DynamicTrackedRegatta trackedRegatta,
            com.tractrac.clientmodule.Event tractracEvent, WindStore windStore,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate,
            long millisecondsOverWhichToAverageWind) {
        super(domainFactory, tractracEvent, trackedRegatta);
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.windStore = windStore;
        this.raceDefinitionSetToUpdate = raceDefinitionSetToUpdate;
    }

    /**
     * The listeners returned will, when added to a controller, receive events about the
     * course definition of a race. When this happens, a new {@link RaceDefinition} is
     * created with the respective {@link Course} and added to the {@link #event event}.
     */
    @Override
    public Iterable<TypeController> getTypeControllersAndStart() {
        List<TypeController> result = new ArrayList<TypeController>();
        for (final Race race : getTracTracEvent().getRaceList()) {
            TypeController routeListener = RouteData.subscribe(race, new ICallbackData<Route, RouteData>() {
                @Override
                public void gotData(Route route, RouteData record, boolean isLiveData) {
                    enqueue(new Triple<Route, RouteData, Race>(route, record, race));
                }
            });
            setAndStartThread(new Thread(this, getClass().getName()));
            result.add(routeListener);
        }
        return result;
    }
    
    @Override
    protected void handleEvent(Triple<Route, RouteData, Race> event) {
        System.out.print("R");
        Course course = getDomainFactory().createCourse(event.getA().getName(), event.getB().getPoints());
        RaceDefinition existingRaceDefinitionForRace = getDomainFactory().getExistingRaceDefinitionForRace(event.getC());
        if (existingRaceDefinitionForRace != null) {
            logger.log(Level.INFO, "Received course update for existing race "+event.getC().getName());
            // race already exists; this means that we obviously found a course re-definition (yuck...)
            // Therefore, don't create TrackedRace again because it already exists.
            try {
                getDomainFactory().updateCourseWaypoints(existingRaceDefinitionForRace.getCourse(), event.getB().getPoints());
                if (getTrackedEvent().getExistingTrackedRace(existingRaceDefinitionForRace) == null) {
                    createTrackedRace(existingRaceDefinitionForRace);
                }
            } catch (PatchFailedException e) {
                logger.log(Level.SEVERE, "Internal error updating race course "+course+": "+e.getMessage());
                logger.throwing(RaceCourseReceiver.class.getName(), "handleEvent", e);
            }
        } else {
            logger.log(Level.INFO, "Received course for non-existing race "+event.getC().getName()+". Creating RaceDefinition.");
            // create race definition and add to event
            getDomainFactory().getOrCreateRaceDefinitionAndTrackedRace(getTrackedEvent(), event.getC(), course,
                    windStore, millisecondsOverWhichToAverageWind, raceDefinitionSetToUpdate);
        }
    }

    private void createTrackedRace(RaceDefinition race) {
        getTrackedEvent().createTrackedRace(race,
                windStore, millisecondsOverWhichToAverageWind,
                /* time over which to average speed: */ race.getBoatClass().getApproximateManeuverDurationInMilliseconds(),
                raceDefinitionSetToUpdate);
    }

}
