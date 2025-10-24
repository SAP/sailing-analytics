package com.sap.sailing.server.gateway.deserialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCCertificatesCollection;
import com.sap.sailing.domain.orc.ORCCertificatesImporter;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.ORCCertificateJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.ORCCertificateJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class ORCCertificateJsonDeserializerTest {
    private ORCCertificateJsonSerializer serializer;
    private ORCCertificateJsonDeserializer deserializer;
    private static final String RESOURCES = "resources/rms/";

    @BeforeEach
    public void setup() {
        deserializer = new ORCCertificateJsonDeserializer();
        serializer = new ORCCertificateJsonSerializer();
    }

    @Test
    public void testNewFormateOrcCertificateDeserializer() throws FileNotFoundException, IOException, ParseException {
        File fileGER = new File(RESOURCES + "newFormatCertificate.json");
        ORCCertificatesCollection importer = ORCCertificatesImporter.INSTANCE.read(new FileInputStream(fileGER));
        Iterator<ORCCertificate> iterator = importer.getCertificates().iterator();
        assertTrue(iterator.hasNext());
        ORCCertificate certificate = iterator.next();
        final Speed[] expectedTrueWindSpeeds = { new KnotSpeedImpl(6.1), new KnotSpeedImpl(8.2), new KnotSpeedImpl(10.6), new KnotSpeedImpl(12.1), new KnotSpeedImpl(14.9), new KnotSpeedImpl(16.7), new KnotSpeedImpl(20.1) };
        assertTrue(Arrays.equals(expectedTrueWindSpeeds, certificate.getTrueWindSpeeds()));
        final Bearing[] expectedTrueWindAngles = { new DegreeBearingImpl(52.7), new DegreeBearingImpl(64.4), new DegreeBearingImpl(75), new DegreeBearingImpl(90),
                new DegreeBearingImpl(110), new DegreeBearingImpl(120), new DegreeBearingImpl(135), new DegreeBearingImpl(150) };
        assertTrue(Arrays.equals(expectedTrueWindAngles, certificate.getTrueWindAngles()));
        JSONObject serializedCertificate = serializer.serialize(certificate);
        assertNotNull(serializedCertificate);
        ORCCertificate certificateAfterDeserialize = deserializer.deserialize(serializedCertificate);
        assertEquals(certificate.getId(), certificateAfterDeserialize.getId());
        assertTrue(Arrays.equals(expectedTrueWindSpeeds, certificateAfterDeserialize.getTrueWindSpeeds()));
        assertTrue(Arrays.equals(expectedTrueWindAngles, certificateAfterDeserialize.getTrueWindAngles()));
    }

    @Test
    public void testOrcCertificateOldFormatDeserializer() throws IOException, ParseException {
        File fileGER = new File(RESOURCES + "oldFormatCertificate.json");
        ORCCertificatesCollection importer = ORCCertificatesImporter.INSTANCE.read(new FileInputStream(fileGER));
        Iterator<ORCCertificate> iterator = importer.getCertificates().iterator();
        assertTrue(iterator.hasNext());
        ORCCertificate certificate = iterator.next();
        assertTrue(Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS, certificate.getTrueWindSpeeds()));
        assertTrue(Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_ANGLES, certificate.getTrueWindAngles()));
        JSONObject serializedCertificate = serializer.serialize(certificate);
        assertNotNull(serializedCertificate);
        ORCCertificate certificateAfterDeserialize = deserializer.deserialize(serializedCertificate);
        assertEquals(certificate.getId(), certificateAfterDeserialize.getId());
        assertTrue(Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_SPEEDS, certificateAfterDeserialize.getTrueWindSpeeds()));
        assertTrue(Arrays.equals(ORCCertificate.ALLOWANCES_TRUE_WIND_ANGLES, certificateAfterDeserialize.getTrueWindAngles()));
    }
}
