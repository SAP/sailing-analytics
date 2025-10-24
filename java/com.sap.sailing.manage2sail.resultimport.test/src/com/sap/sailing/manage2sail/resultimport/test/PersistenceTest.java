package com.sap.sailing.manage2sail.resultimport.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryImpl;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;

@SuppressWarnings("restriction")
public class PersistenceTest {
    @Test
    public void testResultUrlStoring() throws MalformedURLException {
        RacingEventService racingEventService = new RacingEventServiceImpl();
        DomainObjectFactory domainObjectFactory = racingEventService.getDomainObjectFactory();
        MongoObjectFactory mongoObjectFactory = racingEventService.getMongoObjectFactory();
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mongoObjectFactory, domainObjectFactory);
        String testProviderName = "TestProvider";
        URL testUrl = new URL(
                "http://manage2sail.com/api/public/links/event/d30883d3-2876-4d7e-af49-891af6cbae1b?accesstoken=bDAv8CwsTM94ujZ&mediaType=json");
        resultUrlRegistry.registerResultUrl(testProviderName, testUrl);
        Map<String, Set<URL>> resultUrls = domainObjectFactory.loadResultUrls();
        Assertions.assertTrue(resultUrls.containsKey(testProviderName));
        Set<URL> urls = resultUrls.get(testProviderName);
        Assertions.assertEquals(1, urls.size());
        URL url = urls.iterator().next();
        Assertions.assertEquals(testUrl, url);
        resultUrlRegistry.unregisterResultUrl(testProviderName, testUrl);
        resultUrls = domainObjectFactory.loadResultUrls();
        Assertions.assertFalse(resultUrls.containsKey(testProviderName));
    }
}
