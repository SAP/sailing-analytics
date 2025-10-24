package com.sap.sse.aicore.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.Credentials;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class CredentialsImpl implements Credentials {
    private final static String CLIENT_CREDENTIALS_PATH = "/oauth/token?grant_type=client_credentials";
    private final static String GRANT_TYPE_NAME = "grant_type";
    private final static String GRANT_TYPE_VALUE = "client_credentials";
    private final static String CLIENT_ID = "client_id";
    private final static String CLIENT_SECRET = "client_secret";
    private final static String ACCESS_TOKEN = "access_token";
    private final static String AI_RESOURCE_GROUP_HEADER_NAME = "AI-Resource-Group";
    private final static String AI_DEFAULT_RESOURCE_GROUP = "default";
    
    private final String clientId;
    private final String clientSecret;
    private final URL xsuaaUrl;
    private final String identityZone;
    private final String identityZoneId;
    private final String appName;
    private final URL aiApiUrl;
    
    /**
     * Starts out as {@code null} after object construction. Will be lazy-initialized when used through the
     * {@link #getToken} method.
     */
    private String token;

    public CredentialsImpl(String clientId, String clientSecret, String xsuaaUrl, String identityZone, String identityZoneId,
            String appName, String aiApiUrl) throws MalformedURLException {
        super();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.xsuaaUrl = new URL(xsuaaUrl);
        this.identityZone = identityZone;
        this.identityZoneId = identityZoneId;
        this.appName = appName;
        this.aiApiUrl = new URL(aiApiUrl);
    }
    
    @Override
    public void authorize(final HttpRequest httpGet) throws URISyntaxException, UnsupportedOperationException,
            ClientProtocolException, IOException, ParseException {
        httpGet.addHeader(AI_RESOURCE_GROUP_HEADER_NAME, AI_DEFAULT_RESOURCE_GROUP);
        httpGet.addHeader("Authorization", "Bearer "+getToken());
    }
    
    @Override
    public String getIdentityZone() {
        return identityZone;
    }

    @Override
    public String getIdentityZoneId() {
        return identityZoneId;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public URL getAiApiUrl() {
        return aiApiUrl;
    }

    String getToken() throws URISyntaxException, UnsupportedOperationException, ClientProtocolException, IOException, ParseException {
        if (token == null) {
            token = fetchToken();
        }
        return token;
    }

    String getClientId() {
        return clientId;
    }

    String getClientSecret() {
        return clientSecret;
    }

    URL getXsuaaUrl() {
        return xsuaaUrl;
    }

    String fetchToken() throws URISyntaxException, UnsupportedOperationException, ClientProtocolException, IOException, ParseException {
        final HttpPost postRequest = new HttpPost(new URI(xsuaaUrl.toString() + CLIENT_CREDENTIALS_PATH));
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        final List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(GRANT_TYPE_NAME, GRANT_TYPE_VALUE));
        params.add(new BasicNameValuePair(CLIENT_ID, clientId));
        params.add(new BasicNameValuePair(CLIENT_SECRET, clientSecret));
        postRequest.setEntity(new UrlEncodedFormEntity(params));
        final HttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes())
                .build();
        final JSONParser jsonParser = new JSONParser();
        final HttpResponse response = client.execute(postRequest);
        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new IOException("Error obtaining client token: "+response.getStatusLine().getReasonPhrase()+" ("+response.getStatusLine().getStatusCode()+")");
        }
        final JSONObject tokenJson = (JSONObject) jsonParser.parse(new InputStreamReader(response.getEntity().getContent()));
        return (String) tokenJson.get(ACCESS_TOKEN);
    }
}
