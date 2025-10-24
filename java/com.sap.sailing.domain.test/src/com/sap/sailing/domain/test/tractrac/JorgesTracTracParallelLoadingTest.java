package com.sap.sailing.domain.test.tractrac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Duration;
import com.tractrac.model.lib.api.ModelLocator;
import com.tractrac.model.lib.api.data.IControlPassing;
import com.tractrac.model.lib.api.data.IControlPassings;
import com.tractrac.model.lib.api.data.IPosition;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.SubscriptionLocator;
import com.tractrac.subscription.lib.api.competitor.IPositionListener;
import com.tractrac.subscription.lib.api.control.IControlPassingsListener;
import com.tractrac.subscription.lib.api.event.IConnectionStatusListener;
import com.tractrac.subscription.lib.api.event.ILiveDataEvent;
import com.tractrac.subscription.lib.api.event.IStoredDataEvent;
import com.tractrac.subscription.lib.api.map.IPositionedItemPositionListener;
import com.tractrac.util.lib.api.autolog.LoggerLocator;

/**
 * Created by jorge on 15/02/17.
 */
public class JorgesTracTracParallelLoadingTest {
    private CountDownLatch lock;
    private ConcurrentHashMap<String, StringBuilder> outputs;

    private void start(String params) throws CreateModelException, URISyntaxException, InterruptedException, IOException {
        // Initialize the log
        LoggerLocator.getLoggerManager().init(3, "println");

        // Create a folder with the current date per execution: it will be used to compare results
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Get the events URI and create the event
        URI eventURI = new URI(params);
        final List<String> paramsURIs = this.readParameterFiles(eventURI);
        // Create a list with the threads and create the threads
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < paramsURIs.size(); i++) {
            final int index = i;
            threads.add(new Thread(()->{
                    try {
                        // Download the parameters file and load the race
                        IRace race = ModelLocator.getEventFactory().createRace(new URI(paramsURIs.get(index)), (int) /* timeout in milliseconds */ Duration.ONE_MINUTE.asMillis());

                        // Create the subscriptions
                        SubscriptionLogger subscriptionLogger = new SubscriptionLogger(race);
                        IRaceSubscriber raceSubscriber = SubscriptionLocator.getSusbcriberFactory().createRaceSubscriber(race);
                        raceSubscriber.subscribePositions(subscriptionLogger);
                        raceSubscriber.subscribePositionedItemPositions(subscriptionLogger);
                        raceSubscriber.subscribeControlPassings(subscriptionLogger);
                        raceSubscriber.subscribeConnectionStatus(subscriptionLogger);
                        raceSubscriber.start();
                    } catch (Exception e) {
                       e.printStackTrace();
                    }
                }
            ));
        }

        lock = new CountDownLatch(threads.size());

