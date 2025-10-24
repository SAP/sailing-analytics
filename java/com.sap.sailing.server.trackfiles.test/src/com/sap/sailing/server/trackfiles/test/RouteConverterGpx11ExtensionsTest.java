package com.sap.sailing.server.trackfiles.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Element;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.server.trackfiles.impl.BaseRouteConverterGPSFixImporterImpl;
import com.sap.sailing.server.trackfiles.impl.RouteConverterGPSFixImporterImpl;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.gpx.binding11.WptType;

public class RouteConverterGpx11ExtensionsTest {
    private boolean extensionsFound = false;

    @Test
    public void areExtensionsRead() throws Exception {
        String testFile = "/equine-extensions.gpx";
        try (InputStream in = getClass().getResourceAsStream(testFile)) {
            assert in != null : new Exception("Input file not found");
            BaseRouteConverterGPSFixImporterImpl importer = spy(new RouteConverterGPSFixImporterImpl());
            doAnswer(new Answer<GPSFix>() {
                @Override
                public GPSFix answer(InvocationOnMock invocation) throws Throwable {
                    BaseNavigationPosition p = (BaseNavigationPosition) invocation.getArguments()[0];
                    WptType waypoint = p.asGpxPosition().getOrigin(WptType.class);
                    final List<Object> extensions = waypoint.getExtensions().getAny();
                    for (Object o : extensions) {
                        Element el = (Element) o;
                        System.out.println(el.getLocalName() + " " + el.getTextContent());
                        extensionsFound = true;
                    }
                    return (GPSFix) invocation.callRealMethod();
                }
            }).when(importer).convertToGPSFix(any(BaseNavigationPosition.class));
            importer.importFixes(in, Charset.defaultCharset(), new com.sap.sailing.domain.trackimport.GPSFixImporter.Callback() {
                @Override
                public void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device) {
                }
            }, false, "source");
            assertTrue(extensionsFound);
        }
    }
}
