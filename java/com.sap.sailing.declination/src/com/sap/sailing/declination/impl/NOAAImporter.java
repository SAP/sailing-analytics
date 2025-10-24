package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.domain.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.util.XmlUtil;

/**
 * Imports magnetic declination data for earth from NOAA (http://www.ngdc.noaa.gov)
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class NOAAImporter extends DeclinationImporter {
    private static final String QUERY_URL = "https://www.ngdc.noaa.gov/geomag-web/calculators/calculateDeclination";
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

    @Override
    public Declination importRecord(Position position, TimePoint timePoint) throws IOException, ParserConfigurationException, SAXException {
        Declination result = null;
        Date date = timePoint.asDate();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        final int year = calendar.get(Calendar.YEAR);
        URL url = new URL(QUERY_URL+"?key=zNEw7&lon1="+position.getLngDeg()+"&lat1="+position.getLatDeg()+"&startYear=" + calendar.get(Calendar.YEAR) + "&startMonth="
                + (calendar.get(Calendar.MONTH) + 1) + "&startDay=" + calendar.get(Calendar.DAY_OF_MONTH)+"&resultFormat=xml"
                +(year < 2024 ? "&model=IGRF":"")); // WMM / WMMHR start only in 2024; earlier years need to be solved by the IGRF model
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.66 Safari/537.36");
        DocumentBuilder builder = XmlUtil.getSecureDocumentBuilderFactory().newDocumentBuilder();
        final InputStream inputStream = conn.getInputStream();
        Document doc = builder.parse(inputStream);
        Element maggridresultNode = (Element) doc.getFirstChild();
        Element resultNode = (Element) maggridresultNode.getElementsByTagName("result").item(0);
        String declination = resultNode.getElementsByTagName("declination").item(0).getTextContent().trim();
        String declinationAnnualChangeInMinutes = resultNode.getElementsByTagName("declination_sv").item(0).getTextContent().trim();
        inputStream.close();
        double declinationAsDouble = declination.equals("nan") ? Double.NaN : Double.valueOf(declination);
        double declinationAnnualChangeInDegreesAsDouble = declinationAnnualChangeInMinutes.equals("nan") ? Double.NaN : Double.valueOf(declinationAnnualChangeInMinutes);
        result = new DeclinationRecordImpl(position, timePoint, new DegreeBearingImpl(declinationAsDouble),
                new DegreeBearingImpl(declinationAnnualChangeInDegreesAsDouble));
        return result;
    }

}
