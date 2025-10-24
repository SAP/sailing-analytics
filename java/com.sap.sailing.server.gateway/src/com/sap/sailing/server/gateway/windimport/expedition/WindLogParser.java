package com.sap.sailing.server.gateway.windimport.expedition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.server.trackfiles.impl.ExpeditionExtendedDataImporterImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class WindLogParser {
    /**
     * Collects partial wind data until everything is there to create a Wind instance.
     * 
     * @author D047974
     *
     */
    private static class WindBuffer {

        private TimePoint timePoint;
        private Position position;
        private SpeedWithBearing trueWindSpeedWithBearing;

        public void updateTime(TimePoint timePoint) {
            if (timePoint != null) {
                this.timePoint = timePoint;
            }
        }

        public void updateWindData(String trueWindSpeedData, String trueWindDirectionData) {
            if (!trueWindSpeedData.trim().isEmpty() && !trueWindDirectionData.trim().isEmpty()) {
                double trueWindSpeed = Double.parseDouble(trueWindSpeedData);
                double trueWindDirection = Double.parseDouble(trueWindDirectionData);
                Bearing trueWindBearing = new DegreeBearingImpl(trueWindDirection + 180);
                this.trueWindSpeedWithBearing = new KnotSpeedWithBearingImpl(trueWindSpeed, trueWindBearing);
            }
        }

        public void updatePosition(String latData, String lonData) {
            if (!latData.trim().isEmpty() && !lonData.trim().isEmpty()) {
                double lat = Double.parseDouble(latData);
                double lon = Double.parseDouble(lonData);
                this.position = new DegreePosition(lat, lon);
            }
        }

        public Wind createWindIfReady() {
            if ((timePoint != null) && (position != null) && (trueWindSpeedWithBearing != null)) {
                WindImpl result = new WindImpl(position, timePoint, trueWindSpeedWithBearing);
                reset();
                return result;
            } else {
                return null;
            }
        }

        private void reset() {
            timePoint = null;
            position = null;
            trueWindSpeedWithBearing = null;
        }
    }

    private static final String COL_NAME_TRUE_WIND_SPEED = "tws";
    private static final String COL_NAME_TRUE_WIND_SPEED2 = "tw_speed";
    private static final String COL_NAME_TRUE_WIND_DIRECTION = "twd";
    private static final String COL_NAME_TRUE_WIND_DIRECTION2 = "tw_dirn";
    private static final String COL_NAME_LAT = ExpeditionExtendedDataImporterImpl.COL_NAME_LAT;
    private static final String COL_NAME_LON = ExpeditionExtendedDataImporterImpl.COL_NAME_LON;
    
    /**
     * Reads an Expedition log file passed as the {@code windStream} input stream.
     */
    public static Iterable<Wind> importWind(InputStream windStream) throws IOException {
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(windStream));
        String headerLine = csvReader.readLine();
        final ExpeditionExtendedDataImporterImpl importer = new ExpeditionExtendedDataImporterImpl();
        final Map<String, Integer> headers = importer.parseHeader(headerLine);
        List<Wind> result = new ArrayList<Wind>();
        WindBuffer windBuffer = new WindBuffer();
        final AtomicInteger lineNr = new AtomicInteger(1);
        csvReader.lines().forEach(line->{
            if (!line.trim().isEmpty()) {
                importer.parseLine(lineNr.incrementAndGet(), "Expedition Wind Import",
                        line, headers, (timePoint, columnValues, headerDefinitions)->{
                            windBuffer.updateTime(timePoint);
                            final String trueWindSpeedData = headerDefinitions.containsKey(COL_NAME_TRUE_WIND_SPEED) &&
                                    columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_SPEED)] != null ? columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_SPEED)]
                                            : headerDefinitions.containsKey(COL_NAME_TRUE_WIND_SPEED2) ? columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_SPEED2)]
                                                    : null;
                            final String trueWindDirectionData = headerDefinitions.containsKey(COL_NAME_TRUE_WIND_DIRECTION) &&
                                    columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_DIRECTION)] != null ? columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_DIRECTION)]
                                            : headerDefinitions.containsKey(COL_NAME_TRUE_WIND_DIRECTION2) ? columnValues[headerDefinitions.get(COL_NAME_TRUE_WIND_DIRECTION2)]
                                                    : null;
                            windBuffer.updateWindData(trueWindSpeedData, trueWindDirectionData);
                            windBuffer.updatePosition(columnValues[headerDefinitions.get(COL_NAME_LAT)],
                                    columnValues[headerDefinitions.get(COL_NAME_LON)]);
                            Wind wind = windBuffer.createWindIfReady();
                            if (wind != null) {
                                result.add(wind);
                            }
                        });
            }
        });
        return result;
    }
}
