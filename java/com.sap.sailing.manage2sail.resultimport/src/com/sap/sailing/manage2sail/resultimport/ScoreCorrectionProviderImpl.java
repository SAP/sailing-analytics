package com.sap.sailing.manage2sail.resultimport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.xrr.resultimport.Parser;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class ScoreCorrectionProviderImpl extends AbstractManage2SailProvider implements ScoreCorrectionProvider {
    private static final Logger logger = Logger.getLogger(ScoreCorrectionProviderImpl.class.getName());
    private static final long serialVersionUID = 222663322974305822L;

    private final ResultDocumentProvider documentProvider;
    
    public ScoreCorrectionProviderImpl(ResultDocumentProvider documentProvider, ParserFactory parserFactory, ResultUrlRegistry resultUrlRegistry) {
        super(parserFactory, resultUrlRegistry);
        this.documentProvider = documentProvider;
    }

    public ScoreCorrectionProviderImpl(ParserFactory parserFactory, ResultUrlRegistry resultUrlRegistry) {
        super(parserFactory, resultUrlRegistry);
        this.documentProvider = new Manage2SailResultDocumentProvider(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Set<Util.Pair<String, TimePoint>>> getHasResultsForBoatClassFromDateByEventName() throws URISyntaxException, SAXException, ParserConfigurationException {
        Map<String, Set<Util.Pair<String, TimePoint>>> result = new HashMap<String, Set<Util.Pair<String,TimePoint>>>();
        try {
            for (ResultDocumentDescriptor resultDocDescr : documentProvider.getResultDocumentDescriptors()) {
                String eventName = resultDocDescr.getEventName() != null ? resultDocDescr.getEventName() : resultDocDescr.getRegattaName();
                String boatClass = resultDocDescr.getBoatClass();
                if (boatClass != null && eventName != null) {
                    if (resultDocDescr.getCompetitorGenderType() != null) {
                        boatClass += ", " + resultDocDescr.getCompetitorGenderType().name();
                    }
                    Set<Util.Pair<String, TimePoint>> eventResultsSet = result.get(eventName);
                    if (eventResultsSet == null) {
                        eventResultsSet = new HashSet<Util.Pair<String, TimePoint>>();
                        result.put(eventName, eventResultsSet);
                    }
                    eventResultsSet.add(new Util.Pair<String, TimePoint>(boatClass, resultDocDescr.getLastModified()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public RegattaScoreCorrections getScoreCorrections(String eventName, String boatClassName,
            TimePoint timePointPublished) throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        final Parser parser = resolveParser(eventName, boatClassName, timePointPublished);
        try {
            final RegattaResults regattaResults = parser.parse();
            return parser.getRegattaScoreCorrections(regattaResults, /* scoreCorrectionProvider */ this,
                    /* eventNameFilter */ Optional.of(eventName), /* boatClassNameFilter */ Optional.of(boatClassName));
        } catch (JAXBException e) {
            logger.info("Parse error during XRR import. Ignoring document " + parser.toString());
            logger.throwing(ScoreCorrectionProviderImpl.class.getName(), "getHasResultsForBoatClassFromDateByEventName", e);
        }
        return null;
    }
    
    @Override
    public RegattaScoreCorrections getScoreCorrections(InputStream inputStream) throws Exception {
        final Parser parser = getParserFactory().createParser(inputStream, inputStream.toString());
        final RegattaResults regattaResults = parser.parse();
        return parser.getRegattaScoreCorrections(regattaResults, this, /* eventNameFilter */ Optional.empty(),
                /* boatClassNameFilter */ Optional.empty());
    }

    private Parser resolveParser(String eventName, String boatClassName, TimePoint timePointPublished) throws IOException, URISyntaxException, SAXException, ParserConfigurationException {
        Parser result = null;
        for (ResultDocumentDescriptor resultDocDescr : documentProvider.getResultDocumentDescriptors()) {
            String boatClassAndGenderType = resultDocDescr.getBoatClass();
            if (resultDocDescr.getCompetitorGenderType() != null) {
                boatClassAndGenderType += ", " + resultDocDescr.getCompetitorGenderType().name();
            }
            if (eventName.equals(resultDocDescr.getEventName()) && boatClassName.equals(boatClassAndGenderType) &&
                    Util.equalsWithNull(resultDocDescr.getLastModified(), timePointPublished)) {
                result = getParserFactory().createParser(resultDocDescr.getInputStream(), resultDocDescr.getEventName());
                break;
            }
        }
        return result;
    }
}
