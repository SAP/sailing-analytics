package com.sap.sailing.kiworesultimport.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sap.sailing.kiworesultimport.StartReport;
import com.sap.sailing.kiworesultimport.StartReportParser;
import com.sap.sse.util.XmlUtil;

public class StartReportParserImpl implements StartReportParser {
    private Document parseDocument(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = XmlUtil.getSecureDocumentBuilderFactory();
        return dbf.newDocumentBuilder().parse(inputStream);
    }

    @Override
    public StartReport parse(InputStream inputStream, String sourceName) throws SAXException, IOException, ParserConfigurationException {
        Document doc = parseDocument(inputStream);
        StartReport result = new StartReportImpl(doc.getElementsByTagName("star:startbericht").item(0), sourceName);
        return result;
    }
}
