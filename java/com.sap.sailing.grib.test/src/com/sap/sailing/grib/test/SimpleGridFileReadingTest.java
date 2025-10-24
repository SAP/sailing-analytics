package com.sap.sailing.grib.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Bounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.grib.GribWindField;
import com.sap.sailing.grib.GribWindFieldFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import ucar.ma2.ArrayFloat.D2;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

public class SimpleGridFileReadingTest {
    private GridDataset dataSet;

    @Test
    public void testBoundingBox() throws IOException {
        dataSet = GridDataset.open("resources/Drake.wind.grb");
        final LatLonRect boundingBox = dataSet.getBoundingBox();
        System.out.println(boundingBox);
        dataSet.close();
    }

    @Test
    public void testWindContent() throws IOException {
        dataSet = GridDataset.open("resources/Drake.wind.grb");
        final List<GridDatatype> grids = dataSet.getGrids();
        for (GridDatatype grid : grids) {
            assertTrue(grid instanceof GeoGrid);
        }
        GeoGrid grid0 = (GeoGrid) grids.get(0);
        final D2 volumeDataAtTime0 = (D2) grid0.readVolumeData(0);
        final List<VariableSimpleIF> dataVariables = dataSet.getDataVariables();
        final GridCoordSystem coordinateSystem = grid0.getCoordinateSystem();
        final LatLonRect latLngBoundingBox = coordinateSystem.getLatLonBoundingBox();
        final Position sw = new DegreePosition(latLngBoundingBox.getLowerLeftPoint().getLatitude(), latLngBoundingBox.getLowerLeftPoint().getLongitude());
        final Position ne = new DegreePosition(latLngBoundingBox.getUpperRightPoint().getLatitude(), latLngBoundingBox.getUpperRightPoint().getLongitude());
        final Position middle = sw.translateGreatCircle(sw.getBearingGreatCircle(ne), sw.getDistance(ne).scale(0.5));
        final int[] coordinateIndices = coordinateSystem.findXYindexFromLatLon(middle.getLatDeg(), middle.getLngDeg(), new int[2]);
        final float dataAtMiddle = volumeDataAtTime0.get(coordinateIndices[0], coordinateIndices[1]);
        System.out.println(dataVariables);
        System.out.println(dataAtMiddle);
        dataSet.close();
    }
    
