/*
 * EventListener.java
 *
 * Created Oct 18, 2012 7:10:44 AM
 *
 * $Id$
 */
package com.tractrac.subscription.app.tracapi;

import com.tractrac.model.lib.api.data.*;
import com.tractrac.model.lib.api.event.*;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.model.lib.api.sensor.ISensorData;
import com.tractrac.subscription.lib.api.event.ILiveDataEvent;
import com.tractrac.subscription.lib.api.event.IStoredDataEvent;
import com.tractrac.util.lib.api.TimeUtils;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:jorge@tractrac.dk">Jorge Piera Llodr&aacute;</a>
 */
public class EventListener extends AbstractListener {

    private IRace race;

    private static void show(Object obj) {
        System.out.println(TimeUtils.formatDateInMillis(new Date().getTime()) + ": " + obj);
    }


    private static final Map<UUID, Integer> controlPos = new HashMap<>();
    private static final Map<UUID, Integer> compPos = new HashMap<>();

    @Override
    public void gotStoredDataEvent(IStoredDataEvent storedDataEvent) {
        show(storedDataEvent);
        if (storedDataEvent.getType() == IStoredDataEvent.Type.End) {
            synchronized (this) {
                System.out.println("CONTROLS: " + controlPos);
                System.out.println("COMPETITORS: " + compPos);
            }
        }
    }

    @Override
    public void gotLiveDataEvent(ILiveDataEvent liveDataEvent) {
        show(liveDataEvent);
    }

    @Override
    public void gotRouteChange(IControlRoute controlRoute, long timeStamp) {
        StringBuilder message = new StringBuilder();
        message.append("New route at ").append(timeStamp).append(": ").append(controlRoute.toString());
        for (int i = 0; i < controlRoute.getControls().size(); i++) {
            message.append("\n\t").append(i).append(": ").append(controlRoute.getControls().get(i).getName());
        }
        message.append("\n\tMETADATA: ").append(controlRoute.getMetadata().getText());
        show(message);
    }

