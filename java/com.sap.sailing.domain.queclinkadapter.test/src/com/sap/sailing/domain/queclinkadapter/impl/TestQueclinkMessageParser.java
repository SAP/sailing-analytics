package com.sap.sailing.domain.queclinkadapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.HBDAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.HBDServerAcknowledgement;
import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageParser;
import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class TestQueclinkMessageParser {
    private static final double EPSILON = 0.00000001;
    private MessageParser messageParser;

    @BeforeEach
    public void setUp() {
        this.messageParser = MessageParser.create();
    }
    
    @Test
    public void testSimpleMessageString() {
        final TimePoint now = TimePoint.now();
        final Message ackHBD = new HBDAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex("301303"), MessageParserImpl.parseCountNumberHex("033E"), "860599004785994", null, now);
        final String ackHBDAsString = ackHBD.getMessageString();
        assertEquals("+ACK:GTHBD,301303,860599004785994,,"+MessageParserImpl.formatAsYYYYMMDDHHMMSS(now)+",033E$", ackHBDAsString);
    }
    
    @Test
    public void testParsingSimpleHeartbeatAck() throws ParseException {
        final TimePoint now = TimePoint.now();
        final Message ackHBD = new HBDAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex("301303"), MessageParserImpl.parseCountNumberHex("033E"), "860599004785994", null, now);
        final String ackHBDAsString = ackHBD.getMessageString();
        final Message parsedMessage = messageParser.parse(ackHBDAsString);
        assertNotNull(parsedMessage);
        assertEquals(MessageType.HBD, parsedMessage.getType());
        assertEquals(Direction.ACK, parsedMessage.getDirection());
        assertTrue(parsedMessage instanceof HBDAcknowledgement);
    }
    
    @Test
    public void testParsingSimpleHeartbeatSack() throws ParseException {
        final Message sackHBD = new HBDServerAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex("301303"), MessageParserImpl.parseCountNumberHex("033E"));
        final String sackHBDAsString = sackHBD.getMessageString();
        final Message parsedMessage = messageParser.parse(sackHBDAsString);
        assertNotNull(parsedMessage);
        assertEquals(MessageType.HBD, parsedMessage.getType());
        assertEquals(Direction.SACK, parsedMessage.getDirection());
        assertTrue(parsedMessage instanceof HBDServerAcknowledgement);
    }
    
    @Test
    public void simplePatternTestForACKMessage() {
        final TimePoint now = TimePoint.now();
        final Message ackHBD = new HBDAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex("301303"), MessageParserImpl.parseCountNumberHex("033E"), "860599004785994", null, now);
        final Matcher matcher = MessageParserImpl.messagePattern.matcher(ackHBD.getMessageString());
        assertTrue(matcher.matches(), "Pattern "+MessageParserImpl.messagePattern.toString()+" doesn't match "+ackHBD.getMessageString());
        assertEquals("+ACK:", matcher.group(1));
        assertEquals("HBD", matcher.group(8));
        assertEquals("301303,860599004785994,,"+MessageParserImpl.formatAsYYYYMMDDHHMMSS(now)+",033E", matcher.group(9));
    }

    @Test
    public void simplePatternTestForSACKHeartbeatMessage() {
        final Message sackHBD = new HBDServerAcknowledgementImpl(MessageParserImpl.parseProtocolVersionHex("301303"), MessageParserImpl.parseCountNumberHex("033E"));
        final Matcher matcher = MessageParserImpl.messagePattern.matcher(sackHBD.getMessageString());
        assertTrue(matcher.matches(), "Pattern "+MessageParserImpl.messagePattern.toString()+" doesn't match "+sackHBD.getMessageString());
        assertEquals("+SACK:", matcher.group(1));
        assertEquals("HBD", matcher.group(8));
        assertEquals("301303,033E", matcher.group(9));
    }

    @Test
    public void simplePatternTestForBasicSACKMessage() {
        final String sackMessage = "+SACK:11F0$";
        final Matcher matcher = MessageParserImpl.messagePattern.matcher(sackMessage);
        assertTrue(matcher.matches(), "Pattern "+MessageParserImpl.messagePattern.toString()+" doesn't match "+sackMessage);
        assertEquals("+SACK:", matcher.group(1));
        assertEquals(null, matcher.group(8));
        assertEquals("11F0", matcher.group(9));
    }
    
    @Test
    public void parseLiveFRIMessage() throws ParseException {
        final String friMessageAsString = "+RESP:GTFRI,301201,860599002480051,,0,0,4,1,,,,-2.873013,52.161122,20240710164610,,,,,,1,,,,-2.873013,52.161122,20240710164615,,,,,,1,,,,-2.873013,52.161122,20240710164620,,,,,,1,,,,-2.873013,52.161122,20240710164625,,,,,,54,,2248$";
        final Message friMessage = messageParser.parse(friMessageAsString);
        assertNotNull(friMessage);
        assertTrue(friMessage instanceof FRIReport);
        final FRIReport friReport = (FRIReport) friMessage;
        assertEquals(4, friReport.getNumberOfFixes());
        final Position expectedPosition = new DegreePosition(52.161122, -2.873013);
        assertEquals(expectedPosition.getLatDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLatDeg(), EPSILON);
        assertEquals(expectedPosition.getLngDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLngDeg(), EPSILON);
        assertEquals(MessageParserImpl.parseTimeStamp("20240710164610"), friReport.getPositionRelatedReports()[0].getValidityTime());
    }
    
    @Test
    public void parseFRIMessageFromSpecification() throws ParseException {
        final String friMessageAsString = "+RESP:GTFRI,301303,860599004785994,,1,0,1,1,0.2,0,-43.4,117.129316,31.840015,20190923022045,0460,0000,550B,B969,,100,0001,20190923022046,034A$";
        final Message friMessage = messageParser.parse(friMessageAsString);
        assertNotNull(friMessage);
        assertTrue(friMessage instanceof FRIReport);
        final FRIReport friReport = (FRIReport) friMessage;
        final Position expectedPosition = new DegreePosition(31.840015, 117.129316);
        assertEquals(expectedPosition.getLatDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLatDeg(), EPSILON);
        assertEquals(expectedPosition.getLngDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLngDeg(), EPSILON);
        assertEquals(-43.4, friReport.getPositionRelatedReports()[0].getAltitude().getMeters(), EPSILON);
        assertEquals(0.2, friReport.getPositionRelatedReports()[0].getCogAndSog().getKilometersPerHour(), EPSILON);
        assertEquals(0, friReport.getPositionRelatedReports()[0].getCogAndSog().getBearing().getDegrees(), EPSILON);
        assertEquals(MessageParserImpl.parseTimeStamp("20190923022045"), friReport.getPositionRelatedReports()[0].getValidityTime());
    }
    
    @Test
    public void parseBufferedFRIMessage() throws ParseException {
        final String friMessageAsString = "+BUFF:GTFRI,301201,860599002480051,,0,0,4,1,,,,-2.873120,52.161232,20240710153241,,,,,,1,,,,-2.873120,52.161232,20240710153246,,,,,,1,,,,-2.873120,52.161232,20240710153251,,,,,,1,,,,-2.873120,52.161232,20240710153256,,,,,,64,,2193$";
        final Message friMessage = messageParser.parse(friMessageAsString);
        assertNotNull(friMessage);
        assertTrue(friMessage instanceof FRIReport);
        final FRIReport friReport = (FRIReport) friMessage;
        assertEquals(4, friReport.getNumberOfFixes());
        final Position expectedPosition = new DegreePosition(52.161232, -2.873120);
        assertEquals(expectedPosition.getLatDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLatDeg(), EPSILON);
        assertEquals(expectedPosition.getLngDeg(), friReport.getPositionRelatedReports()[0].getPosition().getLngDeg(), EPSILON);
        assertNull(friReport.getPositionRelatedReports()[0].getCogAndSog());
        assertEquals(MessageParserImpl.parseTimeStamp("20240710153241"), friReport.getPositionRelatedReports()[0].getValidityTime());
    }
    
    @Test
    public void countMessagesInLog() throws ParseException, IOException {
        final Reader reader = new InputStreamReader(getClass().getResourceAsStream("/queclink_stream"));
        final Iterable<Message> messages = messageParser.parse(reader);
        assertEquals(206, Util.size(messages));
    }
}