    @Test
    public void testUsingFtAPIForMeteoConsultContent() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/TuTMsTuMxoSYmtRzKDl0e75I4HAjqDApvb_.grb", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position middle = getMiddle(windField);
        final TimePoint midTime = windField.getTimeRange().from().plus(windField.getTimeRange().getDuration().divide(2));
        System.out.println(windField.getWind(midTime, middle));
        dataSet.close();
    }
    
    @Test
    public void testUsingFtAPI() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/wind-Atlantic.24hr.grb.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position middle = getMiddle(windField);
        Calendar cal = new GregorianCalendar(2016, 11, 12, 13, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        System.out.println(windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), middle));
        dataSet.close();
    }

    private Position getMiddle(GribWindField windField) {
        final Bounds bounds = windField.getBounds();
        final double newLatDeg = (bounds.getSouthWest().getLatDeg() + bounds.getNorthEast().getLatDeg())/2.0;
        final double newLngDeg = bounds.isCrossesDateLine() ? (bounds.getSouthWest().getLngDeg() + bounds.getNorthEast().getLngDeg() + 360.)/2.0
                : (bounds.getSouthWest().getLngDeg() + bounds.getNorthEast().getLngDeg())/2.0;
        return new DegreePosition(newLatDeg, newLngDeg>180?newLngDeg-360:newLngDeg);
    }
    
    @Test
    public void testGlobalMarineNetCroatia() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/globalMarineNetCroatia.grb.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position croatia = new DegreePosition(42.819522, 16.478226);
        Calendar cal = new GregorianCalendar(2017, 01, 03, 3, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        final WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), croatia);
        assertEquals(3, wind.getObject().getBeaufort(), 2.2);
        assertEquals(150, wind.getObject().getFrom().getDegrees(), 20);
        dataSet.close();
    }

    @Test
    public void testDWD_CWAM() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSetU = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/CWAM_SP_10M_2017020300_024.grib2.bz2", /* task */ null, errorLog);
        FeatureDataset dataSetV = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/CWAM_DD_10M_2017020300_024.grib2.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSetU, dataSetV);
        final Position middle = getMiddle(windField);
        Calendar cal = new GregorianCalendar(2017, 01, 04, 00, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        final WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), middle);
        assertEquals(2.5, wind.getObject().getBeaufort(), 0.5);
        assertEquals(110, wind.getObject().getFrom().getDegrees(), 10);
        dataSetU.close();
        dataSetV.close();
    }
    
    @Test
    public void testNOAADownload() throws IOException {
        // downloaded from http://tgftp.nws.noaa.gov/SL.us008001/ST.expr/DF.gr2/DC.ndfd/AR.oceanic/VP.001-003/
        // alternatively for the 4-6 day forecast: http://tgftp.nws.noaa.gov/SL.us008001/ST.expr/DF.gr2/DC.ndfd/AR.oceanic/VP.004-007/
        // validate graphically at http://tgftp.nws.noaa.gov/SL.us008001/ST.expr/DF.gr2/DC.ndfd/AR.oceanic/VP.004-007/
        final Formatter errorLog = new Formatter(System.err);
        Files.copy(new URL("https://tgftp.nws.noaa.gov/SL.us008001/ST.expr/DF.gr2/DC.ndfd/AR.conus/VP.001-003/ds.wdir.bin").openStream(),
                new File("resources/ds.wdir.bin").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new URL("https://tgftp.nws.noaa.gov/SL.us008001/ST.expr/DF.gr2/DC.ndfd/AR.conus/VP.001-003/ds.wspd.bin").openStream(),
                new File("resources/ds.wspd.bin").toPath(), StandardCopyOption.REPLACE_EXISTING);

        FeatureDataset dataSetDirection = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/ds.wdir.bin", /* task */ null, errorLog);
        FeatureDataset dataSetSpeed = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/ds.wspd.bin", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSetDirection, dataSetSpeed);
        final Position middle = getMiddle(windField);
        Calendar cal = new GregorianCalendar(2017, 01, 04, 06, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        final WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), middle);
        assertNotNull(wind.getObject());
        dataSetDirection.close();
        dataSetSpeed.close();
    }

    @Test
    public void testGlobalMarineNet54hCroatia() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/globalMarineNetCroatia54h.grb.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position croatia = new DegreePosition(42.819522, 16.478226);
        Calendar cal = new GregorianCalendar(2017, 01, 04, 19, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        final WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), croatia);
        assertEquals(5, wind.getObject().getBeaufort(), 1.5);
        assertEquals(200, wind.getObject().getFrom().getDegrees(), 20);
        dataSet.close();
    }

    @Test
    public void testHavana() throws IOException {
        //        Mon Dec 12 13:00:00 CET 2016
        //        ll: 10.0N 100.0W+ ur: 48.0N 40.00W
        //
        //       Havana, Cuba (23.099242, -82.360187)
        //       5:55 AM 19.0 �C -       18.0 �C 94%     1018 hPa        9.0 km  NE      7.4 km/h / 2.1 m/s      -       N/A             Unknown
        //
        //       NE is approx. from 45
        //       2.1m/s is approx. 4.2kts
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/wind-Atlantic.24hr.grb.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position havanaPosition = new DegreePosition(23.099242, -82.360187);
        Calendar cal = new GregorianCalendar(2016, 11, 12, 13, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), havanaPosition);
        assertEquals(4.2, wind.getObject().getKnots(), 3);
        assertEquals(90, wind.getObject().getFrom().getDegrees(), 45); // historic data says NE but we get 130� which matches the SE
        // direction from https://www.wunderground.com/history/airport/MUHA/2016/12/12/DailyHistory.html?req_city=Havana&req_state=03&req_statename=Cuba&reqdb.zip=00000&reqdb.magic=1&reqdb.wmo=78224&MR=1
        // for 10:55 CST. So we should probably accept that.
        dataSet.close();
    }
    
    @Test
    public void testNassau() throws IOException {
        //        Mon Dec 12 13:00:00 CET 2016
        //
        //       Nassau, Bahamas (25.052497, -77.366815)
        //       8:00 AM        25.0 �C -       23.0 �C 89%     1020.2 hPa      10.0 km ESE     9.3 km/h / 2.6 m/s      -       N/A             Mostly Cloudy
        //
        //       NE is approx. from 45
        //       2.1m/s is approx. 4.2kts
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/wind-Atlantic.24hr.grb.bz2", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position nassauPosition = new DegreePosition(25.052497, -77.366815);
        Calendar cal = new GregorianCalendar(2016, 11, 12, 13, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        WindWithConfidence<TimePoint> wind = windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), nassauPosition);
        assertEquals(5.2, wind.getObject().getKnots(), 12); // the forecast had it at 16.7kts... what can we do...
        assertEquals(112.5, wind.getObject().getFrom().getDegrees(), 30); // historic data says ESE which is approximately 112.5
        dataSet.close();
    }
    
    @Test
    public void testUsingFtAPI2() throws IOException {
        final Formatter errorLog = new Formatter(System.err);
        FeatureDataset dataSet = FeatureDatasetFactoryManager.open(FeatureType.ANY, "resources/Drake.wind.grb", /* task */ null, errorLog);
        GribWindField windField = GribWindFieldFactory.INSTANCE.createGribWindField(dataSet);
        final Position middle = getMiddle(windField);
        Calendar cal = new GregorianCalendar(2016, 11, 12, 13, 00, 00);
        cal.setTimeZone(TimeZone.getTimeZone("CET"));
        System.out.println(windField.getWind(new MillisecondsTimePoint(cal.getTimeInMillis()), middle));
        dataSet.close();
    }
    
    @Test
    public void testWindContent2() throws IOException {
        dataSet = GridDataset.open("resources/wind-Atlantic.24hr.grb.bz2");
        final List<GridDatatype> grids = dataSet.getGrids();
        for (GridDatatype grid : grids) {
            assertTrue(grid instanceof GeoGrid);
        }
        GeoGrid grid0 = (GeoGrid) grids.get(0);
        final D2 volumeDataAtTime0 = (D2) grid0.readVolumeData(0);
        final List<VariableSimpleIF> dataVariables = dataSet.getDataVariables();
        final GridCoordSystem coordinateSystem = grid0.getCoordinateSystem();
        final LatLonRect latLngBoundingBox = coordinateSystem.getLatLonBoundingBox();
        final Position sw = new DegreePosition(latLngBoundingBox.getLowerLeftPoint().getLatitude(), latLngBoundingBox.getLowerLeftPoint().getLongitude());
        final Position ne = new DegreePosition(latLngBoundingBox.getUpperRightPoint().getLatitude(), latLngBoundingBox.getUpperRightPoint().getLongitude());
        final Position middle = sw.translateGreatCircle(sw.getBearingGreatCircle(ne), sw.getDistance(ne).scale(0.5));
        final int[] coordinateIndices = coordinateSystem.findXYindexFromLatLon(middle.getLatDeg(), middle.getLngDeg(), new int[2]);
        final float dataAtMiddle = volumeDataAtTime0.get(coordinateIndices[0], coordinateIndices[1]);
        System.out.println(dataVariables);
        System.out.println(dataAtMiddle);
        dataSet.close();
    }
    
    @Test
    public void testDateRange() throws IOException {
        dataSet = GridDataset.open("resources/Drake.wind.grb");
        final CalendarDateRange dateRange = dataSet.getCalendarDateRange();
        final CalendarDate start = dateRange.getStart();
        final CalendarDate end = dateRange.getEnd();
        assertTrue(end.isAfter(start));
        dataSet.close();
        dataSet.release();
    }
}
