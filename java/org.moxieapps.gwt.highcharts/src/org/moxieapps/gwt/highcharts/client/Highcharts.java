package org.moxieapps.gwt.highcharts.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A utility class that can be used to globally configure options for Highcharts.
 * These options should be configured prior to the initialization of any chart instance.
 * <p/>
 * Example usage:
 * <code><pre>
 *   Highcharts.setOptions(
 *     new Highcharts.Options().setGlobal(
 *         new Global()
 *           .setUseUTC(false)
 *   ));
 * </pre></code>
 *
 * @author myersj@gmail.com (Jeff Myers)
 * @since 1.4.0
 */
public class Highcharts {

    /**
     * A configurable class that can be used to globally configure options for Highcharts.
     * These options should be configured prior to the initialization of any chart instance.
     * <p/>
     * Example usage:
     * <code><pre>
     *   Highcharts.setOptions(
     *     new Highcharts.Options().setGlobal(
     *         new Global()
     *           .setUseUTC(false)
     *   ));
     * </pre></code>
     *
     * @author myersj@gmail.com (Jeff Myers)
     * @since 1.4.0
     */
    public static class Options extends Configurable<Options> {

        /**
         * Set the global options to use on all Highchart instances.
         *
         * @param global the global configuration options
         * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Highcharts.Options} instance for convenient method chaining.
         */
        public Options setGlobal(Global global) {
            return this.setOption("/global", global);
        }

        /**
         * Set the time options to use on all Highchart instances. As of Highcharts 6 the time
         * related options were moved out of the {@link Global} options into a dedicated "time"
         * options object; use this method rather than {@link #setGlobal(Global)} to configure
         * time zone handling on current Highcharts versions.
         *
         * @param time the time configuration options
         * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Highcharts.Options} instance for convenient method chaining.
         */
        public Options setTime(Time time) {
            return this.setOption("/time", time);
        }

        /**
         * Set the language options to use on all Highchart instances.
         *
         * @param lang the language configuration options
         * @return A reference to this {@link org.moxieapps.gwt.highcharts.client.Highcharts.Options} instance for convenient method chaining.
         */
        public Options setLang(Lang lang) {
            return this.setOption("/lang", lang);
        }
    }

    /**
     * Sets the options globally for all charts created after this has been called.
     *
     * @param options the global options to use
     */
    public static void setOptions(Options options) {
        nativeSetOptions(options.getOptions().getJavaScriptObject());
    }

    private native static JavaScriptObject nativeSetOptions(JavaScriptObject options) /*-{
        $wnd.Highcharts.setOptions(options);
    }-*/;

    /**
     * Private constructor to prevent instantiation.
     */
    private Highcharts() {
        // Do nothing
    }

}
