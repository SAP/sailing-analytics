package com.sap.sailing.server.test;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.expeditionconnector.impl.HttpServletMessageReceiver;

public class ExpeditionThroughHttpPostServletTest {
    @Disabled // To run, the OSGi-based Jetty needs to run and listen on port 8888"
    @Test
    public void testConnectDisconnect() throws IOException, InterruptedException {
        int jettyPort = 8888;
        URL url = new URL("http://localhost:"+jettyPort+"/sailingserver/expedition");
        HttpServletMessageReceiver httpReceiver = new HttpServletMessageReceiver(url, new HttpServletMessageReceiver.Receiver() {
            @Override
            public boolean received(byte[] bytes) {
                System.out.println(new String(bytes));
                return false; // don't stop
            }
        });
        httpReceiver.connect();
        synchronized (httpReceiver) {
            while (!httpReceiver.isStopped()) {
                httpReceiver.wait();
            }
        }
    }
}
