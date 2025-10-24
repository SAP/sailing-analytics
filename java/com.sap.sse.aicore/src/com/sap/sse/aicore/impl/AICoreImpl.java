package com.sap.sse.aicore.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.Deployment;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class AICoreImpl implements AICore {
    private final static String DEPLOYMENTS_PATH = "/v2/lm/deployments";
    private static final String DEPLOYMENT_ID = "id";
    private static final String DEPLOYMENT_DETAILS = "details";
    private static final String DEPLOYMENT_DETAILS_RESOURCES = "resources";
    private static final String DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS = "backendDetails";
    private static final String DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL = "model";
    private static final String DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL_NAME = "name";
    
    private Credentials credentials;
    
    private final ScheduledExecutorService executor;
    private TimePoint timePointOfLastRequest;
    private final int maximumNumberOfRetries = 10;
    private static final Duration MAX_DURATION_BETWEEN_RETRIES = Duration.ONE_MINUTE;
    private static final Duration minimumDurationBetweenRequests = Duration.ONE_SECOND.divide(10); // limit request rate to three requests per second

    public AICoreImpl(Credentials credentials, ScheduledExecutorService executor) {
        super();
        this.credentials = credentials;
        this.executor = executor;
    }

    @Override
    public Iterable<Deployment> getDeployments() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final List<Deployment> result = new ArrayList<>();
        final JSONObject deploymentsJson = getJSONResponse(getHttpGetRequest(DEPLOYMENTS_PATH));
        for (final Object deploymentJson : (JSONArray) deploymentsJson.get("resources")) {
            final JSONObject deploymentJsonObject = (JSONObject) deploymentJson;
            final String id = (String) deploymentJsonObject.get(DEPLOYMENT_ID);
            final String modelName;
            if (deploymentJsonObject.containsKey(DEPLOYMENT_DETAILS)) {
                final JSONObject deploymentDetails = (JSONObject) deploymentJsonObject.get(DEPLOYMENT_DETAILS);
                if (deploymentDetails.containsKey(DEPLOYMENT_DETAILS_RESOURCES)) {
                    final JSONObject deploymentDetailsResources = (JSONObject) deploymentDetails.get(DEPLOYMENT_DETAILS_RESOURCES);
                    if (deploymentDetailsResources.containsKey(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS)) {
                        final JSONObject backendDetails = (JSONObject) deploymentDetailsResources.get(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS);
                        if (backendDetails.containsKey(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL)) {
                            final JSONObject model = (JSONObject) backendDetails.get(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL);
                            if (model.containsKey(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL_NAME)) {
                                modelName = (String) model.get(DEPLOYMENT_DETAILS_RESOURCES_BACKEND_DETAILS_MODEL_NAME);
                            } else {
                                modelName = null;
                            }
                        } else {
                            modelName = null;
                        }
                    } else {
                        modelName = null;
                    }
                } else {
                    modelName = null;
                }
            } else {
                modelName = null;
            }
            if (modelName != null) {
                result.add(new DeploymentImpl(id, modelName));
            }
        }
        return result;
    }
    
    @Override
    public boolean hasCredentials() {
        return credentials != null;
    }
    
    @Override
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    @Override
    public HttpGet getHttpGetRequest(final String pathSuffix) throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final HttpGet httpGet = new HttpGet(new URL(credentials.getAiApiUrl(), pathSuffix).toString());
        credentials.authorize(httpGet);
        return httpGet;
    }

    @Override
    public HttpPost getHttpPostRequest(final String pathSuffix) throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final HttpPost httpPost = new HttpPost(new URL(credentials.getAiApiUrl(), pathSuffix).toString());
        credentials.authorize(httpPost);
        return httpPost;
    }
    
    @Override
    public JSONObject getJSONResponse(HttpUriRequest request) throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final CloseableHttpClient client = getHttpClient();
        final HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new IOException("Error fetching "+request.getRequestLine()+": ("+response.getStatusLine().getStatusCode()+") "+response.getStatusLine().getReasonPhrase());
        }
        final JSONObject configurationsJson = (JSONObject) new JSONParser().parse(new InputStreamReader(response.getEntity().getContent()));
        return configurationsJson;
    }

    private CloseableHttpClient getHttpClient() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        return HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
    }

    @Override
    public void getJSONResponse(HttpUriRequest request, Consumer<JSONObject> resultCallback, Optional<Consumer<Exception>> exceptionHandler) {
        try {
            final CloseableHttpClient client = getHttpClient();
            scheduleWithRateLimit(client, request, resultCallback, exceptionHandler);
        } catch (UnsupportedOperationException | URISyntaxException | IOException | ParseException e) {
            exceptionHandler.map(handler->{ handler.accept(e); return null; }).orElseGet(()->{ logger.log(Level.SEVERE, "Exception trying to obtain HTTP client", e); return null; });
        }
    }

    /**
     * Schedules the {@code request} with an initial delay that depends on the {@link #minimumDurationBetweenRequests} and the
     * {@link #timePointOfLastRequest}. If the request fails with a HTTP code 429 indicating a rate limit violation, a back-off
     * strategy is used that tries to send the same request again after a while; this while keeps increasing exponentially until
     * the {@link #MAX_DURATION_BETWEEN_RETRIES} is reached and will then use that between retries. If {@link #maximumNumberOfRetries}
     * re-try attempts have failed, an exception is logged or forwarded to the {@code exceptionHandler} if present.
     */
    private synchronized void scheduleWithRateLimit(CloseableHttpClient client, HttpUriRequest request,
            Consumer<JSONObject> resultCallback, Optional<Consumer<Exception>> exceptionHandler) {
        scheduleWithRateLimitAndBackoff(getInitialDelayForRateLimiting(), client, request, resultCallback, exceptionHandler, /* retry */ 0);
    }

    private synchronized void scheduleWithRateLimitAndBackoff(Duration delayForRateLimiting, CloseableHttpClient client,
            HttpUriRequest request, Consumer<JSONObject> resultCallback,
            Optional<Consumer<Exception>> exceptionHandler, int retryNumber) {
        timePointOfLastRequest = TimePoint.now().plus(delayForRateLimiting);
        executor.schedule(SecurityUtils.getSubject().associateWith(()->{
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == 429) { // exceeded rate limit
                    if (retryNumber == maximumNumberOfRetries) {
                        final String message = ""+retryNumber+" retries failed. Giving up.";
                        throwIOExceptionOrLogSevere(exceptionHandler, message);
                    } else {
                        final Duration nextDelay = delayForRateLimiting.times(2).compareTo(MAX_DURATION_BETWEEN_RETRIES) > 0 ? MAX_DURATION_BETWEEN_RETRIES : delayForRateLimiting.times(2);
                        logger.info("Ran into rate limit; setting delay from "+delayForRateLimiting+" to "+nextDelay);
                        scheduleWithRateLimitAndBackoff(nextDelay, client, request, resultCallback, exceptionHandler, retryNumber+1);
                    }
                } else if (response.getStatusLine().getStatusCode() >= 400) {
                    final String message = "Error fetching "+request.getRequestLine()+": ("+response.getStatusLine().getStatusCode()+") "+response.getStatusLine().getReasonPhrase();
                    throwIOExceptionOrLogSevere(exceptionHandler, message);
                }
                final JSONObject configurationsJson = (JSONObject) new JSONParser().parse(new InputStreamReader(response.getEntity().getContent()));
                resultCallback.accept(configurationsJson);
            } catch (IOException | UnsupportedOperationException | ParseException e) {
                throwExceptionOrLogSevere(exceptionHandler, e);
            }
        }), delayForRateLimiting.asMillis(), TimeUnit.MILLISECONDS);
    }

    private void throwIOExceptionOrLogSevere(Optional<Consumer<Exception>> exceptionHandler, final String message) {
        throwExceptionOrLogSevere(exceptionHandler, new IOException(message));
    }

    private void throwExceptionOrLogSevere(Optional<Consumer<Exception>> exceptionHandler, final Exception exception) {
        exceptionHandler
            .map(handler->{ handler.accept(exception); return null; })
            .orElseGet(()->{ logger.severe(exception.getMessage()); return null; });
    }

    private Duration getInitialDelayForRateLimiting() {
        final Duration result;
        final TimePoint now = TimePoint.now();
        if (timePointOfLastRequest == null || timePointOfLastRequest.plus(minimumDurationBetweenRequests).compareTo(now) < 0) {
            result = Duration.NULL;
        } else {
            result = now.until(timePointOfLastRequest.plus(minimumDurationBetweenRequests));
        }
        return result;
    }
}
