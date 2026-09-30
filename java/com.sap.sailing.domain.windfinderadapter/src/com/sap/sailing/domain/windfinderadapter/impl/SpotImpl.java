package com.sap.sailing.domain.windfinderadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.domain.windfinder.ReviewedSpotsCollection;
import com.sap.sailing.domain.windfinder.Spot;
import com.sap.sailing.domain.windfinder.WindFinderSpotListener;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.ThreadPoolUtil;

public class SpotImpl extends SpotDTO implements Spot {
    private static final long serialVersionUID = -7793334775486020313L;
    
    private static final Logger logger = Logger.getLogger(SpotImpl.class.getName());

    private final WindFinderReportParser parser;
    private final ReviewedSpotsCollection collection;
    private final ConcurrentMap<WindFinderSpotListener, Boolean> listeners;
    private TimePoint latestMeasurement;
    private static final Duration POLL_EVERY = Duration.ONE_MINUTE;
    private ScheduledFuture<?> poller;
    
    public SpotImpl(String name, String id, String keyword, String englishCountryName, Position position, WindFinderReportParser parser, ReviewedSpotsCollection collection) {
        super(name, id, keyword, englishCountryName, position);
        this.parser = parser;
        this.collection = collection;
        listeners = new ConcurrentHashMap<>();
    }
    
    @Override
    public void addListener(WindFinderSpotListener listener) {
        if (listeners.isEmpty()) {
            ensurePollerRunning();
        }
        listeners.put(listener, true);
    }

    private synchronized void ensurePollerRunning() {
        if (poller == null) {
            logger.info("Starting to poll from WindFinder spot "+getId()+" as the first listener started listening.");
            poller = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().scheduleAtFixedRate(this::poll,
                    /* initialDelay */ 0, /* period */ POLL_EVERY.asMillis(), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Fetches wind measurements from this spot and notifies new ones to the {@link #listeners}.
     */
    private void poll() {
        try {
            final Iterable<Wind> windFixes = getAllMeasurementsAfter(latestMeasurement);
            logger.fine("Received "+Util.size(windFixes)+" wind fixes from WindFinder spot "+getId()+". Notifying "+listeners.size()+" listeners.");
            for (final Wind wind : windFixes) {
                if (latestMeasurement == null || wind.getTimePoint().after(latestMeasurement)) {
                    latestMeasurement = wind.getTimePoint();
                }
            }
            notifyListeners(windFixes);
        } catch (IOException | ParseException | org.json.simple.parser.ParseException e) {
            logger.log(Level.SEVERE, "Error trying to obtain WindFinder measurements from spot "+getId(), e);
        }
    }
    
    private void notifyListeners(Iterable<Wind> windFixes) {
        for (final WindFinderSpotListener listener : listeners.keySet()) {
            listener.windDataReceived(windFixes, this);
        }
    }

    private synchronized void stopPoller() {
        logger.info("Stopping polling from WindFinder spot "+getId()+" as last listener stopped listening.");
        poller.cancel(/* mayInterruptIfRunning */ false);
        poller = null;
    }
    
    @Override
    public void removeListener(WindFinderSpotListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            stopPoller();
        }
    }

    @Override
    public Iterable<Wind> getAllMeasurementsAfter(TimePoint timePoint) throws MalformedURLException, IOException, ParseException, org.json.simple.parser.ParseException {
        final List<Wind> result = new ArrayList<>();
        for (final Wind measurement : getAllMeasurements()) {
            if (timePoint == null || measurement.getTimePoint().after(timePoint)) {
                result.add(measurement);
            }
        }
        return result;
    }
    
    @Override
    public Iterable<Wind> getAllMeasurements()
            throws IOException, MalformedURLException, ParseException, org.json.simple.parser.ParseException {
        final URLConnection connection = HttpUrlConnectionHelper.redirectConnection(getMeasurementsUrl());
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(connection, "UTF-8");
        final InputStream response = (InputStream) connection.getContent();
        final Iterable<Wind> measurements = parser.parse(getPosition(), (JSONArray) new JSONParser().parse(new InputStreamReader(response, charset)));
        return measurements;
    }

    private URL getMeasurementsUrl() throws MalformedURLException {
        return new URL(Activator.BASE_URL_FOR_JSON_DOCUMENTS+"/"+collection.getId()+"_"+getId()+".json");
    }
}
