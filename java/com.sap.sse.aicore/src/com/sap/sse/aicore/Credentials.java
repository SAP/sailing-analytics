package com.sap.sse.aicore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

public interface Credentials {

    URL getAiApiUrl();

    String getAppName();

    String getIdentityZoneId();

    String getIdentityZone();

    void authorize(HttpRequest httpGet) throws URISyntaxException, UnsupportedOperationException,
            ClientProtocolException, IOException, ParseException;
    
}
