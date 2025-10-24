package com.sap.sse.landscape.aws;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.MongoProcessInReplicaSet;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;

public class MongoUriParserTest {
    private static final String WWW_EXAMPLE_COM = "127.0.0.2";
    private MongoUriParser<String> parser;
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws UnknownHostException {
        final AwsLandscape<String> landscape = Mockito.mock(AwsLandscape.class);
        final InetAddress wwwExampleCom = InetAddress.getByName(WWW_EXAMPLE_COM);
        final AwsInstance<String> host1 = mock(AwsInstance.class);
        when(host1.getPublicAddress()).thenReturn(wwwExampleCom);
        when(host1.getPrivateAddress()).thenReturn(wwwExampleCom);
        when(host1.getHostname()).thenReturn(WWW_EXAMPLE_COM);
        when(landscape.getHostByPrivateIpAddress(ArgumentMatchers.any(Region.class), ArgumentMatchers.contains(wwwExampleCom.getHostAddress()), ArgumentMatchers.any(HostSupplier.class))).thenReturn(host1);
        final InetAddress loopback = InetAddress.getLoopbackAddress();
        final AwsInstance<String> host2 = mock(AwsInstance.class);
        when(host2.getPrivateAddress()).thenReturn(loopback);
        when(host2.getHostname()).thenReturn(loopback.getHostAddress());
        when(landscape.getHostByPrivateIpAddress(ArgumentMatchers.any(Region.class), ArgumentMatchers.contains(loopback.getHostAddress()), ArgumentMatchers.any(HostSupplier.class))).thenReturn(host2);
        parser = new MongoUriParser<String>(landscape, new AwsRegion("eu-west-2", landscape));
    }
    
    @Test
    public void testSimpleSingleNodeUri() throws URISyntaxException, UnknownHostException {
        final String hostname = "127.0.0.1";
        final String dbName = "myDb";
        final Database database = parser.parseMongoUri("mongodb://"+hostname+"/"+dbName);
        assertFalse(database.getEndpoint().isReplicaSet());
        final MongoProcess mongoProcess = database.getEndpoint().asMongoProcess();
        assertEquals(hostname, mongoProcess.getHostname());
        assertEquals(27017, mongoProcess.getPort());
        assertEquals(dbName, database.getName());
    }

    @Test
    public void testSimpleSingleNodeUriWithExplicitPort() throws URISyntaxException, UnknownHostException {
        final String hostname = "127.0.0.1";
        final int port = 10202;
        final String dbName = "myDb";
        final Database database = parser.parseMongoUri("mongodb://"+hostname+":"+port+"/"+dbName);
        assertFalse(database.getEndpoint().isReplicaSet());
        final MongoProcess mongoProcess = database.getEndpoint().asMongoProcess();
        assertEquals(InetAddress.getByName(hostname).getHostAddress(), mongoProcess.getHostname());
        assertEquals(port, mongoProcess.getPort());
        assertEquals(dbName, database.getName());
    }

    @Test
    public void testExceptionForSingleNodeUriWithoutDatabaseName() throws URISyntaxException, UnknownHostException {
        final String hostname = "host.example.com";
        try {
            parser.parseMongoUri("mongodb://"+hostname);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testSimpleReplicaSetUri() throws URISyntaxException, UnknownHostException {
        final String replicaSetName = "humba";
        final String hostname1 = WWW_EXAMPLE_COM;
        final String hostname2 = "127.0.0.1";
        final String dbName = "myDb";
        final Database database = parser.parseMongoUri("mongodb://"+hostname1+","+hostname2+"/"+dbName+"?retryWrites=true&replicaSet="+replicaSetName+"&readPreference=nearest");
        assertTrue(database.getEndpoint().isReplicaSet());
        final MongoReplicaSet mongoReplicaSet = database.getEndpoint().asMongoReplicaSet();
        boolean foundHost1 = false;
        boolean foundHost2 = false;
        for (final MongoProcessInReplicaSet instance : mongoReplicaSet.getInstances()) {
            if (Util.equalsWithNull(InetAddress.getByName(hostname1).getHostAddress(), instance.getHostname())) {
                foundHost1 = true;
            }
            if (Util.equalsWithNull(hostname2, instance.getHostname())) {
                foundHost2 = true;
            }
            assertEquals(27017, instance.getPort());
        }
        assertTrue(foundHost1);
        assertTrue(foundHost2);
        assertEquals(dbName, database.getName());
    }

    @Test
    public void testSimpleReplicaSetUriWithExplicitPort() throws URISyntaxException, UnknownHostException {
        final String replicaSetName = "humba";
        final String hostname1 = WWW_EXAMPLE_COM;
        int port1 = 12345;
        final String hostname2 = "127.0.0.1";
        final String dbName = "myDb";
        final Database database = parser.parseMongoUri("mongodb://"+hostname1+":"+port1+","+hostname2+"/"+dbName+"?replicaSet="+replicaSetName+"&retryWrites=true&readPreference=nearest");
        assertTrue(database.getEndpoint().isReplicaSet());
        final MongoReplicaSet mongoReplicaSet = database.getEndpoint().asMongoReplicaSet();
        boolean foundHost1 = false;
        boolean foundHost2 = false;
        for (final MongoProcessInReplicaSet instance : mongoReplicaSet.getInstances()) {
            if (Util.equalsWithNull(InetAddress.getByName(hostname1).getHostAddress(), instance.getHostname())) {
                foundHost1 = true;
                assertEquals(port1, instance.getPort());
            }
            if (Util.equalsWithNull(hostname2, instance.getHostname())) {
                foundHost2 = true;
                assertEquals(27017, instance.getPort());
            }
        }
        assertTrue(foundHost1);
        assertTrue(foundHost2);
        assertEquals(dbName, database.getName());
    }

    @Test
    public void testExceptionForReplicaSetUriWithoutDatabaseName() throws URISyntaxException, UnknownHostException {
        final String replicaSetName = "humba";
        final String hostname1 = WWW_EXAMPLE_COM;
        int port1 = 12345;
        final String hostname2 = "127.0.0.1";
        try {
            parser.parseMongoUri("mongodb://"+hostname1+":"+port1+","+hostname2+"?replicaSet="+replicaSetName+"&retryWrites=true&readPreference=nearest");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
