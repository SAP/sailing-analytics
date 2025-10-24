package com.sap.sse.aicore.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sse.aicore.AICore;
import com.sap.sse.aicore.ChatSession;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.Deployment;
import com.sap.sse.common.Util;

public class ChatSessionImpl implements ChatSession {
    private static final Logger logger = Logger.getLogger(ChatSessionImpl.class.getName());

    private static final String TEMPERATURE = "temperature";
    private static final String TOP_P = "top_p";
    private static final String MAX_TOKENS = "max_tokens";
    private static final String MAX_COMPLETION_TOKENS = "max_completion_tokens";
    private static final String PRESENCE_PENALTY = "presence_penalty";
    private static final String FREQUENCY_PENALTY = "frequency_penalty";
    private static final String CONTENT = "content";
    private static final String MESSAGE = "message";
    private static final String CHOICES = "choices";
    private static final String USER_PROMPT = "user";
    private static final String SYSTEM_PROPMPT = "system";
    private static final String MESSAGES = "messages";
    private static final String API_VERSION = "2025-04-01-preview";
    private static final String CHAT_PATH_TEMPLATE = "/v2/inference/deployments/%s/chat/completions?api-version="+API_VERSION;

    private final List<String> systemPrompts;
    private final List<String> userPrompts;
    private final AICore aiCore;
    private final Deployment deployment;
    
    /**
     * What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more random, while
     * lower values like 0.2 will make it more focused and deterministic.<p>
     * 
     * We generally recommend altering this or top_p but not both.
     */
    private Double temperature;
    
    /**
     * An alternative to sampling with temperature, called nucleus sampling, where the model considers the results of
     * the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are
     * considered.<p>
     * 
     * We generally recommend altering this or temperature but not both.
     */
    private Double top_p;
    
    /**
     * The maximum number of tokens that can be generated in the chat completion.<p>
     * 
     * The total length of input tokens and generated tokens is limited by the model's context length.
     */
    private Integer maxTokens;
    
    /**
     * An upper bound for the number of tokens that can be generated for a completion, including visible output tokens
     * and reasoning tokens.
     */
    private Integer maxCompletionTokens;
    
    /**
     * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far,
     * increasing the model's likelihood to talk about new topics.
     */
    private Double presencePenalty;
    
    /**
     * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so
     * far, decreasing the model's likelihood to repeat the same line verbatim.
     */
    private Double frequencyPenalty;
    
    public ChatSessionImpl(final AICore aiCore, final Deployment deployment) {
        this.aiCore = aiCore;
        this.deployment = deployment;
        this.systemPrompts = new ArrayList<>();
        this.userPrompts = new ArrayList<>();
    }

    public ChatSessionImpl(final Credentials credentials, final String modelName) throws UnsupportedOperationException,
            ClientProtocolException, URISyntaxException, IOException, ParseException {
        this(AICore.create(credentials), modelName);
    }
    
    public ChatSessionImpl(final AICore aiCore, final String modelName) throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        this(aiCore, aiCore.getDeploymentByModelName(modelName).get());
    }
    
    @Override
    public Double getTemperature() {
        return temperature;
    }

    @Override
    public ChatSession setTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    @Override
    public Double getTop_p() {
        return top_p;
    }

    @Override
    public ChatSession setTop_p(Double top_p) {
        this.top_p = top_p;
        return this;
    }

    @Override
    public Integer getMaxTokens() {
        return maxTokens;
    }

    @Override
    public ChatSession setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    @Override
    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    @Override
    public ChatSession setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
        return this;
    }

    @Override
    public Double getPresencePenalty() {
        return presencePenalty;
    }

    @Override
    public ChatSession setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }

    @Override
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    @Override
    public ChatSession setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }

    @Override
    public ChatSession addSystemPrompt(String prompt) {
        systemPrompts.add(prompt);
        return this;
    }

    @Override
    public ChatSession addPrompt(String prompt) {
        userPrompts.add(prompt);
        return this;
    }
    
    private String getChatPath() {
        return String.format(CHAT_PATH_TEMPLATE, deployment.getId());
    }

    private void addPromptMessages(final Iterable<String> prompts, final String role, JSONArray messagesToAddTo) {
        for (final String prompt : prompts) {
            final JSONObject message = new JSONObject();
            message.put("role", role);
            message.put(CONTENT, prompt);
            messagesToAddTo.add(message);
        }
    }
    
    @Override
    public void submit(Consumer<String> callback, Optional<Consumer<Exception>> exceptionHandler) {
        try {
            final HttpPost postRequest = createAndParameterizePostRequest();
            aiCore.getJSONResponse(postRequest, chatResponse->{
                try {
                    callback.accept(getChoicesFromResponse(chatResponse));
                } catch (Exception e) {
                    forwardToExceptionHandlerOrLog(exceptionHandler, e);
                }
            }, exceptionHandler);
        } catch (Exception e) {
            forwardToExceptionHandlerOrLog(exceptionHandler, e);
        }
    }

    private Object forwardToExceptionHandlerOrLog(Optional<Consumer<Exception>> exceptionHandler, Exception e) {
        return exceptionHandler.map(handler->{ handler.accept(e); return null; })
            .orElseGet(()->{ logger.log(Level.SEVERE, "Exception trying to submit prompt", e); return null; });
    }

    @Override
    public String submit() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException {
        final HttpPost postRequest = createAndParameterizePostRequest();
        final JSONObject chatResponse = aiCore.getJSONResponse(postRequest);
        return getChoicesFromResponse(chatResponse);
    }

    private String getChoicesFromResponse(final JSONObject chatResponse) {
        final List<String> results = new ArrayList<>();
        for (final Object choice : ((JSONArray) chatResponse.get(CHOICES))) {
            final JSONObject choiseJson = (JSONObject) choice;
            final JSONObject message = (JSONObject) choiseJson.get(MESSAGE);
            final String content = message.get(CONTENT).toString();
            results.add(content);
        }
        return Util.joinStrings("\n", results);
    }

    private HttpPost createAndParameterizePostRequest() throws UnsupportedOperationException, ClientProtocolException,
            URISyntaxException, IOException, ParseException, UnsupportedCharsetException {
        final HttpPost postRequest = aiCore.getHttpPostRequest(getChatPath());
        final JSONObject toSubmit = new JSONObject();
        final JSONArray messages = new JSONArray();
        toSubmit.put(MESSAGES, messages);
        addPromptMessages(systemPrompts, SYSTEM_PROPMPT, messages);
        addPromptMessages(userPrompts, USER_PROMPT, messages);
        if (getFrequencyPenalty() != null) {
            toSubmit.put(FREQUENCY_PENALTY, getFrequencyPenalty());
        }
        if (getMaxCompletionTokens() != null) {
            toSubmit.put(MAX_COMPLETION_TOKENS, getMaxCompletionTokens());
        }
        if (getMaxTokens() != null) {
            toSubmit.put(MAX_TOKENS, getMaxTokens());
        }
        if (getTemperature() != null) {
            toSubmit.put(TEMPERATURE, getTemperature());
        }
        if (getTop_p() != null) {
            toSubmit.put(TOP_P, getTop_p());
        }
        if (getPresencePenalty() != null) {
            toSubmit.put(PRESENCE_PENALTY, getPresencePenalty());
        }
        postRequest.setEntity(new StringEntity(toSubmit.toJSONString(), ContentType.APPLICATION_JSON));
        return postRequest;
    }
}
