package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.sap.sailing.domain.common.ConflictObjectType;
import com.sap.sse.common.ObjectAlreadyExistsException;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.ReplicablesProvider;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationReceiver;
import com.sap.sse.replication.impl.ReplicationReceiverImpl;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;

import net.jpountz.lz4.LZ4BlockOutputStream;

public class ReplicationReceiverExceptionTest {
    @Test
    public void testCurrentMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ false, /* startSuspended */ false, /* asynchronous */ false);
    }

    @Test
    public void testLegacyMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ true, /* startSuspended */ false, /* asynchronous */ false);
    }

    @Test
    public void testSuspendedCurrentMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ false, /* startSuspended */ true, /* asynchronous */ false);
    }

    @Test
    public void testSuspendedLegacyMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ true, /* startSuspended */ true, /* asynchronous */ false);
    }

    @Test
    public void testAsynchronousCurrentMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ false, /* startSuspended */ false, /* asynchronous */ true);
    }

    @Test
    public void testAsynchronousLegacyMessageContinuesAfterFailedOperations() throws Exception {
        assertFollowingOperationsApplied(/* legacy */ true, /* startSuspended */ false, /* asynchronous */ true);
    }

    private void assertFollowingOperationsApplied(final boolean legacy, final boolean startSuspended,
            final boolean asynchronous) throws Exception {
        final TestReplica replica = new TestReplica();
        final ReplicationMasterDescriptor master = mock(ReplicationMasterDescriptor.class);
        final ReplicablesProvider provider = mock(ReplicablesProvider.class);
        when(master.getReplicables()).thenReturn(Collections.singleton(replica));
        doReturn(replica).when(provider).getReplicable(replica.getId().toString(), /* wait */ false);
        final QueueingConsumer consumer = new QueueingConsumer(mock(Channel.class));
        // Both failures and the following successful operation are deliberately in one transport message.
        consumer.handleDelivery("test", new Envelope(/* deliveryTag */ 1, /* redeliver */ false, "test", "test"),
                /* props */ null, serializeMessage(replica, legacy, asynchronous, TestOperation.BEFORE,
                        TestOperation.CONFLICT, TestOperation.CHECKED_FAILURE, TestOperation.AFTER));
        consumer.handleDelivery("test", new Envelope(/* deliveryTag */ 2, /* redeliver */ false, "test", "test"),
                /* props */ null, serializeMessage(replica, legacy, asynchronous, TestOperation.NEXT_MESSAGE));
        consumer.handleCancel("test");
        final ReplicationReceiverImpl receiver = new ReplicationReceiverImpl(master, provider, startSuspended, consumer);
        final List<LogRecord> failures = new CopyOnWriteArrayList<>();
        final Logger logger = Logger.getLogger(ReplicationReceiverImpl.class.getName());
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getThrown() != null) {
                    failures.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            receiver.run();
            if (startSuspended) {
                assertTrue(replica.entries.isEmpty(), "Suspended reception must not apply operations yet");
                receiver.setSuspended(/* suspended */ false);
            }
            assertTrue(replica.completed.await(/* timeout */ 5, TimeUnit.SECONDS),
                    "The operation in the next message did not complete; replica state: " + replica.entries);
            assertEquals(Arrays.asList("BEFORE", "AFTER", "NEXT_MESSAGE"), replica.entries);
            assertEquals(/* expected */ 2, failures.size());
            assertTrue(failures.stream().anyMatch(
                    (final LogRecord record) -> record.getThrown() instanceof ObjectAlreadyExistsException));
            assertTrue(failures.stream().anyMatch(
                    (final LogRecord record) -> record.getThrown().getCause() instanceof IOException));
            assertTrue(failures.stream().allMatch(
                    (final LogRecord record) -> record.getMessage().contains(replica.getId().toString())),
                    "Failure logs must identify the receiving replicable");
            assertTrue(failures.get(/* index */ 0).getMessage().contains(TestOperation.CONFLICT.name()));
            assertTrue(failures.get(/* index */ 1).getMessage().contains(TestOperation.CHECKED_FAILURE.name()));
        } finally {
            logger.removeHandler(handler);
            receiver.stop(/* applyQueuedMessages */ false);
        }
    }

    private byte[] serializeMessage(final TestReplica replica, final boolean legacy, final boolean asynchronous,
            final TestOperation... operations) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final DataOutputStream header = new DataOutputStream(new LZ4BlockOutputStream(bytes))) {
            if (!legacy) {
                header.writeUTF(ReplicationReceiver.VERSION_INDICATOR);
                header.write(/* protocolVersion */ 1);
            }
            header.writeUTF(replica.getId().toString());
            try (final ObjectOutputStream output = new ObjectOutputStream(header)) {
                for (final TestOperation operation : operations) {
                    final OperationWithResult<TestReplica, Void> operationToWrite = asynchronous
                            ? AsyncTestOperation.valueOf(operation.name()) : operation;
                    if (legacy) {
                        final ByteArrayOutputStream operationBytes = new ByteArrayOutputStream();
                        replica.writeOperation(operationToWrite, operationBytes, /* closeStream */ true);
                        output.writeObject(operationBytes.toByteArray());
                    } else {
                        output.writeObject(operationToWrite);
                    }
                }
            }
        }
        return bytes.toByteArray();
    }

    private enum TestOperation implements OperationWithResult<TestReplica, Void> {
        BEFORE, CONFLICT, CHECKED_FAILURE, AFTER, NEXT_MESSAGE;

        @Override
        public Void internalApplyTo(final TestReplica replica) throws IOException {
            if (this == CHECKED_FAILURE) {
                throw new IOException("Expected operation failure, wrapped by applyReplicated");
            }
            final String entry = this == CONFLICT ? BEFORE.name() : name();
            if (replica.entries.contains(entry)) {
                throw new ObjectAlreadyExistsException(ConflictObjectType.LEADERBOARD, entry);
            }
            replica.entries.add(entry);
            if (this == NEXT_MESSAGE) {
                replica.completed.countDown();
            }
            return null;
        }
    }

    private enum AsyncTestOperation implements OperationWithResult<TestReplica, Void> {
        BEFORE, CONFLICT, CHECKED_FAILURE, AFTER, NEXT_MESSAGE;

        @Override
        public Void internalApplyTo(final TestReplica replica) throws IOException {
            return TestOperation.valueOf(name()).internalApplyTo(replica);
        }

        @Override
        public boolean requiresSynchronousExecution() {
            return false;
        }

        @Override
        public Object getKeyForAsynchronousExecution() {
            return TestReplica.class.getName();
        }
    }

    private static final class TestReplica extends AbstractReplicableWithObjectInputStream<TestReplica,
            OperationWithResult<TestReplica, ?>> {
        private final List<String> entries = new CopyOnWriteArrayList<>();
        private final CountDownLatch completed = new CountDownLatch(/* count */ 1);

        @Override
        public ObjectInputStream createObjectInputStreamResolvingAgainstCache(final InputStream input,
                final Map<String, Class<?>> classLoaderCache) throws IOException {
            // Use the receiving replicable's class loader to resolve test operations across OSGi bundle boundaries.
            return new ObjectInputStreamResolvingAgainstCache<TestReplica>(input, this, /* resolveListener */ null,
                    classLoaderCache) {
            };
        }

        @Override
        public void initiallyFillFromInternal(final ObjectInputStream input) {
            throw new UnsupportedOperationException("This test receives operations only");
        }

        @Override
        public void serializeForInitialReplicationInternal(final ObjectOutputStream output) {
            throw new UnsupportedOperationException("This test sends operations only");
        }

        @Override
        public void clearReplicaState() {
            entries.clear();
        }
    }
}
