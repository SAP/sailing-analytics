package com.sap.sse.aicore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

public interface ChatSession {
    ChatSession addSystemPrompt(String prompt);

    ChatSession addPrompt(String prompt);

    /**
     * Submits the prompt synchronously and waits for the response which is then returned. This may run into API rate limits;
     * you're on your own regarding re-try and back-off.
     */
    String submit() throws UnsupportedOperationException, ClientProtocolException, URISyntaxException, IOException, ParseException;
    
    /**
     * Submits the prompt asynchronously in a background thread, trying to respect API rate limits and re-trying with an
     * exponential back-off strategy when having exceeded the limit.
     * 
     * @param callback
     *            invoked with the response {@link String} when submitting the prompt succeeded
     * @param exceptionHandler
     *            can contain an exception handler; this won't be invoked when an issue with rate limiting is detected
     *            and a back-off strategy is used; unrecoverable exceptions will be passed to the exception handler if
     *            present; otherwise they will simply be logged with level {@link Level#SEVERE SEVERE}.
     */
    void submit(Consumer<String> callback, Optional<Consumer<Exception>> exceptionHandler);

    /**
     * @param frequencyPenalty
     *            Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in
     *            the text so far, decreasing the model's likelihood to repeat the same line verbatim.
     */
    ChatSession setFrequencyPenalty(Double frequencyPenalty);

    Double getFrequencyPenalty();

    /**
     * @param presencePenalty
     *            Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the
     *            text so far, increasing the model's likelihood to talk about new topics.
     */
    ChatSession setPresencePenalty(Double presencePenalty);

    Double getPresencePenalty();

    /**
     * @param maxCompletionTokens
     *            An upper bound for the number of tokens that can be generated for a completion, including visible
     *            output tokens and reasoning tokens.
     */
    ChatSession setMaxCompletionTokens(Integer maxCompletionTokens);

    Integer getMaxCompletionTokens();

    /**
     * @param maxTokens
     *            The maximum number of tokens that can be generated in the chat completion.
     *            <p>
     * 
     *            The total length of input tokens and generated tokens is limited by the model's context length.
     */
    ChatSession setMaxTokens(Integer maxTokens);

    Integer getMaxTokens();

    /**
     * @param top_p
     *            An alternative to sampling with temperature, called nucleus sampling, where the model considers the
     *            results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
     *            probability mass are considered.
     *            <p>
     * 
     *            We generally recommend altering this or temperature but not both.
     */
    ChatSession setTop_p(Double top_p);

    Double getTop_p();

    /**
     * @param temperature
     *            What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the output more
     *            random, while lower values like 0.2 will make it more focused and deterministic.
     *            <p>
     * 
     *            We generally recommend altering this or top_p but not both.
     */
    ChatSession setTemperature(Double temperature);

    Double getTemperature();
}
