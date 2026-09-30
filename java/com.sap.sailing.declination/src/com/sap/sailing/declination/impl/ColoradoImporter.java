package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
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
 * A typical request for the Geomag service looks like this:
 * <pre>
 *   http://magcalc.geomag.info/?model=WMM2015&sourcePage=oldCalc&decimalLatitude=10&decimalLongitude=20&minYear=2018&minMonth=1&minDay=23
 * </pre>
 * The response serves an XML document that has elements like this:
 * <pre>
 * &lt;DATE&gt;
 * 2018.06027
 * &lt;/DATE&gt;
 * &lt;LATITUDE units = "Degree"&gt;
 *  10.00000
 * &lt;/LATITUDE&gt;
 * &lt;LONGITUDE units="Degree"&gt;
 *  20.00000
 * &lt;/LONGITUDE&gt;
 * &lt;DECLINATION units="Degree"&gt;
 *  1.89255
 * &lt;/DECLINATION&gt;
 * &lt;DECLINATION_SV units="Degree"&gt;
 *  0.10649
 * &lt;/DECLINATION_SV&gt;
 * </pre>
 * representing the declination in degrees, as well as its annual change.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ColoradoImporter extends DeclinationImporter {
    private static final String URL_PATTERN = "http://magcalc.geomag.info/?model=WMM2015&sourcePage=oldCalc&decimalLatitude=%f&decimalLongitude=%f&minYear=%d&minMonth=%d&minDay=%d";
    
    private static class XmlElementHandler extends DefaultHandler {
        private double dateAsDecimalYear;
        private double latDeg;
        private double lngDeg;
        private double declinationDeg;
        private double annualDeclinationChangeDeg;
        
        private StringBuilder elementContent;
        
        public XmlElementHandler() {
            elementContent = new StringBuilder();
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
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
            case "DATE":
                dateAsDecimalYear = Double.valueOf(content);
                break;
            case "LATITUDE":
                latDeg = Double.valueOf(content);
                break;
            case "LONGITUDE":
                lngDeg = Double.valueOf(content);
                break;
            case "DECLINATION":
                declinationDeg = Double.valueOf(content);
                break;
            case "DECLINATION_SV":
                annualDeclinationChangeDeg = Double.valueOf(content);
                break;
            }
            super.endElement(uri, localName, qName);
        }
        
        Declination getDeclination() {
            return new DeclinationRecordImpl(new DegreePosition(latDeg, lngDeg), new MillisecondsTimePoint((long) ((dateAsDecimalYear-1970.)*365.0*24.0*3600.0*1000.0)),
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
        final URL url = new URL(String.format(URL_PATTERN, position.getLatDeg(), position.getLngDeg(), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)));
        return getDeclinationFromXml(url.openStream());
    }
}
