package com.sap.sailing.server.gateway.expeditionimport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.server.gateway.windimport.expedition.WindLogParser;
import com.sap.sse.common.Util;

public class TestWindLogParser {
    
    private static final double DOUBLE_TOLERANCE = 1e-7;

	@Test
    public void testCompleteRow() throws Exception {
        Iterable<Wind> windImport = WindLogParser.importWind(getClass().getResourceAsStream("CompleteRow.csv"));
        assertThat(Util.size(windImport), is(1));
        Wind wind = windImport.iterator().next();
        assertWind(wind, 11.2, 257.2, 54.430272, 10.172062);
    }

    @Test
    public void testCompleteRowPermutatedColumns() throws Exception {
        Iterable<Wind> windImport = WindLogParser.importWind(getClass().getResourceAsStream("CompleteRowPermutatedColumns.csv"));
        assertThat(Util.size(windImport), is(1));
        Wind wind = windImport.iterator().next();
        assertWind(wind, 11.2, 257.2, 54.430272, 10.172062);
        assertThat(wind.getTimePoint().asMillis(), is(1372231373011L)); 
    }

	@Test
    public void testWindFirst() throws Exception {
        Iterable<Wind> windImport = WindLogParser.importWind(getClass().getResourceAsStream("WindFirst.csv"));
        assertThat(Util.size(windImport), is(1));
        Wind wind = windImport.iterator().next();
        assertWind(wind, 11.2, 257.2, 54.430272, 10.172062);
    }

    @Test
    public void testPositionFirst() throws Exception {
        Iterable<Wind> windImport = WindLogParser.importWind(getClass().getResourceAsStream("PositionFirst.csv"));
        assertThat(Util.size(windImport), is(1));
        Wind wind = windImport.iterator().next();
        assertWind(wind, 11.2, 257.2, 54.430272, 10.172062);
    }

    private void assertWind(Wind wind, double knots, double degrees, double lat, double lon) {
        assertThat(wind.getKnots(), closeTo(knots, DOUBLE_TOLERANCE));
        assertThat(wind.getFrom().getDegrees(), closeTo(degrees, DOUBLE_TOLERANCE));
        assertThat(wind.getPosition().getLatDeg(), closeTo(lat, DOUBLE_TOLERANCE));
        assertThat(wind.getPosition().getLngDeg(), closeTo(lon, DOUBLE_TOLERANCE));
    }
}
