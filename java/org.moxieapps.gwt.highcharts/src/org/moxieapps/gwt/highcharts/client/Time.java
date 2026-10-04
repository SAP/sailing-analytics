package org.moxieapps.gwt.highcharts.client;

/**
 * A configurable class that can be used to globally configure the time options for Highcharts.
 * These options should be configured prior to the initialization of any chart instance.
 * <p/>
 * Note that as of Highcharts 6 the time related options were moved out of the {@link Global}
 * options object into a dedicated "time" options object, which is what this class represents.
 * In particular, setting {@link #setUseUTC(boolean)} to {@code false} on the {@link Global}
 * options no longer has any effect; the equivalent option now lives here.
 * <p/>
 * Example usage:
 * <code><pre>
 *   Highcharts.setOptions(
 *     new Highcharts.Options().setTime(
 *         new Time()
 *           .setUseUTC(false)
 *   ));
 * </pre></code>
 *
 * @since 1.4.0
 */
public class Time extends Configurable<Time> {

    /**
     * Convenience method for setting the 'useUTC' time option.  Equivalent to:
     * <pre><code>
     *     time.setOption("useUTC", false);
     * </code></pre>
     * Whether to use UTC time for axis scaling, tickmark placement and time display in
     * Highcharts.dateFormat. Advantages of using UTC is that the time displays equally
     * regardless of the user agent's time zone settings. Local time can be used when the
     * data is loaded in real time or when correct Daylight Saving Time transitions are
     * required. As of Highcharts 12 this option is deprecated in favor of
     * {@link #setTimezone(String)}: setting {@code useUTC} to {@code true} is equivalent to
     * setting {@code timezone} to {@code "UTC"}, and setting {@code useUTC} to {@code false}
     * is equivalent to leaving {@code timezone} unset ({@code undefined}), which renders in
     * the browser's local time zone. Defaults to true.
     *
     * @param useUTC Flag to indicate whether time displays using UTC or not
     * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Time} instance for convenient method chaining.
     */
    public Time setUseUTC(boolean useUTC) {
        return this.setOption("useUTC", useUTC);
    }

    /**
     * Convenience method for setting the 'timezone' time option.  Equivalent to:
     * <pre><code>
     *     time.setOption("timezone", "Europe/Berlin");
     * </code></pre>
     * Requires Highcharts 11 or later. The timezone to use for displaying time, given as an
     * IANA time zone name such as {@code "Europe/Berlin"}; supported names rely on the
     * browser's {@code Intl.DateTimeFormat} implementation. When set, this option takes
     * precedence over {@link #setUseUTC(boolean)} and {@link #setTimezoneOffset(int)}. Note
     * that as of Highcharts 12 the special value {@code "local"} accepted by Highcharts 11 is
     * no longer valid; to render in the browser's local time zone leave this option unset (or
     * call {@link #setUseUTC(boolean)} with {@code false}, which maps to {@code undefined}).
     *
     * @param timezone The IANA time zone name
     * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Time} instance for convenient method chaining.
     */
    public Time setTimezone(String timezone) {
        return this.setOption("timezone", timezone);
    }

    /**
     * Convenience method for setting the 'timezoneOffset' time option.  Equivalent to:
     * <pre><code>
     *     time.setOption("timezoneOffset", -120);
     * </code></pre>
     * A fixed offset from UTC, in minutes, to apply to all displayed times. The sign follows the
     * JavaScript {@code Date.getTimezoneOffset()} convention (positive west of UTC), so a time
     * zone of UTC+2 is expressed as {@code -120}. Ignored when {@link #setTimezone(String)} is set.
     * Defaults to 0.
     *
     * @param timezoneOffset The offset from UTC in minutes, west-positive
     * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Time} instance for convenient method chaining.
     */
    public Time setTimezoneOffset(int timezoneOffset) {
        return this.setOption("timezoneOffset", timezoneOffset);
    }

}
