package com.sap.sailing.xrr.resultimport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.resultimport.ResultUrlProvider;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.xrr.resultimport.Parser;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Assumes that the single document referenced by the URL through the {@link ResultUrlProvider} passed to the
 * constructor is the single XRR document to read.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class UrlBasedXRRResultDocumentProvider implements ResultDocumentProvider {
    private static final Logger logger = Logger.getLogger(UrlBasedXRRResultDocumentProvider.class.getName());
    
    private final ResultUrlProvider resultUrlProvider;
    private final ParserFactory parserFactory;
    
    public UrlBasedXRRResultDocumentProvider(ResultUrlProvider resultUrlProvider, ParserFactory parserFactory) {
        this.resultUrlProvider = resultUrlProvider;
        this.parserFactory = parserFactory;
    }

    @Override
    public Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException, SAXException, ParserConfigurationException {
        List<ResultDocumentDescriptor> result = new ArrayList<>();
        for (URL url : resultUrlProvider.getReadableUrls()) {
            URLConnection eventResultConn = HttpUrlConnectionHelper.redirectConnection(url);
            final InputStream is = (InputStream) eventResultConn.getContent();
            final Parser parser = parserFactory.createParser(is, url.toString());
            try {
                final RegattaResults xrrParserResult = parser.parse();
                if (xrrParserResult != null) {
                    final List<ResultDocumentDescriptor> resultDocumentDescriptors = resolveResultDocumentDescriptors(xrrParserResult, url);
                    if (resultDocumentDescriptors != null) {
                        result.addAll(resultDocumentDescriptors);
                    }
                }
            } catch (JAXBException e) {
                logger.severe("Could not parse XRR document from URL: " + url.toString());
                e.printStackTrace();
            }
        }
        return result;
    }
    
    abstract public List<ResultDocumentDescriptor> resolveResultDocumentDescriptors(RegattaResults xrrParserResult, URL url);
}
