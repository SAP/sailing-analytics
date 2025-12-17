package com.sap.sse.i18n.impl;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sse.i18n.ResourceBundleStringMessages;

public class ResourceBundleStringMessagesImpl implements ResourceBundleStringMessages {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private final String resourceBaseName;
    private final ClassLoader resourceClassLoader;
    private String encoding;
    private ResourceBundle result;
    
    public ResourceBundleStringMessagesImpl(String resourceBaseName, ClassLoader resourceClassLoader, String encoding) {
        this.resourceBaseName = resourceBaseName;
        this.resourceClassLoader = resourceClassLoader;
        this.encoding = encoding;
    }

    public ResourceBundleStringMessagesImpl(String resourceBaseName, ClassLoader resourceClassLoader) {
        this.resourceBaseName = resourceBaseName;
        this.resourceClassLoader = resourceClassLoader;
        this.encoding = StandardCharsets.ISO_8859_1.name();
    }
    
    @Override
    public String get(Locale locale, String messageKey) {
        return get(locale, messageKey, new String[0]);
    }
    
    @Override
    public String get(Locale locale, String messageKey, String... parameters) {
        final ResourceBundle bundle = getResourceBundle(locale) == null ? getResourceBundle(DEFAULT_LOCALE)
                : getResourceBundle(locale);
        String message;
        try {
            message = bundle.getString(messageKey);
        } catch (MissingResourceException e) {
            message = getResourceBundle(DEFAULT_LOCALE).getString(messageKey);
        }
        return get(message, parameters);
    }

    /**
     * package-protected to allow access by test fragment
     */
    String get(final String message, String... parameters) {
        final StringBuilder result = new StringBuilder();
        boolean withinQuotedArea = false;
        for (int i = 0; i < message.length(); i++) {
            if (isSingleQuote(message, i) || (withinQuotedArea && message.charAt(i) == '\'')) {
                withinQuotedArea = !withinQuotedArea;
            } else if (isDoubleQuote(message, i)) {
                result.append('\''); // an escaped single quote
                i++; // skip the second one
            } else {
                if (withinQuotedArea) {
                    result.append(message.charAt(i));
                } else {
                    final int paramNumber = isParameterPlaceholder(message, i);
                    if (paramNumber != -1) {
                        result.append(parameters[paramNumber]);
                        i += ("" + paramNumber).length() + 1; // skip the number plus one curly brace
                    } else {
                        result.append(message.charAt(i));
                    }
                }
            }
        }
        return result.toString();
    }

    private boolean isDoubleQuote(String message, int i) {
        return i < message.length() - 1 && message.charAt(i) == '\'' && message.charAt(i + 1) == '\'';
    }

    private static final Pattern placeholderMatcher = Pattern.compile("\\{([0-9]+)\\}.*", Pattern.DOTALL);

    /**
     * @return -1 if there is no placeholder starting at character {@code i} in {@code message}, or the number of the
     *         parameter represented by the placeholder, such as {@code 4} for the placeholder
     * 
     *         <pre>
     *         { 4 }
     *         </pre>
     * 
     *         .
     */
    private int isParameterPlaceholder(String message, int i) {
        final Matcher matcher = placeholderMatcher.matcher(message.substring(i));
        final int result;
        if (matcher.matches()) {
            result = Integer.valueOf(matcher.group(1));
        } else {
            result = -1;
        }
        return result;
    }

    private boolean isSingleQuote(String message, int i) {
        return i < message.length() && message.charAt(i) == '\''
                && (i == message.length() - 1 || message.charAt(i + 1) != '\'');
    }

    private ResourceBundle getResourceBundle(Locale locale) {
        final Control controller = Util.createControl(encoding);
        try {
            if (resourceClassLoader != null) {
                result = ResourceBundle.getBundle(resourceBaseName, locale, resourceClassLoader, controller);
            } else {
                result = ResourceBundle.getBundle(resourceBaseName, locale, controller);
            }
        } catch (MissingResourceException e) {
            // try again with default locale
            return getResourceBundle(Locale.getDefault());
        }
        return result;
    }

    @Override
    public String getResourceBaseName() {
        return resourceBaseName;
    }

}
