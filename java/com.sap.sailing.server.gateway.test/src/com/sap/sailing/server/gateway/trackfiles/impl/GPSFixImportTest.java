package com.sap.sailing.server.gateway.trackfiles.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.server.trackfiles.RouteConverterGPSFixImporterFactory;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util.Pair;

public class GPSFixImportTest {
    private static class BufferedInputStreamWithPublicBuffer extends BufferedInputStream {
        public BufferedInputStreamWithPublicBuffer(InputStream in) {
            super(in);
        }

        public byte[] getBuffer() {
            return buf;
        }
    }
    
    @Test
    public void testReReadingEOFFromBufferedInputStream() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/Cardiff Race17 - COMPETITORS.gpx");
                BufferedInputStreamWithPublicBuffer bis = new BufferedInputStreamWithPublicBuffer(in)) {
            bis.mark(10 * 1024 * 1024); // the file has ~2MB, so 10MB is much more than twice the size, so should never
                                        // be required
            int size = 0;
            while (bis.read() != -1) {
                size++;
            }
            bis.reset();
            int secondSize = 0;
            while (bis.read() != -1) {
                secondSize++;
            }
            assertEquals(size, secondSize);
            assertTrue(bis.getBuffer().length < 8 * 1024 * 1024);
            bis.reset();
            int thirdSize = 0;
            while (bis.read() != -1) {
                thirdSize++;
            }
            assertEquals(size, thirdSize);
            assertTrue(bis.getBuffer().length < 8 * 1024 * 1024);
            // Now try to read EOF multiple times
            for (int i = 1; i < 1000; i++) {
                assertEquals(-1, bis.read());
            }
        }
    }
    
    @Test
    public void testReusingImportStream() throws IOException {
        TrackFilesImporter importer = new TrackFilesImporter(null, null, null) {
            @Override
            public Collection<GPSFixImporter> getGPSFixImporters(String type) {
                return Arrays.asList((GPSFixImporter) RouteConverterGPSFixImporterFactory.INSTANCE
                        .createRouteConverterGPSFixImporter());
            }
            
            @Override
            protected void additionalDataExtractor(ImportResult jsonResult, TrackFileImportDeviceIdentifier device)
                    throws TransformationException {
            }
            
            @Override
            public void storeFix(GPSFix fix, DeviceIdentifier deviceIdentifier) {
            }
        };
        FileItem fi = new FileItem() {
            private static final long serialVersionUID = 1L;

            @Override
            public void write(File file) throws Exception {
            }

            @Override
            public void setFormField(boolean state) {
            }

            @Override
            public void setFieldName(String name) {
            }

            @Override
            public boolean isInMemory() {
                return false;
            }

            @Override
            public boolean isFormField() {
                return false;
            }

            @Override
            public String getString(String encoding) throws UnsupportedEncodingException {
                return null;
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return null;
            }

            @Override
            public String getName() {
                return "Cardiff Race17 - COMPETITORS.gpx";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return getClass().getResourceAsStream("/" + getName());
            }

            @Override
            public String getFieldName() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public byte[] get() {
                return null;
            }

            @Override
            public void delete() {
            }
        };
        AtomicBoolean failed = new AtomicBoolean(false);
        ImportResult holder = new ImportResult(Logger.getLogger(GPSFixImportTest.class.getName())){
            
            @Override
            public void add(Exception exception) {
                super.add(exception);
                failed.set(true);
            }
        };
        // The preferred importer will fail, however the default importer should succeed after
        importer.importFilesWithPreferredImporter(Arrays.asList(new Pair<>("test.gpx", fi)), holder, new AlwaysFailingGPSFixImporter(-1));
        assertFalse(failed.get());
    }
}
