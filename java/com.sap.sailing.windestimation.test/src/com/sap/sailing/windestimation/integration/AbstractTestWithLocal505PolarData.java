package com.sap.sailing.windestimation.integration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;
import com.sap.sailing.polars.jaxrs.client.FileBasedPolarDataClient;

public abstract class AbstractTestWithLocal505PolarData extends OnlineTracTracBasedTest {
    protected AbstractTestWithLocal505PolarData() throws MalformedURLException, URISyntaxException {
        super();
    }

    protected PolarDataService createPolarDataService() throws ClassNotFoundException, IOException, ParseException, InterruptedException {
        final PolarDataServiceImpl polarDataService = new PolarDataServiceImpl();
        final com.sap.sailing.domain.tractracadapter.DomainFactory domainFactoryImpl = getDomainFactory();
        final DomainFactory baseDomainFactory = domainFactoryImpl.getBaseDomainFactory();
        final FileBasedPolarDataClient client = new FileBasedPolarDataClient(new File("resources/polar_data"), polarDataService, baseDomainFactory);
        client.updatePolarDataRegressions();
        getTrackedRace().setPolarDataService(polarDataService);
        return polarDataService;
    }
}
