package com.tractrac.subscription.app.tracapi;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.tractrac.model.lib.api.ModelLocator;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IEventFactory;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.subscription.lib.api.*;
import com.tractrac.util.lib.api.autolog.LoggerLocator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:jorge@tractrac.dk">Jorge Piera Llodr&aacute;</a>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException,
            SubscriberInitializationException, CreateModelException {

        LoggerLocator.getLoggerManager().init(1, "println");

        Object[] myArgs = parseArguments(args);
        URI paramURI = (URI) myArgs[0];
        boolean measureDelay = (boolean) myArgs[1];

        // Create the event object
        IEventFactory eventFactory = ModelLocator.getEventFactory();
        IRace race = eventFactory.createRace(paramURI);
        IEvent event = race.getEvent();

        event.getPositionedItems().forEach(positionedItem -> System.out.println(positionedItem.getMetadata().getText()));

        // Create the subscriber
        ISubscriberFactory subscriberFactory = SubscriptionLocator.getSusbcriberFactory();
        IEventSubscriber eventSubscriber = subscriberFactory.createEventSubscriber(event);

        AbstractListener listener;
        if (measureDelay) {
            listener = new DelayListener();
        } else {
            listener = new EventListener();
        }

        eventSubscriber.subscribeConnectionStatus(listener);
        eventSubscriber.subscribeEventMessages(listener);
        eventSubscriber.subscribeRaces(listener);
        eventSubscriber.subscribeMapItems(listener);
        eventSubscriber.subscribeCompetitors(listener);

        IRaceSubscriber raceSubscriber = subscriberFactory.createRaceSubscriber(
                race
        );
        raceSubscriber.subscribeConnectionStatus(listener);
        //raceSubscriber.subscribePositionedItemPositions(listener);
        raceSubscriber.subscribePositions(listener);
        //raceSubscriber.subscribePositionsSnapped(listener);
        raceSubscriber.subscribeControlPassings(listener);
        //raceSubscriber.subscribeCompetitorSensorData(listener);
        //raceSubscriber.subscribeRaceMessages(listener);
        raceSubscriber.subscribeRaceTimesChanges(listener);
        raceSubscriber.subscribeRouteChanges(listener);
        raceSubscriber.subscribeRaceCompetitor(listener);

        raceSubscriber.start();
        eventSubscriber.start();


        // Go ahead with GUI or other stuff in main thread
        System.out.println("Press key to cancel live data stream");
        System.in.read();
        System.out.println("Cancelling data stream");

        // Stop data streams
        eventSubscriber.stop();
        raceSubscriber.stop();
    }

    private static Object[] parseArguments(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar TracAPI.jar parametersfile measureDelay");
            System.exit(0);
        }
        Object[] myArgs = new Object[2];
        try {
            myArgs[0] = new URI(args[0]);
            myArgs[1] = args.length >= 2 && args[1].equals("1");
        } catch (URISyntaxException ex) {
            System.out.println("Malformed URL " + ex.getMessage());
            System.exit(0);
        }
        return myArgs;
    }
}
