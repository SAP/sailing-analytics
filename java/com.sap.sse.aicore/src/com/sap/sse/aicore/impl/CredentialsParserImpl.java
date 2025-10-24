package com.sap.sse.aicore.impl;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.CredentialsParser;
import com.sap.sse.common.Util.Pair;

public class CredentialsParserImpl implements CredentialsParser {
    private final static String CLIENT_ID = "clientid";
    private final static String CLIENT_SECRET = "clientsecret";
    private final static String URL = "url";
    private final static String IDENTITY_ZONE = "identityzone";
    private final static String IDENTITY_ZONE_ID = "identityzoneid";
    private final static String APP_NAME = "appname";
    private final static String SERVICE_URLS = "serviceurls";
    private final static String AI_API_URL = "AI_API_URL";
    
    @Override
    public Credentials parse(Reader r) throws MalformedURLException, IOException, ParseException  {
        final JSONParser parser = new JSONParser();
        return parse((JSONObject) parser.parse(r));
    }

    @Override
    public Credentials parse(CharSequence s) throws MalformedURLException, ParseException{
        final JSONParser parser = new JSONParser();
        return parse((JSONObject) parser.parse(s.toString()));
    }

    private Credentials parse(JSONObject o) throws MalformedURLException {
        final String clientId = (String) o.get(CLIENT_ID);
        final String clientSecret = (String) o.get(CLIENT_SECRET);
        final String url = (String) o.get(URL);
        final String identityZone = (String) o.get(IDENTITY_ZONE);
        final String identityZoneId = (String) o.get(IDENTITY_ZONE_ID);
        final String appName = (String) o.get(APP_NAME);
        final JSONObject serviceURLs = (JSONObject) o.get(SERVICE_URLS);
        final String aiApiUrl = (String) serviceURLs.get(AI_API_URL);
        return new CredentialsImpl(clientId, clientSecret, url, identityZone, identityZoneId, appName, aiApiUrl);
    }

    @Override
    public Pair<String, String> getAsEncodedString(Credentials credentials) {
        final String clientId = ((CredentialsImpl) credentials).getClientId();
        final String clientSecret = ((CredentialsImpl) credentials).getClientSecret();
        final String url = ((CredentialsImpl) credentials).getXsuaaUrl().toString();
        final String identityZone = credentials.getIdentityZone();
        final String identityZoneId = credentials.getIdentityZoneId();
        final String appName = credentials.getAppName();
        final String aiApiUrl = credentials.getAiApiUrl().toString();
        final JSONObject jsonCredentials = new JSONObject();
        jsonCredentials.put(CLIENT_ID, clientId);
        jsonCredentials.put(CLIENT_SECRET, clientSecret);
        jsonCredentials.put(URL, url);
        jsonCredentials.put(IDENTITY_ZONE, identityZone);
        jsonCredentials.put(IDENTITY_ZONE_ID, identityZoneId);
        jsonCredentials.put(APP_NAME, appName);
        final JSONObject serviceURLs = new JSONObject();
        jsonCredentials.put(SERVICE_URLS, serviceURLs);
        serviceURLs.put(AI_API_URL, aiApiUrl);
        final String salt = createRandomAESKeyLength16();
        SecretKeySpec secretKey = new SecretKeySpec(salt.getBytes(), "AES"); // 16-byte key for AES
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(jsonCredentials.toJSONString().getBytes());
            return new Pair<>(Base64.getEncoder().encodeToString(encryptedBytes), salt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createRandomAESKeyLength16() {
        final Random random = new Random();
        final int numberOfCharacters = 16;
        final char[] chars = new char[numberOfCharacters];
        for (int i=0; i<numberOfCharacters; i++) {
            chars[i] = (char) (((int) 'A') + random.nextInt((int) 'z' - (int) 'A'));
        }
        return new String(chars);
    }

    @Override
    public Credentials parseFromEncoded(CharSequence encoded, String Key) {
        final byte[] decodedBytes = Base64.getDecoder().decode(encoded.toString().getBytes());
        final SecretKeySpec secretKey = new SecretKeySpec(Key.getBytes(), "AES"); // 16-byte key for AES
        try {
            final Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            final byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            final String decryptedString = new String(decryptedBytes);
            return parse(decryptedString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
