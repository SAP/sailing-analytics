package com.sap.sailing.domain.igtimiadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sap.sailing.domain.igtimiadapter.FixFactory;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sse.common.Util;

public class LatestDatumTest {

    private final String sample1 = "{\"DC-MM-AACN\":[\"EiIKIAoSShAIkNG7/LQyESkGSDSBItI/EgpEQy1NTS1BQUNO\"]}";
    
    @Test
    public void testFixProductionFromSample1() throws ParseException, InvalidProtocolBufferException {
        Iterable<Fix> fixes = new FixFactory().createFixes((JSONObject) new JSONParser().parse(sample1));
        assertEquals(1, Util.size(fixes));
        Iterator<Fix> i = fixes.iterator();
        Fix fix1 = i.next();
        assertEquals("DC-MM-AACN", fix1.getSensor().getDeviceSerialNumber());
        assertEquals(Type.SOG, fix1.getType());
    }
}
