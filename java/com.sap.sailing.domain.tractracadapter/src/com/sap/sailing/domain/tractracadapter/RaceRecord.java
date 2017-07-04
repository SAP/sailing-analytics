package com.sap.sailing.domain.tractracadapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sse.InvalidDateException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.DateParser;

public class RaceRecord {
    private static final Logger logger = Logger.getLogger(RaceRecord.class.getName());
    
    private static final String LIVE_URI_PROPERTY = "live-uri";
    private static final String STORED_URI_PROPERTY = "stored-uri";
    
    private final String regattaName;
    private final String name;
    private final String replayURL;
    private final String ID;
    private final URL paramURL;
    private final TimePoint trackingstarttime;
    private final TimePoint trackingendtime;
    private final TimePoint racestarttime;
    private final URI liveURI;
    private final URI storedURI;
    private final List<String> boatClassNames;
    private final String raceStatus;
    private final String raceVisibility;
    private final boolean hasReplay;
    private final URL jsonUrl;
    
    public RaceRecord(URL jsonURL, String regattaName, String name, String replayURL, String paramURLAsString,
            String ID, String trackingstarttime, String trackingendtime, String racestarttime,
            String commaSeparatedBoatClassNames, String status, String visibility, boolean hasReplay, boolean loadLiveAndStoredURI)
            throws URISyntaxException, IOException {
        super();
        this.regattaName = regattaName;
        this.name = name;
        this.replayURL = replayURL;
        this.raceStatus = status;
        this.raceVisibility = visibility;
        this.hasReplay = hasReplay;
        this.ID = ID;
        this.jsonUrl = jsonURL;
        this.boatClassNames = new ArrayList<String>();
        if (commaSeparatedBoatClassNames != null) {
            for (String boatClassName : commaSeparatedBoatClassNames.split(",")) {
                this.boatClassNames.add(boatClassName.trim());
            }
        }
        TimePoint tp = null;
        if (trackingstarttime != null) {
            try {
                tp = new MillisecondsTimePoint(DateParser.parseUTC(trackingstarttime).getTime());
            } catch (InvalidDateException e) {
                logger.warning("Unable to parse trackingstarttime of race "+name+": "+trackingstarttime+". Leaving null.");
            }
        }
        this.trackingstarttime = tp;
        tp = null;
        if (trackingendtime != null) {
            try {
                tp = new MillisecondsTimePoint(DateParser.parseUTC(trackingendtime).getTime());
            } catch (InvalidDateException e) {
                logger.warning("Unable to parse trackingendtime of race "+name+": "+trackingendtime+". Leaving null.");
            }
        }
        this.trackingendtime = tp;
        tp = null;
        if (racestarttime != null) {
            try {
                tp = new MillisecondsTimePoint(DateParser.parseUTC(racestarttime).getTime());
            } catch (InvalidDateException e) {
                logger.warning("Unable to parse racestarttime of race "+name+": "+racestarttime+". Leaving null.");
            }
        }
        this.racestarttime = tp;
        
        String jsonURLAsString = jsonURL.toString();
        int indexOfLastSlash = jsonURLAsString.lastIndexOf('/');
        int indexOfLastButOneSlash = jsonURLAsString.lastIndexOf('/', indexOfLastSlash-1);
        String technicalEventName = jsonURLAsString.substring(indexOfLastButOneSlash+1, indexOfLastSlash);
        final String baseURL = jsonURLAsString.substring(0, indexOfLastSlash);
        
        try {
            if (paramURLAsString == null || paramURLAsString.isEmpty()) {
                // for backward compatibility (the param_url field was not always in the JSON document) and perhaps for live mode
                paramURL = new URL(baseURL + "/clientparams.php?event="
                        + technicalEventName + "&race=" + ID);
            } else {
                paramURL = new URL(paramURLAsString);
            }
        } catch (Exception e) {
            logger.info("Couldn't parse TracTrac paramURL " + paramURLAsString + " for race record " + getName());
            logger.log(Level.INFO, "The exception was:", e);
            throw e;
        }
        if (loadLiveAndStoredURI) {
            Map<String, String> paramURLContents = parseParams(paramURL);
            String liveURIAsString = paramURLContents.get(LIVE_URI_PROPERTY);
            liveURI = liveURIAsString == null ? null : new URI(liveURIAsString);
            String storedURIAsString = paramURLContents.get(STORED_URI_PROPERTY);
            if (storedURIAsString == null || storedURIAsString.startsWith("tcp:") || storedURIAsString.startsWith("http:") ||
                    storedURIAsString.startsWith("https:")) {
                storedURI = storedURIAsString == null ? null : new URI(storedURIAsString);
            } else {
                storedURI = new URI(baseURL+"/"+storedURIAsString);
            }
        } else {
            paramURLAsString = null;
            liveURI = null;
            storedURI = null;
        }
    }

    private Map<String, String> parseParams(URL paramURL) throws IOException {
        Map<String, String> result = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("^([^:]*):(.*)$");
        BufferedReader r = new BufferedReader(new InputStreamReader(paramURL.openStream()));
        String line;
        while ((line = r.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                result.put(matcher.group(1), matcher.group(2));
            }
        }
        return result;
    }
    
    public boolean hasLiveAndStoredURI() {
        return liveURI != null;
    }
    
    public boolean hasReplay() {
        return hasReplay;
    }

    public String getName() {
        return name;
    }
    
    public String getEventName() {
        return regattaName;
    }

    public String getReplayURL() {
        return replayURL;
    }
    
    public URI getLiveURI() {
        return liveURI;
    }

    public URI getStoredURI() {
        return storedURI;
    }

    public String getID() {
        return ID;
    }

    public URL getParamURL() {
        return paramURL;
    }
    
    public Iterable<String> getBoatClassNames() {
        return boatClassNames;
    }

    public TimePoint getTrackingStartTime() {
        return trackingstarttime;
    }

    public TimePoint getTrackingEndTime() {
        return trackingendtime;
    }

    public TimePoint getRaceStartTime() {
        return racestarttime;
    }
    
    public String getRaceStatus() {
        return raceStatus;
    }
    
    public String getRaceVisibility() {
        return raceVisibility;
    }
    
    public URL getJsonURL() {
        return jsonUrl;
    }
    
}

