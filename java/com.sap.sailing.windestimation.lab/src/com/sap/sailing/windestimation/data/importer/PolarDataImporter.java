package com.sap.sailing.windestimation.data.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.parser.ParseException;

import com.sap.sailing.windestimation.util.LoggingUtil;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class PolarDataImporter {
    private static final Logger logger = Logger.getLogger(PolarDataImporter.class.getName());

    private static final String polarDataSourceUrl = "https://www.sapsailing.com";

    public static final String polarDataFilePath = "polarData.dat";

    private static final String RESOURCE = "polars/api/polar_data";

    public PolarDataImporter() {
    }

    public InputStream retrievePolarDataRegressionAsBytes(String bearerToken) throws IOException, ParseException {
        LoggingUtil.logInfo("Loading polar regression data from remote server " + polarDataSourceUrl);
        final InputStream inputStream = getContentFromResponse(bearerToken);
        LoggingUtil.logInfo("Loading polar regression data succeeded");
        return inputStream;
    }

    public void persistPolarDataRegressionAsBytes(File targetFile, InputStream inputStream) throws IOException {
        logger.info("Persisting polar regression data to file "+targetFile+" (canonical path: "+targetFile.getCanonicalPath()+")");
        FileUtils.copyInputStreamToFile(inputStream, targetFile);
        logger.info("Persisting polar regression data succeeded");
    }

    private String getAPIString() {
        return polarDataSourceUrl + (polarDataSourceUrl.endsWith("/") ? "" : "/") + RESOURCE;
    }

    protected InputStream getContentFromResponse(String bearerToken) throws IOException, ParseException {
        HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        HttpGet getProcessor = new HttpGet(getAPIString());
        getProcessor.setHeader("Authorization", "Bearer "+bearerToken);
        HttpResponse processorResponse = client.execute(getProcessor);
        return processorResponse.getEntity().getContent();
    }

    public static void main(String[] args) throws Exception {
        PolarDataImporter polarDataImporter = new PolarDataImporter();
        polarDataImporter.importPolarData(args[0]);
    }

    public void importPolarData(String bearerToken) throws IOException, ParseException {
        InputStream inputStream = retrievePolarDataRegressionAsBytes(bearerToken);
        persistPolarDataRegressionAsBytes(new File(polarDataFilePath), inputStream);
    }

}
