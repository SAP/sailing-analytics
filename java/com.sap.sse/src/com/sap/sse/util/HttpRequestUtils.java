package com.sap.sse.util;

import javax.servlet.http.HttpServletRequest;

import com.sap.sse.common.Util;

/**
 * A utility class for parsing the request parameters of a HTTP request in a type safe way.
 * 
 * @author Frank Mittag
 */
public abstract class HttpRequestUtils {
    private static final IntParser INT_PARSER = new IntParser();
    private static final LongParser LONG_PARSER = new LongParser();
    private static final FloatParser FLOAT_PARSER = new FloatParser();
    private static final DoubleParser DOUBLE_PARSER = new DoubleParser();
    private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();
    private static final StringParser STRING_PARSER = new StringParser();
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * From an {@link HttpServletRequest} get the client IP address. This is determined in one of two ways:
     * If an {@code X-Forwarded-For} HTTP header is found, its value's first element is returned which
     * represents the IP of the original client (and not that of a proxy in between). If no such header is
     * found or the header is empty, the {@link HttpServletRequest#getRemoteAddr()} is returned.
     */
    public static String getClientIP(HttpServletRequest req) {
        final String xForwardedFor = req.getHeader(X_FORWARDED_FOR_HEADER);
        final String result;
        if (Util.hasLength(xForwardedFor)) {
            final String[] ips = xForwardedFor.split(", *");
            result = ips[0];
        } else {
            result = req.getRemoteAddr();
        }
        return result;
    }
    
    /**
     * @return the value of the "User-Agent" header; may be {@code null} or an empty string
     */
    public static String getUserAgent(HttpServletRequest req) {
        return req.getHeader(USER_AGENT_HEADER);
    }
    
    /**
     * Get an Integer parameter, or <code>null</code> if not present. Throws an exception if it the parameter value
     * isn't a number.
     * 
     * @param name
     *            the name of the parameter
     */
    public static Integer getIntParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final Integer result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = Integer.valueOf(getRequiredIntParameter(req, name));
        }
        return result;
    }

    /**
     * Get an int parameter, with a fallback value. Never throws an exception. Can pass a distinguished value as default
     * to enable checks of whether it was supplied.
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValueue
     *            the default value to use as fallback
     */
    public static int getIntParameter(HttpServletRequest req, String name, int defaultValue) {
        if (req.getParameter(name) == null) {
            return defaultValue;
        }
        try {
            return getRequiredIntParameter(req, name);
        } catch (ParseHttpParameterException ex) {
            return defaultValue;
        }
    }

    /**
     * Get an array of int parameters, return an empty array if not found.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static int[] getIntParameters(HttpServletRequest req, String name) {
        try {
            return getRequiredIntParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            return new int[0];
        }
    }

    /**
     * Get an int parameter, throwing an exception if it isn't found or isn't a number.
     * 
     * @param name
     *            the name of the parameter
     */
    public static int getRequiredIntParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return INT_PARSER.parseInt(name, req.getParameter(name));
    }

    /**
     * Get an array of int parameters, throwing an exception if not found or one is not a number..
     * 
     * @param name
     *            the name of the parameter
     */
    public static int[] getRequiredIntParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return INT_PARSER.parseInts(name, req.getParameterValues(name));
    }

    /**
     * Get a Long parameter, or <code>null</code> if not present. Throws an exception if it the parameter value isn't a
     * number.
     * 
     * @param name
     *            the name of the parameter
     * @return the Long value, or <code>null</code> if not present
     */
    public static Long getLongParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final Long result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = Long.valueOf(getRequiredLongParameter(req, name));
        }
        return result;
    }

    /**
     * Get a long parameter, with a fallback value. Never throws an exception. Can pass a distinguished value as default
     * to enable checks of whether it was supplied.
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValue
     *            the default value to use as fallback
     */
    public static long getLongParameter(HttpServletRequest req, String name, long defaultValue) {
        long result;
        if (req.getParameter(name) == null) {
            result = defaultValue;
        } else {
            try {
                result = getRequiredLongParameter(req, name);
            } catch (ParseHttpParameterException ex) {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * Get an array of long parameters, return an empty array if not found.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static long[] getLongParameters(HttpServletRequest req, String name) {
        long[] result;
        try {
            result = getRequiredLongParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            result = new long[0];
        }
        return result;
    }

    /**
     * Get a long parameter, throwing an exception if it isn't found or isn't a number.
     * 
     * @param name
     *            the name of the parameter
     */
    public static long getRequiredLongParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return LONG_PARSER.parseLong(name, req.getParameter(name));
    }

    /**
     * Get an array of long parameters, throwing an exception if not found or one is not a number.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static long[] getRequiredLongParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return LONG_PARSER.parseLongs(name, req.getParameterValues(name));
    }

    /**
     * Get a Float parameter, or <code>null</code> if not present. Throws an exception if it the parameter value isn't a
     * number.
     * 
     * @param name
     *            the name of the parameter
     * @return the Float value, or <code>null</code> if not present
     */
    public static Float getFloatParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final Float result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = Float.valueOf(getRequiredFloatParameter(req, name));
        }
        return result;
    }

    /**
     * Get a float parameter, with a fallback value. Never throws an exception. Can pass a distinguished value as
     * default to enable checks of whether it was supplied.
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValue
     *            the default value to use as fallback
     */
    public static float getFloatParameter(HttpServletRequest req, String name, float defaultValue) {
        if (req.getParameter(name) == null) {
            return defaultValue;
        }
        try {
            return getRequiredFloatParameter(req, name);
        } catch (ParseHttpParameterException ex) {
            return defaultValue;
        }
    }

    /**
     * Get an array of float parameters, return an empty array if not found.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static float[] getFloatParameters(HttpServletRequest req, String name) {
        try {
            return getRequiredFloatParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            return new float[0];
        }
    }

    /**
     * Get a float parameter, throwing an exception if it isn't found or isn't a number.
     * 
     * @param name
     *            the name of the parameter
     */
    public static float getRequiredFloatParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {

        return FLOAT_PARSER.parseFloat(name, req.getParameter(name));
    }

    /**
     * Get an array of float parameters, throwing an exception if not found or one is not a number.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static float[] getRequiredFloatParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return FLOAT_PARSER.parseFloats(name, req.getParameterValues(name));
    }

    /**
     * Get a Double parameter, or <code>null</code> if not present. Throws an exception if it the parameter value isn't
     * a number.
     * 
     * @param name
     *            the name of the parameter
     * @return the Double value, or <code>null</code> if not present
     */
    public static Double getDoubleParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final Double result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = Double.valueOf(getRequiredDoubleParameter(req, name));
        }
        return result;
    }

    /**
     * Get a double parameter, with a fallback value. Never throws an exception. Can pass a distinguished value as
     * default to enable checks of whether it was supplied.
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValue
     *            the default value to use as fallback
     */
    public static double getDoubleParameter(HttpServletRequest req, String name, double defaultValue) {
        double result;
        if (req.getParameter(name) == null) {
            result = defaultValue;
        } else {
            try {
                result = getRequiredDoubleParameter(req, name);
            } catch (ParseHttpParameterException ex) {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * Get an array of double parameters, return an empty array if not found.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static double[] getDoubleParameters(HttpServletRequest req, String name) {
        try {
            return getRequiredDoubleParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            return new double[0];
        }
    }

    /**
     * Get a double parameter, throwing an exception if it isn't found or isn't a number.
     * 
     * @param name
     *            the name of the parameter
     */
    public static double getRequiredDoubleParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return DOUBLE_PARSER.parseDouble(name, req.getParameter(name));
    }

    /**
     * Get an array of double parameters, throwing an exception if not found or one is not a number.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static double[] getRequiredDoubleParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return DOUBLE_PARSER.parseDoubles(name, req.getParameterValues(name));
    }

    /**
     * Get a Boolean parameter, or <code>null</code> if not present. Throws an exception if it the parameter value isn't
     * a boolean.
     * <p>
     * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats every other non-empty value as false
     * (i.e. parses leniently).
     * 
     * @param name
     *            the name of the parameter
     * @return the Boolean value, or <code>null</code> if not present
     */
    public static Boolean getBooleanParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final Boolean result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = (getRequiredBooleanParameter(req, name) ? Boolean.TRUE : Boolean.FALSE);
        }
        return result;
    }

    /**
     * Get a boolean parameter, with a fallback value. Never throws an exception. Can pass a distinguished value as
     * default to enable checks of whether it was supplied.
     * <p>
     * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats every other non-empty value as false
     * (i.e. parses leniently).
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValue
     *            the default value to use as fallback
     */
    public static boolean getBooleanParameter(HttpServletRequest req, String name, boolean defaultValue) {
        boolean result;
        if (req.getParameter(name) == null) {
            result = defaultValue;
        } else {
            try {
                result = getRequiredBooleanParameter(req, name);
            } catch (ParseHttpParameterException ex) {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * Get an array of boolean parameters, return an empty array if not found.
     * <p>
     * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats every other non-empty value as false
     * (i.e. parses leniently).
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static boolean[] getBooleanParameters(HttpServletRequest req, String name) {
        try {
            return getRequiredBooleanParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            return new boolean[0];
        }
    }

    /**
     * Get a boolean parameter, throwing an exception if it isn't found or isn't a boolean.
     * <p>
     * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats every other non-empty value as false
     * (i.e. parses leniently).
     * 
     * @param name
     *            the name of the parameter
     */
    public static boolean getRequiredBooleanParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return BOOLEAN_PARSER.parseBoolean(name, req.getParameter(name));
    }

    /**
     * Get an array of boolean parameters, throwing an exception if not found or one isn't a boolean.
     * <p>
     * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats every other non-empty value as false
     * (i.e. parses leniently).
     * 
     * @param name
     *            the name of the parameter
     */
    public static boolean[] getRequiredBooleanParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return BOOLEAN_PARSER.parseBooleans(name, req.getParameterValues(name));
    }

    /**
     * Get a String parameter, or <code>null</code> if not present.
     * 
     * @param name
     *            the name of the parameter
     * @return the String value, or <code>null</code> if not present
     * @throws ParseHttpParameterException
     *             a subclass of ServletException, so it doesn't need to be caught
     */
    public static String getStringParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        final String result;
        if (req.getParameter(name) == null) {
            result = null;
        } else {
            result = getRequiredStringParameter(req, name);
        }
        return result;
    }

    /**
     * Get a String parameter, with a fallback value. Never throws an exception. Can pass a distinguished value to
     * default to enable checks of whether it was supplied.
     * 
     * @param name
     *            the name of the parameter
     * @param defaultValue
     *            the default value to use as fallback
     */
    public static String getStringParameter(HttpServletRequest req, String name, String defaultValue) {
        String val = req.getParameter(name);
        return (val != null ? val : defaultValue);
    }

    /**
     * Get an array of String parameters, return an empty array if not found.
     * 
     * @param name
     *            the name of the parameter with multiple possible values
     */
    public static String[] getStringParameters(HttpServletRequest req, String name) {
        String[] result;
        try {
            result = getRequiredStringParameters(req, name);
        } catch (ParseHttpParameterException ex) {
            result = new String[0];
        }
        return result;
    }

    /**
     * Get a String parameter, throwing an exception if it isn't found.
     * 
     * @param name
     *            the name of the parameter
     */
    public static String getRequiredStringParameter(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return STRING_PARSER.validateRequiredString(name, req.getParameter(name));
    }

    /**
     * Get an array of String parameters, throwing an exception if not found.
     * 
     * @param name
     *            the name of the parameter
     */
    public static String[] getRequiredStringParameters(HttpServletRequest req, String name) throws ParseHttpParameterException {
        return STRING_PARSER.validateRequiredStrings(name, req.getParameterValues(name));
    }

    private abstract static class ParameterParser {
        protected final Object parse(String name, String parameter) throws ParseHttpParameterException {
            validateRequiredParameter(name, parameter);
            try {
                return doParse(parameter);
            } catch (NumberFormatException ex) {
                throw new ParseHttpParameterException(name, "Required " + getType() + " parameter '" + name
                        + "' with value of '" + parameter + "' is not a valid number.", ex);
            }
        }

        protected final void validateRequiredParameter(String name, Object parameter) throws ParseHttpParameterException {
            if (parameter == null) {
                throw new ParseHttpParameterException(name, "Required " + getType() + " parameter '" + name + "' is null.");
            }
        }

        protected abstract String getType();

        protected abstract Object doParse(String parameter) throws NumberFormatException;
    }

    private static class IntParser extends ParameterParser {
        protected String getType() {
            return "int";
        }

        protected Object doParse(String s) throws NumberFormatException {
            return Integer.valueOf(s);
        }

        public int parseInt(String name, String parameter) throws ParseHttpParameterException {
            return ((Number) parse(name, parameter)).intValue();
        }

        public int[] parseInts(String name, String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            int[] parameters = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseInt(name, values[i]);
            }
            return parameters;
        }
    }

    private static class LongParser extends ParameterParser {
        protected String getType() {
            return "long";
        }

        protected Object doParse(String parameter) throws NumberFormatException {
            return Long.valueOf(parameter);
        }

        public long parseLong(String name, String parameter) throws ParseHttpParameterException {
            return ((Number) parse(name, parameter)).longValue();
        }

        public long[] parseLongs(String name, String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            long[] parameters = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseLong(name, values[i]);
            }
            return parameters;
        }
    }

    private static class FloatParser extends ParameterParser {
        protected String getType() {
            return "float";
        }

        protected Object doParse(String parameter) throws NumberFormatException {
            return Float.valueOf(parameter);
        }

        public float parseFloat(String name, String parameter) throws ParseHttpParameterException {
            return ((Number) parse(name, parameter)).floatValue();
        }

        public float[] parseFloats(String name, String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            float[] parameters = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseFloat(name, values[i]);
            }
            return parameters;
        }
    }

    private static class DoubleParser extends ParameterParser {
        protected String getType() {
            return "double";
        }

        protected Object doParse(String parameter) throws NumberFormatException {
            return Double.valueOf(parameter);
        }

        public double parseDouble(String name, String parameter) throws ParseHttpParameterException {
            return ((Number) parse(name, parameter)).doubleValue();
        }

        public double[] parseDoubles(String name,String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            double[] parameters = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseDouble(name, values[i]);
            }
            return parameters;
        }
    }

    private static class BooleanParser extends ParameterParser {

        protected String getType() {
            return "boolean";
        }

        protected Object doParse(String parameter) throws NumberFormatException {
            return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on")
                    || parameter.equalsIgnoreCase("yes") || parameter.equals("1") ? Boolean.TRUE : Boolean.FALSE);
        }

        public boolean parseBoolean(String name, String parameter) throws ParseHttpParameterException {
            return ((Boolean) parse(name, parameter)).booleanValue();
        }

        public boolean[] parseBooleans(String name, String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            boolean[] parameters = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseBoolean(name, values[i]);
            }
            return parameters;
        }
    }

    private static class StringParser extends ParameterParser {
        protected String getType() {
            return "string";
        }

        protected Object doParse(String parameter) throws NumberFormatException {
            return parameter;
        }

        public String validateRequiredString(String name, String value) throws ParseHttpParameterException {
            validateRequiredParameter(name, value);
            return value;
        }

        public String[] validateRequiredStrings(String name, String[] values) throws ParseHttpParameterException {
            validateRequiredParameter(name, values);
            String[] parameters = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = validateRequiredString(name, values[i]);
            }
            return parameters;
        }
    }
}
