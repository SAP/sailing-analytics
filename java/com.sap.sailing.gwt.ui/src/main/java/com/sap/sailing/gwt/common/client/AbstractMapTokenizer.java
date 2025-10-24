package com.sap.sailing.gwt.common.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sse.common.Util;

public abstract class AbstractMapTokenizer<P extends Place> implements PlaceTokenizer<P> {

    private static final Logger logger = Logger.getLogger(AbstractMapTokenizer.class.getName());

    @Override
    public P getPlace(String token) {
        /*
         * See bug 6088; the token delivered here as parameter has undergone encoding/decoding using URL.encodeURI; this
         * leaves encoded characters with special meaning in the URL untouched, such as '/', '&' or '?', meaning that
         * their encoding such as %2F for the '/' character will remain unchanged in the token and will not be replaced
         * by '/'. Other characters, such as the '%' character, encoded as '%25', will be decoded.
         * 
         * But we do need to be able to encode special characters in tokens, e.g., for regatta names which may contain
         * any UTF character. The problem, though, is that what we receive here as the "token" parameter is already a
         * "projection" that cannot be inverted to the original string. For example, when we see "%2F" as the "token"
         * value, we cannot tell whether the original URL hash/fragment was "%252F" or just "%2F" as the latter would
         * have been returned unchanged.
         * 
         * Alternatively, we could look at Window.Location.getHash() and silently assume that that would be what was
         * decoded and then passed to this method; interestingly, the call to here from SwitchingEntryPoint.onModuleLoad()
         * passes an non-decoded hash/fragment taken straight from Window.Location.getHash().
         * 
         * Yet alternatively, we could find an encoding of values in tokens that is guaranteed to never produce the
         * offending characters such as '%', '/' or the like. As an extreme case, we could use a Base64 encoding
         * of all values and then run a Base64 decoding to obtain the original token again.
         */
        return getPlaceFromParameters(toParameterMap(token));
    }

    protected abstract P getPlaceFromParameters(Map<String, Set<String>> parameters);

    protected abstract Map<String, Set<String>> getParameters(P place);

    @Override
    public String getToken(P place) {
        return toToken(getParameters(place));
    }

    private String toToken(Map<String, Set<String>> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : parameters.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            for (String val : entry.getValue()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(entry.getKey() + "=" + val); // see bug6088; values must contain only URL-compliant characters, so any encoding must be done by the caller
            }
        }
        return stringBuilder.toString();
    }

    private Map<String, Set<String>> toParameterMap(String placeParametersAsToken) {
        final Map<String, Set<String>> result;
        final List<String> list = Arrays.asList(placeParametersAsToken.split("&"));
        if (list == null || list.size() < 1) {
            logger.warning("Token empty, no-op");
            result = Collections.emptyMap();
        } else {
            result = new HashMap<>();
            for (String listItem : list) {
                final int indexOfEquals = listItem.indexOf('='); // don't use "split" because value may contain '=' characters
                final String key;
                final String value;
                if (indexOfEquals < 0) {
                    key = listItem;
                    value = null;
                } else {
                    key = listItem.substring(0, indexOfEquals);
                    value = listItem.substring(indexOfEquals+1);
                }
                Util.addToValueSet(result, key, value); // see bug6088; values are expected to travel safely through URLs; encoding is the caller's responsibility
            }
        }
        return result;
    }
}
