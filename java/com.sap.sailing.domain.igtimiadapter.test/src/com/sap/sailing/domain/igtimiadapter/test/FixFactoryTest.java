package com.sap.sailing.domain.igtimiadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sap.sailing.domain.igtimiadapter.FixFactory;
import com.sap.sailing.domain.igtimiadapter.datatypes.COG;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsAltitude;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsLatLong;
import com.sap.sailing.domain.igtimiadapter.datatypes.SOG;
import com.sap.sse.common.Util;

public class FixFactoryTest {
    private FixFactory fixFactory;
    
    @BeforeEach
    public void setUp() {
        fixFactory = new FixFactory();
    }
    
    @Test
    public void testUnknownFixTypes() throws ParseException, InvalidProtocolBufferException {
        final String[] messages = new String[] {
                               "{\"DC-FE-AAKG\":[\"EjIKMAoJMgcI1Oe6/LQyChJKEAjU57r8tDIRKQZINIEi0j8SCkRDLU1NLUFBQ04gAyjmAg==\","+
                                                "\"ElUKUwomCiQIyOu6/LQyETJZ3H9kKUtAGd+mP/uRQiRAIfC+AECI9gYAeAcKCxoJCMjruvy0MhAECgsSCQjI67r8tDIQARIKREMtTU0tQUFDTiADKOYC\","+
                                                "\"EjIKMAoJMgcIyOu6/LQyChJKEAjI67r8tDIRb04lA0AV0z8SCkRDLU1NLUFBQ04gAyjmAg==\"]}",
                               "{\"DC-FE-AAKG\":[\"ElUKUwomCiQIsPO6/LQyEaULxMVkKUtAGeJNkVaPQiRAIfC+AECI9gYAeAcKCxoJCLDzuvy0MhAECgsSCQiw87r8tDIQARIKREMtTU0tQUFDTiADKOYC\","+
                                                "\"EjIKMAoJMgcIsPO6/LQyChJKEAiw87r8tDIR4bIKmwEuyD8SCkRDLU1NLUFBQ04gAyjmAg==\","+
                                                "\"ElUKUwomCiQIpPe6/LQyEf48IONkKUtAGdWUZB2OQiRAIfC+AECI9gYAeAcKCxoJCKT3uvy0MhAECgsSCQik97r8tDIQARIKREMtTU0tQUFDTiADKOYC\"]}"
                                               };
        List<Fix> fixes = new ArrayList<>();
        for (final String message : messages) {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(message);
            Util.addAll(fixFactory.createFixes(jsonObject), fixes);
        }
        assertTrue(!Util.isEmpty(fixes)); // this largely asserts that no exception was thrown although there were unknown fix types (22/23)
        assertEquals(3, Util.size(Util.filter(fixes, f->f instanceof GpsLatLong)));
        assertEquals(3, Util.size(Util.filter(fixes, f->f instanceof GpsAltitude)));
        assertEquals(3, Util.size(Util.filter(fixes, f->f instanceof COG)));
        assertEquals(3, Util.size(Util.filter(fixes, f->f instanceof SOG)));
    }
}
