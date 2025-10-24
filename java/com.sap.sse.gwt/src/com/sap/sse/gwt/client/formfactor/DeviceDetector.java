package com.sap.sse.gwt.client.formfactor;

import java.util.logging.Logger;

import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window.Navigator;

/**
 * Helps to detect different form factors/device categories by analyzing the browser's user agent string. Based on the
 * browser itself or the name of the operating system it's easy to detect mobile OSes as iOS or Android. Distinguishing
 * smartphones and tablets is a little bit more complex because you need to analyze common device ID patterns for known
 * tablets of various companies as Samsung or Sony.
 * 
 * By now, it's not possible to ask the browser about the device or even the physical size of the device or browser
 * window. It's also not possible to render an element with CSS size "1cm" or "1in" and read its pixel size in order to
 * calculate the real size of the screen. This is caused by the fact that at least mobile browsers always fake 96 dpi.
 */
public class DeviceDetector {
    private static Logger LOG = Logger.getLogger(DeviceDetector.class.getName());
    
    private static final RegExp isMobileRegExp = RegExp.compile(
            "Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile Safari", "i");
    private static final RegExp tabletBlacklistRegExp = RegExp.compile(
            "iPad|" // Apple ;-)
            + "Nexus 7|Nexus 10|Nexus 9|" // Google Tablets
            + "Ryu|" // Google Pixel C
            + "PlayBook|" // Blackberry
            + "KFAPWI|" // Kindle Fire HD
            + "FZ-B|" // Panasonic ToughPad
            + "SGP|" // Sony Xperia Tablets
            + "GT-P|SM-T|SM-P|" // Galaxy Tab (https://en.wikipedia.org/wiki/Samsung_Galaxy ; http://forum.xda-developers.com/wiki/Samsung/Model_naming_scheme)
            + "Tablet" // Firefox on Android
                    , "i");
    
    private static DeviceCategory deviceCategory;
    public static DeviceCategory getDeviceCategory() {
        if (deviceCategory == null) {
            deviceCategory = calculateDeviceCategory();
        }
        return deviceCategory;
    }
    
    private static DeviceCategory calculateDeviceCategory() {
        final DeviceCategory result;
        final boolean isMobile = isMobileRegExp.test(Navigator.getUserAgent());
        LOG.info("Navigator user agent matched mobile regex: " + isMobile);
        if (!isMobile) {
            result = DeviceCategory.DESKTOP;
        } else {
            boolean isTablet = tabletBlacklistRegExp.test(Navigator.getUserAgent());
            LOG.info("Navigator user agent matched tablet regex: " + isTablet);
            result = isTablet ? DeviceCategory.TABLET : DeviceCategory.MOBILE;
        }
        return result;
    }

    /**
     * Uses regular expression and user agent to detect mobile device.
     * 
     * @return
     */
    public static boolean isMobile() {
        return getDeviceCategory() == DeviceCategory.MOBILE;
    }
    
    /**
     * Uses regular expression and user agent to detect tablet device.
     * 
     * @return
     */
    public static boolean isTablet() {
        return getDeviceCategory() == DeviceCategory.TABLET;
    }

    /**
     * Convinience method
     * 
     * @return
     */
    public static boolean isDesktop() {
        return getDeviceCategory() == DeviceCategory.DESKTOP;
    }
}
