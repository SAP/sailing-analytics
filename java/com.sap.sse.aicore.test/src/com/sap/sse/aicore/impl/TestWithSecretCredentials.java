package com.sap.sse.aicore.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.ChatSession;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.CredentialsParser;
import com.sap.sse.aicore.Deployment;
import com.sap.sse.common.Util;

@Disabled("Requires system property sap.aicore.credentials to be set and contain a JSON credentials string; for format, see resources/sample_credentials.json")
public class TestWithSecretCredentials {
    private Credentials credentials;
    private AICore aiCore;

    @BeforeEach
    public void setUp() throws MalformedURLException, ParseException {
        final String credentialsJsonString = System.getProperty(AICore.CREDENTIALS_SYSTEM_PROPERTY_NAME);
        credentials = CredentialsParser.create().parse(credentialsJsonString);
        aiCore = AICore.create(credentials);
    }

    @Test
    public void testFetchToken() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException,
            IOException, ParseException {
        final String token = ((CredentialsImpl) credentials).fetchToken();
        assertTrue(Util.hasLength(token));
    }

    @Test
    public void testFetchConfigurations() throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException {
        final JSONObject configurationsJson = aiCore.getJSONResponse(aiCore.getHttpGetRequest("/v2/lm/configurations"));
        assertNotNull(configurationsJson);
    }

    @Test
    public void testFetchDeployments() throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException {
        final JSONObject deploymentsJson = aiCore.getJSONResponse(aiCore.getHttpGetRequest("/v2/lm/deployments"));
        assertNotNull(deploymentsJson);
    }
    
    @Test
    public void testFetchModelNames() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        assertTrue(Util.contains(aiCore.getModelNames(), "gpt-4o-mini"));
    }
    
    @Test
    public void testFindingGpt4oMiniModel() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final Optional<Deployment> gpt4oMini = aiCore.getDeploymentByModelName("gpt-4o-mini");
        assertTrue(gpt4oMini.isPresent());
    }
    
    @Test
    public void testSimpleChatSession() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final ChatSession chatSession = aiCore.createChatSession("gpt-4o-mini").get();
        chatSession.addPrompt("Hello");
        final String response = chatSession.submit();
        assertTrue(response.contains("Hello!"));
    }
    
    @Test
    public void testChatSessionWithMultiplePrompts() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final ChatSession chatSession = aiCore.createChatSession("gpt-4o-mini").get();
        chatSession.addSystemPrompt("You are a sailing coach and expert, have read all books about the subject and have yourself been a very successful sailor in many different boat classes");
        chatSession.addSystemPrompt("We want you to compare two sailors with each other, in the context of a current sailing event and against the background of statistics about the two from previous events.");
        chatSession.addPrompt("The following are the distances in meters that Matt Wearn and Philipp Buhl were leeward of the favored side of the start line in the races of the Olympic Summer Games 2020:\n" +
                              "Series name;Matt Wearn;Philipp Buhl\n" +
                              "Men's Laser Standard - Medal Race;16.314;12.038\n" +
                              "Men's Laser Standard - Race 1;5.273;8.023\n" +
                              "Men's Laser Standard - Race 2;20.235;10.774\n" +
                              "Men's Laser Standard - Race 3;27.897;29.739\n" +
                              "Men's Laser Standard - Race 4;28.725;33.953\n" +
                              "Men's Laser Standard - Race 5;12.908;9.467\n" +
                              "Men's Laser Standard - Race 6;35.151;13.884\n" +
                              "Men's Laser Standard - Race 7;5.862;8.722\n" +
                              "Men's Laser Standard - Race 8;5.781;6.289\n" +
                              "Men's Laser Standard - Race 9;7.647;-0.367\n" +
                              "Men's Laser Standard - Race 10;20.731;27.23\n" +
                              "Who of the two is the better starter and why?");
        final String response = chatSession.submit();
        assertTrue(Util.hasLength(response));
        assertTrue(response.contains("Conclusion"));
        chatSession.addPrompt("Please be more concise");
        final String response2 = chatSession.submit();
        assertTrue(Util.hasLength(response2));
        assertThat("Answer requested to be more concise was longer than the original answer", response2.length(), lessThan(response.length()));
    }
    
    @Test
    public void explainQuantumMechanics() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final ChatSession chatSession = aiCore.createChatSession("gpt-4o-mini").get();
        final String response = chatSession
                .addSystemPrompt("You are a teacher.")
                .addPrompt("Explain quantum mechanics in easy terms!").submit();
        assertTrue(Util.hasLength(response));
        assertTrue(response.contains("atoms"));
    }
}