        // Start all the threads
        for (Thread thread: threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * Reads the event JSON and creates a list with the parameters file.
     * It doesn't use any external library to parse the JSON: we don't want
     * to add external dependencies in order to make easier the execution of
     * this program
     *
     * @param eventURI the event URI
     * @return a list with the parameters files
     * @throws MalformedURLException
     */
    private List<String> readParameterFiles(URI eventURI) throws IOException {
        List<String> paramFiles = new ArrayList<String>();

        InputStream is = eventURI.toURL().openStream();
        StringBuffer jsonContent = new StringBuffer();
        String line = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));) {
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }
        StringTokenizer it = new StringTokenizer(jsonContent.toString(), ",");
        while (it.hasMoreElements()) {
            String item = it.nextElement().toString();
            if (item.startsWith("\"params_url")) {
                String paramsURL = item.split(":", 2)[1];
                paramsURL = paramsURL.replace("\\/", "/");
                paramFiles.add(paramsURL.substring(1, paramsURL.length()-1));
            }
        }
        return paramFiles;
    }
    
    private StringBuilder getOutput(String filename) {
        StringBuilder result = outputs.get(filename);
        if (result == null) {
            result = new StringBuilder();
            outputs.put(filename, result);
        }
        return result;
    }

    /**
     * Created by jorge on 15/02/17.
     */
    public class SubscriptionLogger implements IPositionedItemPositionListener, IControlPassingsListener, IPositionListener,
            IConnectionStatusListener {
        private final IRace race;
        
        public SubscriptionLogger(IRace race) {
            this.race = race;
        }

        @Override
        public void gotControlPassings(long timestamp, IRaceCompetitor raceCompetitor, IControlPassings controlPassings) {
            String outputFile = race.getName() + "-Competitor-"
                    + raceCompetitor.getCompetitor().getName() + "-MarkPassings.txt";
            StringBuilder out = getOutput(outputFile);
            out.append("Mark Passings:" + "\n");
            if (controlPassings.getPassings().size() > 0) {
                IControlPassing firstPassing = controlPassings.getPassings().get(0);
                long firstTimeStamp = firstPassing.getTimestamp();
                out.append(" - " + firstPassing.getControl().getName());
                out.append(": 00:00:00\n");
                for (int i = 1; i < controlPassings.getPassings().size(); i++) {
                    IControlPassing passing = controlPassings.getPassings().get(i);
                    out.append(" - " + passing.getControl().getName());
                    long time = passing.getTimestamp() - firstTimeStamp;
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append(" => +");
                    long hours = TimeUnit.MILLISECONDS.toHours(time);
                    if (hours < 10) {
                        stringBuffer.append("0").append(hours);
                    } else {
                        stringBuffer.append(hours);
                    }
                    stringBuffer.append(":");
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
                    if (minutes < 10) {
                        stringBuffer.append("0").append(minutes);
                    } else {
                        stringBuffer.append(minutes);
                    }
                    stringBuffer.append(":");
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
                    if (seconds < 10) {
                        stringBuffer.append("0").append(seconds);
                    } else {
                        stringBuffer.append(seconds);
                    }
                    out.append(stringBuffer.toString() + "\n");
                }
            }
        }

        @Override
        public void gotPositionedItemPosition(IPositionedItem control, IPosition position) {
            String outputFile = race.getName() + "-Control-" + control.getName() + "-Positions.txt";
            String date = String.valueOf(position.getTimestamp());

            write(outputFile, control.getId() + ", " + date + ", " + position.getLongitude() + ", " + position.getLatitude()
                    + "\n");
        }

        @Override
        public void gotPosition(IRaceCompetitor raceCompetitor, IPosition position) {
            String outputFile = race.getName() + "-Competitor-" + raceCompetitor.getCompetitor().getName() + "-Positions.txt";
            String date = String.valueOf(position.getTimestamp());
            write(outputFile, raceCompetitor.getCompetitor().getId() + ", " + date + ", " + position.getLongitude() + ", "
                    + position.getLatitude() + "\n");
        }

        private void write(String file, String line) {
            StringBuilder out = getOutput(file);
            out.append(line);
        }

        @Override
        public void gotStoredDataEvent(IStoredDataEvent storedDataEvent) {

        }

        @Override
        public void gotLiveDataEvent(ILiveDataEvent liveDataEvent) {

        }

        @Override
        public void stopped(Object subscribedObject) {
            lock.countDown();
        }
    }

    @Test
    public void logParallelLoading() throws CreateModelException, URISyntaxException, InterruptedException, IOException {
        outputs = new ConcurrentHashMap<>();
        final String jsonUrl = "http://event2.tractrac.com/events/event_20160622_ESSCardiff/jsonservice.php";
        start(jsonUrl);
        lock.await(60, TimeUnit.SECONDS);
        Map<String, StringBuilder> firstRunsOutput = outputs;
        outputs = new ConcurrentHashMap<>();
        start(jsonUrl);
        lock.await(60, TimeUnit.SECONDS);
        Map<String, StringBuilder> secondRunsOutput = outputs;
        outputs = new ConcurrentHashMap<>();
        final Set<String> diff1 = new HashSet<>(firstRunsOutput.keySet());
        final Set<String> diff2 = new HashSet<>(secondRunsOutput.keySet());
        if (!diff1.equals(diff2)) {
            diff1.removeAll(secondRunsOutput.keySet());
            diff2.removeAll(firstRunsOutput.keySet());
            Thread.sleep(100);
            fail("Key set differs: a-b="+diff1+", b-a="+diff2+"; in the meantime the following keys were updated: "+outputs.keySet());
        }
        for (String key : firstRunsOutput.keySet()) {
            assertEquals(firstRunsOutput.get(key).toString(), secondRunsOutput.get(key).toString(), "values for key "+key+" differ");
        }
    }
}
