package com.sap.sailing.domain.swisstimingreplayadapter;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingRaceConfig;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;

public interface SwissTimingReplayService {

    List<SwissTimingReplayRace> parseJSONObject(InputStream inputStream, String swissTimingUrlText) throws IOException,
            ParseException, org.json.simple.parser.ParseException;

    List<SwissTimingReplayRace> listReplayRaces(String swissTimingUrlText);

    SwissTimingRaceConfig loadRaceConfig(InputStream configDataStream) throws IOException,
            org.json.simple.parser.ParseException;

    DateFormat getStartTimeFormat();

    /**
     * @param link
     *            the URL without the implicit "http://" prefix, as obtained, e.g., from
     *            {@link SwissTimingReplayRace#getLink()}.
     * @param replayListener
     *            the listener to receive all persing events
     */
    void loadRaceData(String link, SwissTimingReplayListener replayListener);
    
    /**
     * @param link
     *            the URL without the implicit "http://" prefix, as obtained, e.g., from
     *            {@link SwissTimingReplayRace#getLink()}.
     * @param regatta TODO
     */
    void loadRaceData(String link, DomainFactory domainFactory, Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry);

}
