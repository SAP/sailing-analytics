package com.sap.sailing.server.trackfiles.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Element;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.gpx.binding11.WptType;

import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.server.trackfiles.impl.BaseRouteConverterGPSFixImporterImpl;
import com.sap.sailing.server.trackfiles.impl.RouteConverterGPSFixImporterImpl;

public class RouteConverterGpx11ExtensionsTest {
    private boolean extensionsFound = false;

    @Test
    public void areExtensionsRead() throws Exception {
        String testFile = "/equine-extensions.gpx";
        InputStream in = getClass().getResourceAsStream(testFile);
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

        importer.importFixes(in, new com.sap.sailing.domain.trackimport.GPSFixImporter.Callback() {
            @Override
            public void startTrack(String name, Map<String, String> properties) {
            }

            @Override
            public void addFix(GPSFix fix) {
            }
        }, false);
        
        assertTrue(extensionsFound);
    }
}
