package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;

/**
 * Imports magnetic declination data for earth from NOAA (http://www.ngdc.noaa.gov)
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class NOAAImporter {
    private static final String QUERY_URL = "http://www.ngdc.noaa.gov/geomag-web/calculators/calculateDeclination";
    private static final String REGEXP_DECLINATION = "<p class=\"indent\"><b>Declination</b> = ([0-9]*)&deg; ([0-9]*)' *([EW])";
    private static final String REGEXP_ANNUAL_CHANGE = "changing by *([0-9]*)&deg; *([0-9]*)' ([EW])/year *</p>";
    
    private final Pattern declinationPattern;
    private final Pattern annualChangePattern;

    public NOAAImporter() {
        super();
        this.declinationPattern = Pattern.compile(REGEXP_DECLINATION);
        this.annualChangePattern = Pattern.compile(REGEXP_ANNUAL_CHANGE);
    }

    protected Pattern getDeclinationPattern() {
        return declinationPattern;
    }

    protected Pattern getAnnualChangePattern() {
        return annualChangePattern;
    }

    public Declination importRecord(Position position, TimePoint timePoint) throws IOException, ParserConfigurationException, SAXException {
        Declination result = null;
        Date date = timePoint.asDate();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        URL url = new URL(QUERY_URL+"?lon1="+position.getLngDeg()+"&lat1="+position.getLatDeg()+"&startYear=" + calendar.get(Calendar.YEAR) + "&startMonth="
                + (calendar.get(Calendar.MONTH) + 1) + "&startDay=" + calendar.get(Calendar.DAY_OF_MONTH)+"&resultFormat=xml");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final InputStream inputStream = conn.getInputStream();
        Document doc = builder.parse(inputStream);
        Element maggridresultNode = (Element) doc.getFirstChild();
        Element resultNode = (Element) maggridresultNode.getElementsByTagName("result").item(0);
        String declination = resultNode.getElementsByTagName("declination").item(0).getTextContent().trim();
        String declinationAnnualChangeInMinutes = resultNode.getElementsByTagName("declination_sv").item(0).getTextContent().trim();
        inputStream.close();
        result = new DeclinationRecordImpl(position, timePoint, new DegreeBearingImpl(Double.valueOf(declination)),
                new DegreeBearingImpl(Double.valueOf(declinationAnnualChangeInMinutes)/60.));
        return result;
    }

    /**
     * Tries two things in parallel: fetch a more or less precise response from the online service and load
     * the requested year's declination values from a stored resource to look up a value that comes close.
     * The online lookup will be given preference. However, should it take longer than
     * <code>timeoutForOnlineFetchInMilliseconds</code>, then the method will return whatever it found
     * in the stored file, or <code>null</code> if no file exists for the year of <code>timePoint</code>.
     * 
     * @param timeoutForOnlineFetchInMilliseconds if 0, this means wait forever for the online result
     * @throws ParseException 
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
    public Declination getDeclination(final Position position, final TimePoint timePoint,
            long timeoutForOnlineFetchInMilliseconds) throws IOException, ClassNotFoundException, ParseException {
        final Declination[] result = new Declination[1];
        Thread fetcher = new Thread("Declination fetcher for "+position+"@"+timePoint) {
            @Override
            public void run() {
                try {
                    Declination fetched = importRecord(position, timePoint);
                    synchronized (result) {
                        result[0] = fetched;
                        result.notifyAll();
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        fetcher.start();
        synchronized (result) {
            if (result[0] == null) {
                try {
                    result.wait(timeoutForOnlineFetchInMilliseconds);
                } catch (InterruptedException e) {
                    // ignore; simply use value from file in this case
                }
            }
        }
        return result[0];
    }

}
