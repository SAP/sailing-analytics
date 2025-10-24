package com.tractrac.subscription.app.tracapi;

import com.tractrac.model.lib.api.ModelLocator;
import com.tractrac.model.lib.api.event.*;
import com.tractrac.subscription.lib.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ConnectToAllRaces {

    public static void main(String[] args) throws CreateModelException, URISyntaxException, SubscriberInitializationException, IOException, RaceLoadingException {

        URI paramURI = new URI("https://event.tractrac.com/events/event_20140429_ESSQingdao/jsonservice.php");
        IEventFactory eventFactory = ModelLocator.getEventFactory();
        IEvent event = eventFactory.createEvent(paramURI);

        List<IRaceSubscriber> raceSubscriberList = new ArrayList<>();

        ISubscriberFactory subscriberFactory = SubscriptionLocator.getSusbcriberFactory();

        EventListener listener = new EventListener();

        IEventSubscriber eventSubscriber = subscriberFactory.createEventSubscriber(
                event
        );
        eventSubscriber.subscribeConnectionStatus(listener);
        eventSubscriber.subscribeEventMessages(listener);
        eventSubscriber.subscribeRaces(listener);
        eventSubscriber.subscribeMapItems(listener);
        eventSubscriber.subscribeCompetitors(listener);

        for (IRace race : event.getRaces()) {
            race.reloadFromServer();
                listener = new EventListener();
                listener.setRace(race);
                IRaceSubscriber raceSubscriber = subscriberFactory.createRaceSubscriber(
                        race
                );
                raceSubscriber.subscribeConnectionStatus(listener);
                raceSubscriber.subscribePositionedItemPositions(listener);
                raceSubscriber.subscribePositions(listener);
                //raceSubscriber.subscribePositionsSnapped(listener);
                raceSubscriber.subscribeControlPassings(listener);
                raceSubscriber.subscribeCompetitorSensorData(listener);
                raceSubscriber.subscribeRaceMessages(listener);
                raceSubscriber.subscribeRaceTimesChanges(listener);
                raceSubscriber.subscribeRouteChanges(listener);
                raceSubscriber.subscribeRaceCompetitor(listener);

                raceSubscriber.start();

                raceSubscriberList.add(raceSubscriber);
        }
        eventSubscriber.start();

        System.out.println("Press key to cancel live data stream");
        System.in.read();
        System.out.println("Cancelling data stream");

        for (IRaceSubscriber raceSubscriber : raceSubscriberList) {
            raceSubscriber.stop();
        }
        eventSubscriber.stop();
    }

}
