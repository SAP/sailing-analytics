package com.sap.sailing.server.trackfiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;

import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.server.trackfiles.common.BaseGPSFixImporterImpl;

public abstract class BaseRouteConverterGPSFixImporterImpl extends BaseGPSFixImporterImpl {    
    public BaseRouteConverterGPSFixImporterImpl(List<Class<? extends NavigationFormat<?>>> supportedFormats) {
        this.supportedReadFormats = getFormatInstances(supportedFormats, false);
    }
    
    /**
     * Copied from {@link NavigationFormatParser#getFormatInstances}.
     * @param restrictToWritableFormats
     * @return
     */
    @SuppressWarnings("rawtypes")
    private static List<NavigationFormat> getFormatInstances(List<Class<? extends NavigationFormat<?>>> supportedFormats,
            boolean restrictToWritableFormats) {
        List<NavigationFormat> formats = new ArrayList<NavigationFormat>();
        for (Class<? extends NavigationFormat> formatClass : supportedFormats) {
            try {
                NavigationFormat format = formatClass.newInstance();
                if (restrictToWritableFormats && format.isSupportsWriting() ||
                        !restrictToWritableFormats && format.isSupportsReading())
                    formats.add(format);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate " + formatClass, e);
            }
        }
        return formats;
    }
    
    @SuppressWarnings("rawtypes")
    private final List<NavigationFormat> supportedReadFormats;
    
    public abstract GPSFix convertToGPSFix(BaseNavigationPosition position) throws Exception;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void importFixes(InputStream inputStream, Callback callback, boolean inferSpeedAndBearing, String sourceName)
            throws IOException, FormatNotSupportedException {
        //TODO dirty hack, because no public read method for inputstream and custom list of formats
        NavigationFormatParser parser = new NavigationFormatParser();
        List<BaseRoute> routes;
        try {
            Method m = NavigationFormatParser.class.getDeclaredMethod("read", InputStream.class, Integer.TYPE,
                    CompactCalendar.class, List.class);
            m.setAccessible(true);
            ParserResult result = (ParserResult) m.invoke(parser, inputStream, /* read buffer size; 1MB should be
            sufficient to tell whether the format is understood or not */ 10 * 1024 * 1024, null, supportedReadFormats);
            if (result == null) {
                throw new FormatNotSupportedException();
            }
            if (result.isSuccessful()) {
                routes = result.getAllRoutes();
            } else {
                routes = Collections.emptyList();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        
        for (BaseRoute route : routes) {
            List<? extends BaseNavigationPosition> positions = (List<? extends BaseNavigationPosition>) route.getPositions();
            String routeName = route.getName();
            TrackFileImportDeviceIdentifier device = new TrackFileImportDeviceIdentifierImpl(sourceName, routeName);
            for (BaseNavigationPosition p : positions) {
                try {
                    addFixAndInfer(callback, inferSpeedAndBearing, convertToGPSFix(p), device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
