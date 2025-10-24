package com.sap.sse.gwt.client.suggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.sap.sse.common.Util;

public abstract class AbstractSuggestOracle<T> extends SuggestOracle {

    public final void setSuggestions(Request request, Callback callback, Iterable<T> suggestionObjects,
            Iterable<String> queryTokens) {
        List<Suggestion> suggestions = new ArrayList<>();
        List<String> normalizedQueryTokens = new ArrayList<>();
        for (String token : queryTokens) {
            normalizedQueryTokens.add(token.toLowerCase());
        }
        int count = 0;
        for (T match : suggestionObjects) {
            suggestions.add(createSuggestion(match, normalizedQueryTokens));
            if (++count >= request.getLimit()) {
                break;
            }
        }
        Response response = new Response(suggestions);
        response.setMoreSuggestionsCount(Util.size(suggestionObjects) - count);
        callback.onSuggestionsReady(request, response);
    }

    protected SimpleSuggestion createSuggestion(T match, Iterable<String> queryTokens) {
        return new SimpleSuggestion(match, queryTokens);
    }

    protected abstract void getSuggestions(Request request, Callback callback, Iterable<String> queryTokens);

    protected abstract String createSuggestionKeyString(T value);

    protected abstract String createSuggestionAdditionalDisplayString(T value);
    
    @Override
    public final void requestDefaultSuggestions(Request request, Callback callback) {
        getSuggestions(request, callback, Collections.<String> emptyList());
    }

    @Override
    public final void requestSuggestions(Request request, Callback callback) {
        String normalizedQuery = request.getQuery().trim();
        if (normalizedQuery == null || normalizedQuery.isEmpty()) {
            requestDefaultSuggestions(request, callback);
        } else {
            Iterable<String> queryTokens = Util.splitAlongWhitespaceRespectingDoubleQuotedPhrases(normalizedQuery);
            getSuggestions(request, callback, queryTokens);
        }
    }

    @Override
    public final boolean isDisplayStringHTML() {
        return true;
    }

    public class SimpleSuggestion implements Suggestion {

        private static final String STRONG_TAG_OPEN = "<strong>", STRONG_TAG_CLOSE = "</strong>";
        private static final String ITALIC_TAG_OPEN = "<em>", ITALIC_TAG_CLOSE = "</em>";
        private final T suggestObject;
        private final Iterable<String> queryTokens;

        public SimpleSuggestion(T suggestObject, Iterable<String> queryTokens) {
            this.suggestObject = suggestObject;
            this.queryTokens = queryTokens;
        }

        @Override
        public String getDisplayString() {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            appendHighlighted(builder, createSuggestionKeyString(suggestObject));
            String additionalString = createSuggestionAdditionalDisplayString(suggestObject);
            if (additionalString != null && !additionalString.isEmpty()) {
                builder.append(SafeHtmlUtils.fromTrustedString(ITALIC_TAG_OPEN));
                builder.append(SafeHtmlUtils.fromSafeConstant(" - "));
                appendHighlighted(builder, additionalString);
                builder.append(SafeHtmlUtils.fromTrustedString(ITALIC_TAG_CLOSE));
            }
            return builder.toSafeHtml().asString();
        }

        private void appendHighlighted(SafeHtmlBuilder builder, String displayString) {
            String normalizedString = displayString.toLowerCase();
            int cursor = 0;
            while (true) {
                int index = displayString.length();
                String matchToken = null;
                for (String token : queryTokens) {
                    if (token != null && !token.isEmpty()) {
                        int matchIndex = normalizedString.indexOf(token, cursor);
                        if (matchIndex >= 0 && matchIndex < index) {
                            index = matchIndex;
                            matchToken = token;
                        }
                    }
                }
                if (matchToken != null) {
                    builder.appendEscaped(displayString.substring(cursor, index));
                    builder.append(SafeHtmlUtils.fromTrustedString(STRONG_TAG_OPEN));
                    builder.appendEscaped(displayString.substring(index, index + matchToken.length()));
                    builder.append(SafeHtmlUtils.fromTrustedString(STRONG_TAG_CLOSE));
                    cursor = index + matchToken.length();
                } else {
                    builder.appendEscaped(displayString.substring(cursor));
                    break;
                }
            }
        }

        @Override
        public String getReplacementString() {
            return createSuggestionKeyString(suggestObject);
        }
        
        public T getSuggestObject() {
            return suggestObject;
        }
    }
  
}
