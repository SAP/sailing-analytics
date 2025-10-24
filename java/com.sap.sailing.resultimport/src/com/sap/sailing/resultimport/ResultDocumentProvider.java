package com.sap.sailing.resultimport;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public interface ResultDocumentProvider {
    Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException, URISyntaxException, SAXException, ParserConfigurationException;
}
