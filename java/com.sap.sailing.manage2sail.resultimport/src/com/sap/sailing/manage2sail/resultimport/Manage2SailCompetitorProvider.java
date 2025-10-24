package com.sap.sailing.manage2sail.resultimport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.competitorimport.CompetitorProvider;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.resultimport.impl.CompetitorResolver;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

public class Manage2SailCompetitorProvider extends AbstractManage2SailProvider implements CompetitorProvider {
    private static final long serialVersionUID = 7389956404604333931L;
    private final static String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";
    private final static String ID_PREFIX = "Manage2Sail";
    private final CompetitorDocumentProvider documentProvider;
    private final ResourceBundleStringMessages stringMessages;
    private final CompetitorResolver competitorResolver;

    public Manage2SailCompetitorProvider(ParserFactory parserFactory, ResultUrlRegistry resultUrlRegistry) {
        super(parserFactory, resultUrlRegistry);
        documentProvider = new CompetitorDocumentProvider(this);
        competitorResolver = new CompetitorResolver(documentProvider, parserFactory, ID_PREFIX);
        stringMessages = new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME, getClass().getClassLoader());
    }
    
    @Override
    public Map<String, Set<String>> getHasCompetitorsForRegattasInEvent() throws IOException, URISyntaxException, SAXException, ParserConfigurationException {
        return competitorResolver.getHasCompetitorsForRegattasInEvent();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Iterable<CompetitorDescriptor> getCompetitorDescriptors(String eventName, String regattaName) throws JAXBException, IOException, URISyntaxException, SAXException, ParserConfigurationException {
        return competitorResolver.getCompetitorDescriptors(eventName, regattaName);
    }

    protected CompetitorDocumentProvider getDocumentProvider() {
        return documentProvider;
    }

    @Override
    public String getHint(Locale locale) {
        return stringMessages.get(locale, "CompetitorImporterHint");
    }
}
