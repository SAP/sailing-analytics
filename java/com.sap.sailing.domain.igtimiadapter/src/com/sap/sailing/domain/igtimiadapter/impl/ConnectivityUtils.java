package com.sap.sailing.domain.igtimiadapter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.util.HttpUrlConnectionHelper;

public class ConnectivityUtils {
    public static JSONObject getJsonFromResponse(HttpResponse response) throws IllegalStateException, IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        final Header contentEncoding = response.getEntity().getContentEncoding();
        final Reader reader;
        if (contentEncoding == null) {
            reader = new InputStreamReader(response.getEntity().getContent(), HttpUrlConnectionHelper.getCharsetFromHttpEntity(response.getEntity(), "UTF-8"));
        } else {
            reader = new InputStreamReader(response.getEntity().getContent(), contentEncoding.getValue());
        }
        JSONObject json = (JSONObject) jsonParser.parse(reader);
        reader.close();
        return json;
    }
    
    public static String getContent(HttpResponse response) throws IOException {
        StringBuilder result = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                HttpUrlConnectionHelper.getCharsetFromHttpEntity(response.getEntity(), "UTF-8")));
        while ((line=reader.readLine()) != null) {
            result.append(line);
            result.append('\n');
        }
        return result.toString();
    }

    public static HttpResponse postForm(String baseUrl, final String action, final Map<String, String> inputFieldsToSubmit, CloseableHttpClient client, String referer)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {
        HttpPost post = new HttpPost(baseUrl+action);
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        for (Entry<String, String> nameValue : inputFieldsToSubmit.entrySet()) {
            urlParameters.add(new BasicNameValuePair(nameValue.getKey(), nameValue.getValue()));
        }
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        post.setHeader("Origin", baseUrl);
        post.setHeader("Referer", referer);
        post.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36");
        HttpResponse responseForSignIn = client.execute(post);
        return responseForSignIn;
    }

}
