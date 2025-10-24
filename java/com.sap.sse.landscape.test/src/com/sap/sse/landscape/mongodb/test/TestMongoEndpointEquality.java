package com.sap.sse.landscape.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.jcraft.jsch.ChannelSftp;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.AvailabilityZone;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoProcessInReplicaSet;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.mongodb.impl.MongoProcessInReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.MongoReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.SimpleMongoEndpointImpl;
import com.sap.sse.landscape.ssh.SshCommandChannel;

public class TestMongoEndpointEquality {
    @Test
    public void testSimpleEndpointEquality() {
        final MongoEndpoint a = new SimpleMongoEndpointImpl("my.mongodb.com", 27017);
        final MongoEndpoint b = new SimpleMongoEndpointImpl("my.mongodb.com", 27017);
        assertEquals(a, b);
    }

    @Test
    public void testSimpleEndpointUnequality() {
        final MongoEndpoint a = new SimpleMongoEndpointImpl("my.mongodb.com", 27017);
        final MongoEndpoint b = new SimpleMongoEndpointImpl("your.mongodb.com", 27017);
        assertNotEquals(a, b);
    }

    @Test
    public void testReplicaSetEquality() throws UnknownHostException {
        // set up first replica set:
        final MongoReplicaSet replicaSet1 = new MongoReplicaSetImpl("abc");
        final Host hostA = new MockHost("abc", InetAddress.getByName("www.fzi.de"));
        final Host hostB = new MockHost("def", InetAddress.getByName("www.sap.com"));
        final MongoProcessInReplicaSet a = new MongoProcessInReplicaSetImpl(replicaSet1, 27017, hostA);
        replicaSet1.addReplica(a);
        final MongoProcessInReplicaSet b = new MongoProcessInReplicaSetImpl(replicaSet1, 10202, hostB);
        replicaSet1.addReplica(b);
        // set up second replica set:
        final MongoReplicaSet replicaSet2 = new MongoReplicaSetImpl("abc");
        final Host hostC = new MockHost("abc", InetAddress.getByName("www.fzi.de"));
        final Host hostD = new MockHost("def", InetAddress.getByName("www.sap.com"));
        final MongoProcessInReplicaSet c = new MongoProcessInReplicaSetImpl(replicaSet2, 27017, hostC);
        replicaSet2.addReplica(c);
        final MongoProcessInReplicaSet d = new MongoProcessInReplicaSetImpl(replicaSet2, 10202, hostD);
        replicaSet2.addReplica(d);
        assertEquals(replicaSet1, replicaSet2);
        final Host hostE = new MockHost("ghi", InetAddress.getByName("www.sap.com"));
        final MongoProcessInReplicaSet e = new MongoProcessInReplicaSetImpl(replicaSet2, 10202, hostE);
        replicaSet2.addReplica(e);
        assertNotEquals(replicaSet1, replicaSet2);
    }
    
    private static class MockHost implements Host {
        private final Serializable id;
        private final InetAddress privateAddress;
        
        public MockHost(Serializable id, InetAddress privateAddress) {
            super();
            this.id = id;
            this.privateAddress = privateAddress;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MockHost other = (MockHost) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        @Override
        public Serializable getId() {
            return id;
        }

        @Override
        public InetAddress getPublicAddress() {
            return null;
        }

        @Override
        public InetAddress getPublicAddress(Optional<Duration> timeoutEmptyMeaningForever)
                throws TimeoutException, Exception {
            return null;
        }

        @Override
        public InetAddress getPrivateAddress() {
            return privateAddress;
        }

        @Override
        public InetAddress getPrivateAddress(Optional<Duration> timeoutEmptyMeaningForever) {
            return null;
        }

        @Override
        public SshCommandChannel createSshChannel(String sshUserName, Optional<Duration> optionalTimeout,
                Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
            return null;
        }

        @Override
        public SshCommandChannel createRootSshChannel(Optional<Duration> optionalTimeout,
                Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
            return null;
        }

        @Override
        public ChannelSftp createSftpChannel(String sshUserName, Optional<Duration> optionalTimeout,
                Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
            return null;
        }

        @Override
        public ChannelSftp createRootSftpChannel(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName,
                byte[] privateKeyEncryptionPassphrase) throws Exception {
            return null;
        }

        @Override
        public AvailabilityZone getAvailabilityZone() {
            return null;
        }

        @Override
        public Iterable<SecurityGroup> getSecurityGroups() {
            return null;
        }

        @Override
        public TimePoint getLaunchTimePoint() {
            return null;
        }

        @Override
        public String getNameTag() {
            return null;
        }

        @Override
        public String getImageId() {
            return null;
        }
    }
}
