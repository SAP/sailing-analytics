package com.sap.sailing.kiworesultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.kiworesultimport.Boat;
import com.sap.sailing.kiworesultimport.Crewmember;
import com.sap.sailing.kiworesultimport.ParserFactory;
import com.sap.sailing.kiworesultimport.BoatResultInRace;
import com.sap.sailing.kiworesultimport.ResultList;
import com.sap.sailing.kiworesultimport.ResultListParser;
import com.sap.sailing.kiworesultimport.Skipper;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ParserTest {
    private static final String SAMPLE_INPUT_NAME = "2011-06-18_49er_Wettfahrt_2_Extra.xml";
    private static final String RESOURCES = "resources/";

    private InputStream getInputStream(String filename) throws FileNotFoundException, IOException {
        return new FileInputStream(getFile(filename));
    }

    private File getFile(String filename) {
        return new File(RESOURCES + filename);
    }

    @Test
    public void testLoadingSampleXML() throws SAXException, IOException, ParserConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(getSampleInputStream());
        assertNotNull(doc);
        Node resultList = doc.getElementsByTagName("ResultList").item(0);
        assertNotNull(resultList);
    }

    private InputStream getSampleInputStream() throws FileNotFoundException, IOException {
        return getInputStream(SAMPLE_INPUT_NAME);
    }
    
    @Test
    public void testEmptyIsafID() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        ResultListParser parser = ParserFactory.INSTANCE.createResultListParser();
        ResultList resultList = parser.parse(getSampleInputStream(), SAMPLE_INPUT_NAME);
        assertNotNull(resultList);
        assertNull(resultList.getBoatBySailID("SWE 1196").getCrew().getSkipper().getIsaf());
    }
    
    @Test
    public void testEmptyStatus() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        ResultListParser parser = ParserFactory.INSTANCE.createResultListParser();
        ResultList resultList = parser.parse(getSampleInputStream(), SAMPLE_INPUT_NAME);
        assertNotNull(resultList);
        assertNull(resultList.getBoatBySailID("SWE 1196").getResultsInRaces().iterator().next().getStatus());
        assertEquals(MaxPointsReason.NONE, resultList.getBoatBySailID("SWE 1196").getResultsInRaces()
                .iterator().next().getMaxPointsReason());
    }
    
    @Test
    public void testNonEmptyStatus() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        ResultListParser parser = ParserFactory.INSTANCE.createResultListParser();
        ResultList resultList = parser.parse(getSampleInputStream(), SAMPLE_INPUT_NAME);
        assertNotNull(resultList);
        final Boat GER1199 = resultList.getBoatBySailID("GER 1199");
        assertEquals("DNC", GER1199.getResultsInRaces().iterator().next().getStatus());
        assertEquals(MaxPointsReason.DNC, GER1199.getResultsInRaces()
                .iterator().next().getMaxPointsReason());
        assertEquals(47, (int) GER1199.getRank());
        assertEquals(25.00, GER1199.getResultsInRace(1).getPoints(), 0.00000001);
        assertEquals(25.00, GER1199.getResultsInRace(2).getPoints(), 0.00000001);
    }
    
    @Test
    public void testObtainingResultList() throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        ResultListParser parser = ParserFactory.INSTANCE.createResultListParser();
        ResultList resultList = parser.parse(getSampleInputStream(), SAMPLE_INPUT_NAME);
        assertNotNull(resultList);
        assertEquals("D:\\Programme\\KWSailing\\eventlogos\\KielerWoche_Ergebnislistenkopf_2011.jpg", resultList.getImagePath());
        assertEquals(new String(new char[] { (char) 160  /* non-breaking space */}), resultList.getLegend());
        Iterable<Boat> boats = resultList.getBoats();
        assertFalse(Util.isEmpty(boats));
        assertEquals(48, Util.size(boats));
        Boat DEN9 = resultList.getBoatBySailID("DEN 9");
        assertNotNull(DEN9);
        assertEquals(7, (int) DEN9.getRank());
        Skipper DEN9Skipper = DEN9.getCrew().getSkipper();
        assertEquals("Norregaard, Allan (1981) Kolding", DEN9Skipper.getName());
        assertEquals(new URL("http://www.sailing.org/biog.php?id=DENAN1"), DEN9Skipper.getIsaf());
        Iterable<Crewmember> DEN9Crewmembers = DEN9.getCrew().getCrewmembers();
        assertEquals(1, Util.size(DEN9Crewmembers));
        Crewmember DEN9Crewmember = DEN9Crewmembers.iterator().next();
        assertEquals("Lang, Peter (1989) Kolding Sejlklub", DEN9Crewmember.getName());
        Iterable<BoatResultInRace> DEN9Races = DEN9.getResultsInRaces();
        assertNotNull(DEN9Races);
        assertEquals(2, Util.size(DEN9Races));
        Iterator<BoatResultInRace> i = DEN9Races.iterator();
        BoatResultInRace r1 = i.next();
        assertEquals(9.00, r1.getPoints(), 0.0000000001);
        assertEquals(1, (int) r1.getRaceNumber());
        BoatResultInRace r2 = i.next();
        assertEquals(1.00, r2.getPoints(), 0.0000000001);
        assertEquals(2, (int) r2.getRaceNumber());
        assertEquals(new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 18, 16, 26).getTime()), resultList.getTimePointPublished());
    }
}
