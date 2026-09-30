package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.sailing.declination.Declination;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * A typical request for the BGS Geomag service looks like this:
 * <pre>
 *   https://geomag.bgs.ac.uk/web_service/GMModels/wmm/2020/?latitude=-80&longitude=240&altitude=0&date=2022-07-02&format=xml
 * </pre>
 * The response serves an XML document that has elements like this:
 * <pre>
&lt;geomagnetic-field-model-result&gt;
&lt;model revision="2020"&gt;wmm&lt;/model&gt;
&lt;date&gt;2022-07-02&lt;/date&gt;
&lt;coordinates&gt;
&lt;latitude units="deg (north)"&gt;-80&lt;/latitude&gt;
&lt;longitude units="deg (east)"&gt;240&lt;/longitude&gt;
&lt;altitude units="km"&gt;0.00&lt;/altitude&gt;
&lt;/coordinates&gt;
&lt;field-value&gt;
&lt;total-intensity units="nT"&gt;54912&lt;/total-intensity&gt;
&lt;declination units="deg (east)"&gt;69.125&lt;/declination&gt;
&lt;inclination units="deg (down)"&gt;-72.092&lt;/inclination&gt;
&lt;north-intensity units="nT"&gt;6017&lt;/north-intensity&gt;
&lt;east-intensity units="nT"&gt;15777&lt;/east-intensity&gt;
&lt;vertical-intensity units="nT"&gt;-52252&lt;/vertical-intensity&gt;
&lt;horizontal-intensity units="nT"&gt;16885&lt;/horizontal-intensity&gt;
&lt;/field-value&gt;
&lt;secular-variation&gt;
&lt;total-intensity units="nT/y"&gt;-83.4&lt;/total-intensity&gt;
&lt;declination units="arcmin/y (east)"&gt;-5.6&lt;/declination&gt;
&lt;inclination units="arcmin/y (down)"&gt;2.5&lt;/inclination&gt;
&lt;north-intensity units="nT/y"&gt;30.4&lt;/north-intensity&gt;
&lt;east-intensity units="nT/y"&gt;1.8&lt;/east-intensity&gt;
&lt;vertical-intensity units="nT/y"&gt;91.7&lt;/vertical-intensity&gt;
&lt;horizontal-intensity units="nT/y"&gt;12.6&lt;/horizontal-intensity&gt;
&lt;/secular-variation&gt;
&lt;/geomagnetic-field-model-result&gt;
 * </pre>
 * representing the declination in degrees, as well as its annual change.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BGSImporter extends DeclinationImporter {
//    private static final String URL_PATTERN = "http://geomag.bgs.ac.uk/web_service/GMModels/bggm/2015/?latitude=%f&longitude=%f&altitude=0&date=%d-%d-%d&format=xml"; 
//    private static final String URL_PATTERN = "http://geomag.bgs.ac.uk/web_service/GMModels/wmm/2020/?latitude=%f&longitude=%f&altitude=0&date=%d-%d-%d&format=xml"; 
    private static final String URL_PATTERN_PRE_2025 = "http://geomag.bgs.ac.uk/web_service/GMModels/igrf/13/?latitude=%f&longitude=%f&altitude=0&date=%d-%d-%d&format=xml"; 
    private static final String URL_PATTERN_POST_2025 = "https://geomag.bgs.ac.uk/web_service/GMModels/wmm/2025?latitude=%f&longitude=%f&altitude=0&date=%d-%d-%d&format=xml";
    
    private static class XmlElementHandler extends DefaultHandler {
        private Date dateAsDecimalYear;
        private double latDeg;
        private double lngDeg;
        private double declinationDeg;
        private double annualDeclinationChangeDeg;
        private boolean inFieldValue;
        private boolean inSecularVariation;
        
        private StringBuilder elementContent;
        
        public XmlElementHandler() {
            elementContent = new StringBuilder();
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            switch (qName) {
            case "field-value":
                inFieldValue = true;
                break;
            case "secular-variation":
                inSecularVariation = true;
                break;
            }
            elementContent.delete(0, elementContent.length());
            super.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            elementContent.append(ch, start, length);
            super.characters(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            final String content = elementContent.toString();
            switch (qName) {
            case "field-value":
                inFieldValue = false;
                break;
            case "secular-variation":
                inSecularVariation = false;
                break;
            case "date":
                try {
                    dateAsDecimalYear = new SimpleDateFormat("yyyy-MM-dd").parse(content);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "latitude":
                latDeg = Double.valueOf(content);
                break;
            case "longitude":
                lngDeg = Double.valueOf(content);
                break;
            case "declination":
                if (inFieldValue) {
                    declinationDeg = Double.valueOf(content);
                } else if (inSecularVariation) {
                    annualDeclinationChangeDeg = Double.valueOf(content)/60.;
                }
                break;
            }
            super.endElement(uri, localName, qName);
        }
        
        Declination getDeclination() {
            return new DeclinationRecordImpl(new DegreePosition(latDeg, lngDeg), new MillisecondsTimePoint(dateAsDecimalYear),
                    new DegreeBearingImpl(declinationDeg), new DegreeBearingImpl(annualDeclinationChangeDeg));
        }
    }
    
    public Declination getDeclinationFromXml(InputStream is) throws SAXException, IOException, ParserConfigurationException {
        XmlElementHandler handler = new XmlElementHandler();
        SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
        return handler.getDeclination();
    }

    @Override
    public Declination importRecord(Position position, TimePoint timePoint)
            throws IOException, ParserConfigurationException, SAXException {
        final Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(timePoint.asDate());
        final int year = cal.get(Calendar.YEAR);
        final URL url = new URL(String.format(year >= 2025 ? URL_PATTERN_POST_2025 : URL_PATTERN_PRE_2025, position.getLatDeg(), position.getLngDeg(), year, cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)));
        return getDeclinationFromXml(url.openStream());
    }
}
