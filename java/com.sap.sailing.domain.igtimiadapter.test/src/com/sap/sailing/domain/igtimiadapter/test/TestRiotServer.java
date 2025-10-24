package com.sap.sailing.domain.igtimiadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.FixFactory;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.domain.igtimiadapter.server.Activator;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotMessageListener;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;
import com.sap.sse.shared.util.Wait;

public class TestRiotServer {
    private static final Logger logger = Logger.getLogger(TestRiotServer.class.getName());

    private static class TestListener implements RiotMessageListener {
        private final List<Fix> fixes = new ArrayList<>();
        
        public Iterable<Fix> getAllFixesReceived() {
            return fixes;
        }

        @Override
        public void onMessage(Msg message) {
            final Iterable<Fix> newFixes = new FixFactory().createFixes(message);
            synchronized (this) {
                Util.addAll(newFixes, fixes);
            }
        }
    }
    
    @BeforeEach
    public void setUp() throws ClientProtocolException, IllegalStateException, IOException, ParseException {
        final SecurityService mockSecurityService = SecurityServiceMockFactory.mockSecurityService();
        Activator.getInstance().setSecurityService(mockSecurityService);
    }

    @Test
    public void testSendingAFewMessages() throws Exception {
        final MongoDBConfiguration mongoTestConfig = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService mongoTestService = mongoTestConfig.getService();
        final RiotServer riot = RiotServer.create(PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoTestService),
                PersistenceFactory.INSTANCE.getMongoObjectFactory(mongoTestService), /* OSGi Bundle Context */ null);
        final TestListener listener = new TestListener();
        riot.addListener(listener);
        try (final Socket socket = new Socket("localhost", riot.getPort())) {
            final OutputStream socketOutputStream = socket.getOutputStream();
            final InputStream is = getClass().getResourceAsStream("/windbot_startup_jan.protobuf");
            int b;
            while ((b=is.read()) != -1) {
                socketOutputStream.write(b);
            }
            socketOutputStream.flush();
            final boolean[] finished = new boolean[1];
            new Thread(()->{
                final InputStream socketInputStream;
                try {
                    socketInputStream = socket.getInputStream();
                    while (!finished[0]) {
                        final Msg message = Msg.parseDelimitedFrom(socketInputStream);
                        logger.info("Received message from Riot server:\n"+message);
                    }
                } catch (SocketException e) {
                    // we expect a SocketException when the socket is closed
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Exception trying to read from test Riot server", ioe);
                }
            }).start();
            Wait.wait(()->Util.size(listener.getAllFixesReceived()) >= 218, /* timeout */ Optional.of(Duration.ONE_MINUTE),
                    /* sleepBetweenAttempts */ Duration.ONE_SECOND,
                    Level.WARNING, "218 expected fixes; received "+Util.size(listener.getAllFixesReceived())+" so far");
            finished[0] = true;
            riot.stop();
            is.close();
            socketOutputStream.close();
            assertEquals(218, Util.size(listener.getAllFixesReceived()));
        } finally {
            riot.stop(); // may be redundant to the stop() call above, but it's idempotent and important to call at least once in case of exceptions
        }
    }
}
