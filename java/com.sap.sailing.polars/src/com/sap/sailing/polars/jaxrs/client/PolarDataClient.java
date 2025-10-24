package com.sap.sailing.polars.jaxrs.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.polars.ReplicablePolarService;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;
import com.sap.sailing.polars.mining.PolarDataMiner;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

/**
 * This class is used to replicate polar data regressions calculation from the remote server using Apache
 * {@link HttpClient}
 * 
 * @author Oleg_Zheleznov
 * @author Axel Uhl (d043530)
 *
 */
public class PolarDataClient {

    private static final Logger logger = Logger.getLogger(PolarDataClient.class.getName());

    private static final String RESOURCE = "polars/api/polar_data";

    private final ReplicablePolarService polarDataService;
    private final String polarDataSourceURL;
    private final Optional<String> polarDataBearerToken;
    
    /**
     * Default constructor is missing because we need {@link PolarDataServiceImpl} to reach regressions
     * 
     * @param polarDataSourceURL
     *            archive server URL string
     * @param polarDataService
     *            {@link PolarDataServiceImpl} service to work with
     * @param polarDataBearerToken
     *            if present, this will be used in a <tt>Authorization: Bearer ...</tt> HTTP header to authenticate the
     *            request that fetches the polar data. The token must authenticate a subject with permission
     *            {@link SecuredDomainType#POLAR_DATA}.{@link DefaultActions#READ READ} on the "SERVER" object from
     *            where polar data is request as per the {@code polarDataSourceURL}.
     */
    public PolarDataClient(String polarDataSourceURL, ReplicablePolarService polarDataService, Optional<String> polarDataBearerToken) {
        this.polarDataService = polarDataService;
        this.polarDataSourceURL = polarDataSourceURL;
        this.polarDataBearerToken = polarDataBearerToken;
    }

    /**
     * This method is used to update {@link PolarDataMiner} regressions with data received from remote server. Before
     * the regression maps will be updated it may be cleaned
     */
    public void updatePolarDataRegressions() throws IOException, ParseException, InterruptedException, ClassNotFoundException {
        try {
            logger.log(Level.INFO, "Loading polar regression data from remote server " + polarDataSourceURL);
            final InputStream inputStream = getContentFromResponse();
            polarDataService.clearReplicaState();
            polarDataService.initiallyFillFrom(inputStream);
            logger.log(Level.INFO,
                    "Loading polar regression data from remote server " + polarDataSourceURL + " succeeded");
        } catch (Exception e) {
            // Catching ClientProtocolException to indicate problems with HTTP protocol
            logger.log(Level.WARNING, "Failed to load polar regression data from remote server " + polarDataSourceURL
                    + ", " + e.getMessage()+"; resetting polar data miner");
            polarDataService.resetState();
        }
    }

    private String getAPIString() {
        return polarDataSourceURL + (polarDataSourceURL.endsWith("/") ? "" : "/") + RESOURCE;
    }

    protected InputStream getContentFromResponse() throws IOException, ParseException {
        final HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        final HttpGet getProcessor = new HttpGet(getAPIString());
        polarDataBearerToken.ifPresent(bearerToken -> getProcessor.setHeader("Authorization", "Bearer " + bearerToken));
        final HttpResponse processorResponse = client.execute(getProcessor);
        if (processorResponse.getStatusLine().getStatusCode() >= 300) {
            throw new IOException("Error trying to load polar data from "+getAPIString()+": "
                    +processorResponse.getStatusLine().getReasonPhrase()+" ("+processorResponse.getStatusLine().getStatusCode()+")");
        }
        return processorResponse.getEntity().getContent();
    }
}
