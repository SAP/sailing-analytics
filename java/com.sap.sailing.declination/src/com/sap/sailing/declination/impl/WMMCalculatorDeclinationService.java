package com.sap.sailing.declination.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.TreeMap;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.declination.impl.Geomagnetism.Result;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class WMMCalculatorDeclinationService implements DeclinationService {
    /**
     * The magnetic models, in ascending order by their {@link Geomagnetism#getIssueTimePoint() issue time point}
     */
    private final TreeMap<TimePoint, Geomagnetism> worldMagneticModelsByIssueTimePoint;
    
    public WMMCalculatorDeclinationService() {
        final String[] modelNames = new String[] { "/WMM2010.COF", "/WMM2015.COF", "/WMM2020.COF", "/WMM2025.COF" };
        worldMagneticModelsByIssueTimePoint = new TreeMap<>();
        try {
            for (final String modelFileName : modelNames) {
                final Geomagnetism model = new Geomagnetism(new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(modelFileName))));
                worldMagneticModelsByIssueTimePoint.put(model.getStartOfValidity(), model);
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Declination getDeclination(TimePoint timePoint, Position position, long timeoutForOnlineFetchInMilliseconds) throws IOException, ParseException {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timePoint.asMillis());
        final Result wmmResult = worldMagneticModelsByIssueTimePoint.floorEntry(timePoint).getValue().calculate(position.getLngDeg(), position.getLatDeg(), /* altitude */ 0, calendar);
        return new DeclinationRecordForExactTimePoint(position, timePoint, new DegreeBearingImpl(wmmResult.getDeclination()));
    }

    @Override
    public Declination getDeclination(TimePoint timePoint, Position position, Distance maxDistance,
            long timeoutForOnlineFetchInMilliseconds) throws IOException, ParseException {
        return getDeclination(timePoint, position, timeoutForOnlineFetchInMilliseconds);
    }
}
