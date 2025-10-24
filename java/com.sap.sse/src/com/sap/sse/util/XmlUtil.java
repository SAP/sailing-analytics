package com.sap.sse.util;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlUtil {
    /**
     * Creates an XML {@link Source} from an {@link InputStream} and secures it against XML External Entity (XXE)
     * attacks by disallowing doctype parsing and enforcing {@link XMLConstants#FEATURE_SECURE_PROCESSING secure
     * processing} to avoid denial-of-service attacks.
     */
    public static javax.xml.transform.Source getXmlSourceForInputStream(final InputStream is)
            throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException, SAXException {
        final SAXParserFactory spf = SAXParserFactory.newInstance();
        // This to defend against XXE:
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        spf.setXIncludeAware(false);
        javax.xml.transform.Source xmlSource = new javax.xml.transform.sax.SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(is));
        return xmlSource;
    }

    /**
     * Obtains a secure {@link DocumentBuilderFactory} that is configured to disallow doctype processing and includes.
     * This way, it avoids XML External Entity (XXE) attacks as well as resource depletion / denial-of-service.
     */
    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() throws ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final String DISALLOW_DOCTYPE_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
        dbf.setFeature(DISALLOW_DOCTYPE_FEATURE, true);
        dbf.setXIncludeAware(false);
        return dbf;
    }

}