    @Override
    public void gotControlPassings(long timestamp, IRaceCompetitor raceCompetitor,
                                   IControlPassings markPassings) {
        show("New markpassings " + markPassings + " for the competitor " + raceCompetitor.getCompetitor().toString() + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void gotPositionedItemPosition(IPositionedItem positionedItem, IPosition position) {
        if (this.race != null) {
            String markCourseArea = positionedItem.getCourseArea();
            String raceCourseArea = this.race.getCourseArea();
            if (markCourseArea != null && raceCourseArea != null && !markCourseArea.equals(raceCourseArea)) {
                //show("Discarding position because control course area " + markCourseArea + "(" + control.getName() + ") is not like " + race.getCourseArea());
                return;
            }
        }
        String message = "COTRLPOS " + positionedItem.getName() +
                position.toString();
        int posNumber = increasePos(controlPos, positionedItem.getId());
        message += (", TOTAL POS: " + posNumber);
        show(message);
    }

    private synchronized int increasePos(Map<UUID, Integer> map, UUID id) {
        map.merge(id, 1, Integer::sum);
        return map.get(id);
    }

    @Override
    public void gotPositionSnapped(IRaceCompetitor raceCompetitor,
                                   IPositionSnapped positionSnapped) {
        show("New position " + positionSnapped + " for the competitor " + raceCompetitor.getCompetitor().toString());
    }

    @Override
    public void gotPositionOffset(IRaceCompetitor raceCompetitor,
                                  IPositionOffset position) {
        show("New position " + position + " for the competitor " + raceCompetitor.getCompetitor().toString());
    }

    @Override
    public void gotPosition(IRaceCompetitor raceCompetitor, IPosition position) {
        StringBuilder message = new StringBuilder();
        message.append("POSITION");
        if (raceCompetitor != null) {
            message.append("\t").append(raceCompetitor.getCompetitor().getShortName()).append("\t");
        }
        message.append(position.toString());
        int posNumber = increasePos(compPos, raceCompetitor.getCompetitor().getId());
        message.append(", TOTAL POS: ").append(posNumber);
        show(message.toString());
    }

    @Override
    public void gotRaceStartStopTime(IRace race, IStartStopData startStopData) {
        show("RACE START TIME: " + (startStopData.getStartTime() == 0 ? "-" : TimeUtils.formatDate(startStopData.getStartTime())));
    }

    @Override
    public void gotTrackingStartStopTime(IRace race, IStartStopData startStopData) {
        show("TRACKING TIMES: " + TimeUtils.formatDate(startStopData.getStartTime()) + " - " + TimeUtils.formatDate(startStopData.getStopTime()));
    }

    @Override
    public void gotRaceMessage(IRace race, IMessageData messageData) {
        show("New race message " + messageData.toString());
    }

    @Override
    public void stopped(Object object) {
        show("Stopping the connection with " + object.toString());
    }

    @Override
    public void gotEventMessage(IEvent event, IMessageData messageData) {
        show("New event message " + messageData.toString());
    }

    @Override
    public void gotSensorData(IRaceCompetitor raceCompetitor, ISensorData sensorData) {
        StringBuilder message = new StringBuilder();
        message.append("SENSOR");
        if (raceCompetitor != null) {
            message.append("\t").append(raceCompetitor.getCompetitor().getShortName()).append("\t");
        }
        message.append(sensorData.toString());
        show(message.toString());
    }

    @Override
    public void updateCompetitor(long timestamp, ICompetitor competitor) {
        show("UPDATE COMPETITOR " + competitor.toString() + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void addCompetitor(long timestamp, ICompetitor competitor) {
        show("ADD COMPETITOR " + competitor.toString() + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void deleteCompetitor(long timestamp, UUID competitorId) {
        show("DELETE COMPETITOR" + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void updateMapItem(long timestamp, IMapItem mapItem) {
        show("UPDATE CONTROL " + mapItem.toString() + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void addMapItem(long timestamp, IMapItem mapItem) {
        show("ADD CONTROL " + mapItem.toString() + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void deleteMapItem(long timestamp, UUID mapItemId) {
        show("DELETE CONTROL " + mapItemId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void addRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {
        show("ADD RACE COMPETITOR " + raceCompetitor.toString() + " at " + TimeUtils.formatDateInMillis(timestamp) +
                "\n\t ENTRY STATUS: " + raceCompetitor.getStatus() +
                "\n\t ENTRY STATUS TIME: " + TimeUtils.formatDate(raceCompetitor.getStatusTime()) +
                "\n\t UPDATE ENTRY STATUS: " + TimeUtils.formatDateInMillis(raceCompetitor.getStatusLastChangedTime()) +
                "\n\t OFFICIAL RANK: " + (raceCompetitor.getOfficialRank() == 0 ? "-" : raceCompetitor.getOfficialRank()) +
                "\n\t OFFICIAL RANK TIME: " + (raceCompetitor.getOfficialFinishTime() == 0 ? "-" : TimeUtils.formatDateInMillis(raceCompetitor.getOfficialFinishTime()))
        );
    }

    @Override
    public void updateRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {
        show("UPDATE RACE COMPETITOR " + raceCompetitor.toString() + " at " + TimeUtils.formatDateInMillis(timestamp) +
                "\n\t ENTRY STATUS: " + raceCompetitor.getStatus() +
                "\n\t ENTRY STATUS TIME: " + TimeUtils.formatDate(raceCompetitor.getStatusTime()) +
                "\n\t UPDATE ENTRY STATUS: " + TimeUtils.formatDateInMillis(raceCompetitor.getStatusLastChangedTime()) +
                "\n\t OFFICIAL RANK: " + (raceCompetitor.getOfficialRank() == 0 ? "-" : raceCompetitor.getOfficialRank()) +
                "\n\t OFFICIAL RANK TIME: " + (raceCompetitor.getOfficialFinishTime() == 0 ? "-" : TimeUtils.formatDateInMillis(raceCompetitor.getOfficialFinishTime()))
        );
    }

    @Override
    public void deleteRaceCompetitor(long timestamp, UUID competitorId) {
        show("DELETE RACE COMPETITOR " + competitorId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }


    @Override
    public void updateRace(long timestamp, IRace race) {
        show("UPDATE RACE " + race.toString() + " at " + TimeUtils.formatDateInMillis(timestamp) +
                "\n\tTrackingStartTime = " + TimeUtils.formatDate(race.getTrackingStartTime()) +
                "\n\tTrackingEndTime = " + TimeUtils.formatDate(race.getTrackingEndTime()) +
                "\n\tRaceStartTime = " + TimeUtils.formatDate(race.getRaceStartTime()) +
                "\n\tLiveDelay = " + race.getLiveDelay() +
                "\n\tRaceStatus = " + race.getStatus() +
                "\n\tRaceStatusTime = " + TimeUtils.formatDate(race.getStatusTime()) +
                "\n\tRaceStatusUpdatedAt: " + TimeUtils.formatDate(race.getStatusLastChangedTime())
        );
    }

    @Override
    public void addRace(long timestamp, IRace race) {
        show("ADD RACE " + race.toString() + " at " + TimeUtils.formatDateInMillis(timestamp) +
                "\n\tTrackingStartTime = " + TimeUtils.formatDate(race.getTrackingStartTime()) +
                "\n\tTrackingEndTime = " + TimeUtils.formatDate(race.getTrackingEndTime()) +
                "\n\tRaceStartTime = " + TimeUtils.formatDate(race.getRaceStartTime()) +
                "\n\tLiveDelay = " + race.getLiveDelay() +
                "\n\tRaceStatus = " + race.getStatus() +
                "\n\tRaceStatusTime = " + TimeUtils.formatDate(race.getStatusTime()) +
                "\n\tRaceStatusUpdatedAt: " + TimeUtils.formatDate(race.getStatusLastChangedTime())
        );
    }

    @Override
    public void deleteRace(long timestamp, UUID raceId) {
        show("DELETE RACE " + raceId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void reloadRace(long timestamp, UUID raceId) {
        show("RELOAD RACE " + raceId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void abandonRace(long timestamp, UUID raceId) {
        show("ABANDON RACE " + raceId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void startTracking(long timestamp, UUID raceId) {
        show("START TRACKING " + raceId + " at " + TimeUtils.formatDateInMillis(timestamp));
    }

    @Override
    public void dataSourceChanged(long timestamp, IRace race, DataSource oldDataSource, URI oldLiveURI, URI oldStoredURI) {
        show("DATA SOURCE CHANGE " + race + ":\n" +
                "\tLIVE URI: " + race.getLiveURI() + "\n" +
                "\tSTORED URI: " + race.getStoredURI());
    }

    public IRace getRace() {
        return race;
    }

    public void setRace(IRace race) {
        this.race = race;
    }
}
