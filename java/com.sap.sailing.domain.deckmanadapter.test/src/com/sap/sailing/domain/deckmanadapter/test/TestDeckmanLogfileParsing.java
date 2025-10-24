package com.sap.sailing.domain.deckmanadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.deckmanadapter.DeckmanAdapter;
import com.sap.sailing.domain.deckmanadapter.DeckmanAdapterFactory;
import com.sap.sailing.domain.deckmanadapter.Record;
import com.sap.sailing.domain.deckmanadapter.impl.FieldType;
import com.sap.sse.common.TimePoint;

public class TestDeckmanLogfileParsing {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    
    @Test
    public void testSimpleLogFileParsing() throws IOException, ParseException {
        Reader r = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("simpletest.csv"));
        DeckmanAdapter adapter = DeckmanAdapterFactory.INSTANCE.createDeckmanAdapter();
        Iterator<Record> recordIter = adapter.parseLogFile(r);
        // date,race_name,leg_name,leg_score,boat_speed,heel,leeway,rudder,heading,course,depth,tws,twa,twd,aws,awa,latitude,longitude,sog,cog,abstwa,absawa,absheel,absleeway,absrudder,foresail,staysail,mainsail,forestay,rake,vmg,status,starboard_port,rudder2,keelang,absrudder2,abskeelang
        // 2013-10-11 07:16:33 UTC,2013_10_11_aa_Training01,,,12.65,-4.4,-0.5,20.1,305.0,304.5,,13.8,152.0,97.0,6.6,86.0,45.657005,13.70788,12.48,305.0,152.0,86.0,-4.4,-0.5,20.1,A2F,,M offshore D,2.4,,-11.1692870496654,DOWNWIND,starboard,41.9,65.0,41.9,65.0
        Record r1 = recordIter.next();
        TimePoint r1TimePoint = r1.getTimePoint();
        Position r1Position = r1.getPosition();
        GPSFixMoving r1Fix = r1.getGpsFix();
        Wind r1Wind = r1.getWind();
        assertEquals(dateFormat.parse("2013-10-11 07:16:33 UTC"), r1TimePoint.asDate());
        assertEquals(r1Fix.getPosition(), r1Position);
        // SOG / COG: 12.48,305.0
        assertEquals(12.48, r1Fix.getSpeed().getKnots(), 0.000001);
        assertEquals(305.0, r1Fix.getSpeed().getBearing().getDegrees(), 0.000001);
        assertEquals(45.657005, r1Position.getLatDeg(), 0.0000001);
        assertEquals(13.70788, r1Position.getLngDeg(), 0.0000001);
        assertEquals(13.8, r1Wind.getKnots(), 0.000001);
        assertEquals(97.0, r1Wind.getBearing().getDegrees(), 0.000001);
        assertEquals(12.65, Double.valueOf(r1.getField(FieldType.boat_speed)), 0.000001);
        
        recordIter.next();
        recordIter.next();
        assertFalse(recordIter.hasNext());
        
    }
}
