package com.sap.sse.aicore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.impl.AICoreImpl;
import com.sap.sse.aicore.impl.ChatSessionImpl;
import com.sap.sse.common.Util;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Provides connectivity and a facade to SAP AI Core and large language models hosted by it. To start with, create an
 * instance using the static {@link #create(Credentials)} factory method on this interface. It needs {@link Credentials}
 * which can be obtained, e.g., from your BTP Cockpit's "View Credentials" link in the top right corner. A
 * {@link CredentialsParser} can be obtained using {@link CredentialsParser#create()} to turn this JSON document into a
 * {@link Credentials} object. With the resulting {@link AICore} object you can make authenticated calls to
 * SAP AI Core, {@link #getDeployments() find out} about {@link Deployment}s in the landscape your credentials grant
 * you access to, and pick a deployment of a large language model by the LLM's name which you can then use to
 * {@link #createChatSession(String) create} a {@link ChatSession}.<p>
 * 
 * Example:
 * <pre>
 *   final AICore aiCore = AICore.create(CredentialsParser.create().parse(System.getProperty(CREDENTIALS_SYSTEM_PROPERTY_NAME)));
 *   final ChatSession chatSession = aiCore.createChatSession("gpt-4o-mini").get();
 *   final String response = chatSession
 *                             .addSystemPrompt("You are a teacher.")
 *                             .addPrompt("Explain quantum mechanics in easy terms!")
 *                             .submit();
 * </pre>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface AICore {
    Logger logger = Logger.getLogger(AICore.class.getName());

    /**
     * Name of the system property in which we look for default credentials that will be used by the
     * {@link #getDefault} method to obtain valid credentials.
     */
    String CREDENTIALS_SYSTEM_PROPERTY_NAME = "sap.aicore.credentials";
    
    /**
     * Produces a default {@link AICore} instance using credentials from the system property whose name
     * is specified by {@link #CREDENTIALS_SYSTEM_PROPERTY_NAME}. If that property is not set, {@code null}
     * is returned.
     */
    static AICore getDefault() throws MalformedURLException, ParseException {
        final String systemProperty = System.getProperty(CREDENTIALS_SYSTEM_PROPERTY_NAME);
        final AICore result;
        if (systemProperty == null) {
            logger.warning("No credentials provided for AICore service through system property "+CREDENTIALS_SYSTEM_PROPERTY_NAME+
                    "; cannot produce an authenticated default service instance");
            result = null;
        } else {
            result = AICore.create(CredentialsParser.create().parse(systemProperty));
        }
        return result;
    }
    
    static AICore create(final Credentials credentials) {
        return new AICoreImpl(credentials, ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor());
    }

    Iterable<Deployment> getDeployments() throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException;
    
    default Iterable<String> getModelNames() throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException {
        return Util.map(getDeployments(), d -> d.getModelName());
    }

    default Optional<Deployment> getDeploymentByModelName(String modelName) throws UnsupportedOperationException,
            ClientProtocolException, URISyntaxException, IOException, ParseException {
        return Util.stream(getDeployments()).filter(d -> d.getModelName().equals(modelName)).findAny();
    }

    default Optional<ChatSession> createChatSession(String modelName) throws UnsupportedOperationException,
            ClientProtocolException, URISyntaxException, IOException, ParseException {
        return getDeploymentByModelName(modelName).map(d -> new ChatSessionImpl(this, d));
    }
    
    default ChatSession createChatSession(Deployment deployment) {
        return new ChatSessionImpl(this, deployment);
    }

    HttpGet getHttpGetRequest(String pathSuffix) throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException;

    HttpPost getHttpPostRequest(String pathSuffix) throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException;

    JSONObject getJSONResponse(HttpUriRequest request) throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException;

    /**
     * Submits the request asynchronously in a background executor, trying to respect API rate limits and re-trying with an
     * exponential back-off strategy when having exceeded the limit.
     * 
     * @param callback
     *            invoked with the response {@link JSONObject} when submitting the request succeeded
     * @param exceptionHandler
     *            can contain an exception handler; this won't be invoked when an issue with rate limiting is detected
     *            and a back-off strategy is used; unrecoverable exceptions will be passed to the exception handler if
     *            present; otherwise they will simply be logged with level {@link Level#SEVERE SEVERE}.
     */
    void getJSONResponse(HttpUriRequest request, Consumer<JSONObject> resultCallback, Optional<Consumer<Exception>> exceptionHandler);
}
