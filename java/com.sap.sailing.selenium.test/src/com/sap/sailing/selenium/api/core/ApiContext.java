package com.sap.sailing.selenium.api.core;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.representation.Form;

/**
 * The APIContext represents a "connection" to a server instance that provides a rest api. This is defined by the
 * context root (server instance), a web application context and the security credentials.
 */
public class ApiContext {

    public static final String SERVER_CONTEXT = "sailingserver"; //$NON-NLS-1$
    public static final String SHARED_SERVER_CONTEXT = "sharedsailingserver"; //$NON-NLS-1$
    public static final String SECURITY_CONTEXT = "security"; //$NON-NLS-1$
    public static final String ADMIN_USERNAME = "admin"; //$NON-NLS-1$
    public static final String ADMIN_PASSWORD = "admin"; //$NON-NLS-1$
    public static final String INVALID_TOKEN_SAMPLE = "INVALID_TOKEN_SAMPLE";

    private static final Logger logger = Logger.getLogger(ApiContext.class.getName());

    /** Jax-RS client. */
    private final Client client;
    /** Token to be sent as authorization header. */
    private final String token;
    /** Server instance. */
    private final String contextRoot;
    /** Web application context. */
    private final String context;

    /**
     * Create ApiContext by providing user name and password.
     * 
     * @param contextRoot
     *            server instance
     * @param context
     *            web application context
     * @param username
     *            user name
     * @param password
     *            password
     * @return authorized ApiContext
     */
    public static ApiContext createApiContext(String contextRoot, String context, String username, String password) {
        Authenticator authenticator = new Authenticator(contextRoot);
        String token = authenticator.authForToken(username, password);
        return new ApiContext(contextRoot, context, token);
    }

    /**
     * Creates an anonymous (unauthorized) ApiContext.
     * 
     * @param contextRoot
     *            server instance
     * @param context
     *            web application context
     * @return unauthorized ApiContext
     */
    public static ApiContext createAnonymousApiContext(String contextRoot, String context) {
        return new ApiContext(contextRoot, context, null);
    }

    /**
     * Creates an ApiContext with an invalid token. Useful for testing.
     * 
     * @param contextRoot
     *            server instance
     * @param context
     *            web application context
     * @return ApiContext with invalid token
     */
    public static ApiContext createApiContextWithInvalidToken(String contextRoot, String context) {
        return new ApiContext(contextRoot, context, INVALID_TOKEN_SAMPLE);
    }

    /**
     * Creates an ApiContext with administrator privileges.
     * 
     * @param contextRoot
     *            server instance
     * @param context
     *            web application context
     * @return administrator ApiContext
     */
    public static ApiContext createAdminApiContext(String contextRoot, String context) {
        return createApiContext(contextRoot, context, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private ApiContext(String contextRoot, String context, String token) {
        this.contextRoot = contextRoot;
        this.token = token;
        this.context = context;
        this.client = new Client();
    }

    /**
     * Sending a post request with query params and without payload.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R post(String url, Map<String, String> queryParams) {
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        String result;
        try {
            result = auth(wres.getRequestBuilder()).post(String.class);
        } catch (UniformInterfaceException e) {
            String error = "API POST request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
        return (R) JSONValue.parse(result);
    }

    /**
     * Sending a post request with query params and with form encoded payload.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @param formParams
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R post(String url, Map<String, String> queryParams, Map<String, String> formParams) {
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        Form form = new Form();
        if (formParams != null) {
            formParams.forEach(form::putSingle);
        }
        String result;
        try {
            result = auth(wres.getRequestBuilder()).entity(form).post(String.class);
        } catch (UniformInterfaceException e) {
            String error = "API POST request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
        return (R) JSONValue.parse(result);
    }

    /**
     * Sending a post request with query params and with json payload.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @param body
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R post(String url, Map<String, String> queryParams, JSONObject body) {
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        String result;
        try {
            result = auth(wres.getRequestBuilder()).entity(body.toJSONString(), MediaType.APPLICATION_JSON)
                    .post(String.class);
        } catch (UniformInterfaceException e) {
            String error = "API POST request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
        return (R) JSONValue.parse(result);
    }

    /**
     * Sending a post request with query params and with json payload.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @param body
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R put(String url, Map<String, String> queryParams, JSONObject body) {
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        String result;
        try {
            result = auth(wres.getRequestBuilder()).entity(body.toJSONString(), MediaType.APPLICATION_JSON)
                    .put(String.class);
        } catch (UniformInterfaceException e) {
            String error = "API PUT request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
        return (R) JSONValue.parse(result);
    }

    /**
     * Sending a put request with query params and with form encoded payload.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @param formParams
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R put(String url, Map<String, String> queryParams, Map<String, String> formParams) {
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        Form form = new Form();
        if (formParams != null) {
            formParams.forEach(form::putSingle);
        }
        String result;
        try {
            result = auth(wres.getRequestBuilder()).entity(form).put(String.class);
        } catch (UniformInterfaceException e) {
            String error = "API POST request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
        return (R) JSONValue.parse(result);
    }

    /**
     * Sending a delete request.
     * 
     * @param url
     *            context relative url of rest api endpoint
     */
    public void delete(String url) {
        WebResource wres = getWebResource().path(url);
        try {
            auth(wres.getRequestBuilder()).delete();
        } catch (UniformInterfaceException e) {
            String error = "API DELETE request " + url + " failed (rc=" + e.getResponse().getStatus() + "): "
                    + e.getResponse().getEntity(String.class);
            logger.severe(error);
            throw HttpException.forResponse(e.getResponse(), error).orElse(e);
        }
    }

    /**
     * Sending a get request.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R get(String url) {
        return (R) getObject(url, null);
    }

    /**
     * Sending a get request with query params.
     * 
     * @param url
     *            context relative url of rest api endpoint
     * @param queryParams
     * @return response entity as {@link JSONObject} or {@link JSONArray}
     */
    @SuppressWarnings("unchecked")
    public <R extends JSONAware> R get(String url, Map<String, String> queryParams) {
        return (R) getObject(url, queryParams);
    }

    private Object getObject(String url, Map<String, String> queryParams) {
        String result;
        WebResource wres = getWebResource().path(url);
        wres = addQueryParams(wres, queryParams);
        try {
            result = auth(wres.getRequestBuilder()).get(String.class);
        } catch (UniformInterfaceException e) {
            final int rc = e.getResponse().getStatus();
            if (rc == NO_CONTENT.getStatusCode()) {
                logger.info("API GET request " + url + " failed (rc=" + rc + "): " + "<no content>");
                return null;
            } else {
                String error = "API GET request " + url + " failed (rc=" + rc + "): "
                        + e.getResponse().getEntity(String.class);
                logger.severe(error);
                throw HttpException.forResponse(e.getResponse(), error).orElse(e);
            }
        }
        return JSONValue.parse(result);
    }

    private WebResource getWebResource() {
        return client.resource(contextRoot + context);
    }

    private WebResource addQueryParams(WebResource wres, Map<String, String> queryParams) {
        if (queryParams != null) {
            for (Entry<String, String> e : queryParams.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    wres = wres.queryParam(e.getKey(), e.getValue());
                }
            }
        }
        return wres;
    }

    private Builder auth(Builder builder) {
        if (token != null) {
            return builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public String getContextRoot() {
        return contextRoot;
    }

}