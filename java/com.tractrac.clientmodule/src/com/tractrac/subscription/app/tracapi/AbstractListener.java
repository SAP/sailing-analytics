package com.tractrac.subscription.app.tracapi;

import com.tractrac.model.lib.api.data.*;
import com.tractrac.model.lib.api.event.*;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.model.lib.api.route.IPathRoute;
import com.tractrac.model.lib.api.sensor.ISensorData;
import com.tractrac.subscription.lib.api.competitor.*;
import com.tractrac.subscription.lib.api.control.IControlPassingsListener;
import com.tractrac.subscription.lib.api.map.IPositionedItemPositionListener;
import com.tractrac.subscription.lib.api.control.IControlRouteChangeListener;
import com.tractrac.subscription.lib.api.map.IMapItemsListener;
import com.tractrac.subscription.lib.api.event.IConnectionStatusListener;
import com.tractrac.subscription.lib.api.event.IEventMessageListener;
import com.tractrac.subscription.lib.api.event.ILiveDataEvent;
import com.tractrac.subscription.lib.api.event.IStoredDataEvent;
import com.tractrac.subscription.lib.api.race.IRaceCompetitorListener;
import com.tractrac.subscription.lib.api.race.IRaceMessageListener;
import com.tractrac.subscription.lib.api.race.IRaceStartStopTimesChangeListener;
import com.tractrac.subscription.lib.api.race.IRacesListener;

import java.net.URI;
import java.util.UUID;

/**
 * @author <a href="mailto:jorge@tractrac.dk">Jorge Piera Llodr&aacute;</a>
 */
public abstract class AbstractListener implements IEventMessageListener,
        IRaceMessageListener, IPositionListener, IPositionOffsetListener,
        IPositionSnappedListener, IConnectionStatusListener, IPositionedItemPositionListener,
        IControlPassingsListener, IRaceStartStopTimesChangeListener,
        IControlRouteChangeListener, ICompetitorSensorDataListener,
        IRacesListener, ICompetitorsListener, IMapItemsListener, IRaceCompetitorListener {

    @Override
    public void gotStoredDataEvent(IStoredDataEvent storedDataEvent) {

    }

    @Override
    public void gotLiveDataEvent(ILiveDataEvent liveDataEvent) {

    }

    @Override
    public void gotRouteChange(IControlRoute controlRoute, long timeStamp) {

    }

    @Override
    public void gotRouteChange(IPathRoute controlRoute, long timeStamp) {

    }

    @Override
    public void gotControlPassings(long timestamp, IRaceCompetitor raceCompetitor,
                                   IControlPassings markPassings) {

    }

    @Override
    public void gotPositionedItemPosition(IPositionedItem control, IPosition position) {

    }

    @Override
    public void gotPositionSnapped(IRaceCompetitor raceCompetitor,
                                   IPositionSnapped positionSnapped) {

    }

    @Override
    public void gotPositionOffset(IRaceCompetitor raceCompetitor,
                                  IPositionOffset position) {

    }

    @Override
    public void gotPosition(IRaceCompetitor raceCompetitor, IPosition position) {

    }

    @Override
    public void gotRaceStartStopTime(IRace race, IStartStopData startStopData) {

    }

    @Override
    public void gotTrackingStartStopTime(IRace race, IStartStopData startStopData) {

    }

    @Override
    public void gotRaceMessage(IRace race, IMessageData messageData) {

    }

    @Override
    public void stopped(Object object) {

    }

    @Override
    public void gotEventMessage(IEvent event, IMessageData messageData) {

    }

    @Override
    public void gotSensorData(IRaceCompetitor raceCompetitor, ISensorData sensorData) {

    }

    @Override
    public void updateCompetitor(long timestamp, ICompetitor competitor) {

    }

    @Override
    public void addCompetitor(long timestamp, ICompetitor competitor) {

    }

    @Override
    public void deleteCompetitor(long timestamp, UUID competitorId) {

    }

    @Override
    public void updateMapItem(long timestamp, IMapItem mapItem) {

    }

    @Override
    public void addMapItem(long timestamp, IMapItem mapItem) {

    }

    @Override
    public void deleteMapItem(long timestamp, UUID mapItemId) {

    }

    @Override
    public void updateRace(long timestamp, IRace race) {

    }

    @Override
    public void addRace(long timestamp, IRace race) {

    }

    @Override
    public void deleteRace(long timestamp, UUID raceId) {

    }

    @Override
    public void reloadRace(long timestamp, UUID raceId) {

    }

    @Override
    public void abandonRace(long timestamp, UUID raceId) {

    }

    @Override
    public void startTracking(long timestamp, UUID raceId) {

    }

    @Override
    public void dataSourceChanged(long timestamp, IRace race, DataSource oldDataSource, URI oldLiveURI, URI oldStoredURI) {

    }

    @Override
    public void addRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {

    }

    @Override
    public void updateRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {

    }

    @Override
    public void deleteRaceCompetitor(long timestamp, UUID competitorId) {

    }

    @Override
    public void removeOffsetPositions(long timestamp, UUID competitorId, int offset) {

    }
}
