package com.sap.sailing.kiworesultimport.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sap.sailing.kiworesultimport.ResultList;
import com.sap.sailing.kiworesultimport.ResultListParser;
import com.sap.sse.util.XmlUtil;

public class ResultListParserImpl implements ResultListParser {
    private Document parseDocument(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
        return XmlUtil.getSecureDocumentBuilderFactory().newDocumentBuilder().parse(inputStream);
    }

    @Override
    public ResultList parse(InputStream inputStream, String sourceName) throws SAXException, IOException, ParserConfigurationException {
        Document doc = parseDocument(inputStream);
        ResultList result = new ResultListImpl(doc.getElementsByTagName("ResultList").item(0), sourceName);
        return result;
    }

}
