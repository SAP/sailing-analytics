package com.sap.sailing.domain.queclinkadapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.queclinkadapter.tracker.QueclinkTCPTracker;

public class TestQueclinkTCPTracker extends AbstractQueclinkTrackerTest {
    private QueclinkTCPTracker tracker;

    @Override
    @BeforeEach
    public void setUp() throws MongoException, IOException {
        super.setUp();
        tracker = new QueclinkTCPTracker(/* pick a port */ 0, store);
    }

    @Override
    @AfterEach
    public void after() throws IOException {
        super.after();
        tracker.stop();
    }

    @Test
    public void testSendingLogToSocket() throws IOException, InterruptedException {
        final int port = tracker.getPort();
        assertTrue(port > 0);
        final Socket socket = new Socket("127.0.0.1", port);
        final OutputStream outputStream = socket.getOutputStream();
        final InputStream inputStreamForTestData = getClass().getResourceAsStream("/queclink_stream");
        int b;
        while ((b=inputStreamForTestData.read()) != -1) {
            outputStream.write(b);
        }
        socket.close();
        inputStreamForTestData.close();
        Thread.sleep(5000); // wait for all data to have arrived...
        assertEquals(807, store.getNumberOfFixes(deviceIdentifier));
    }
}
