package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

/**
 * When a {@link NamedReentrantReadWriteLock} is being serialized and de-serialized, what will its locking state be on
 * the receiving end? Can problems occur because the de-serialized copy is locked, as was the original that got
 * serialized? This could lead to massive problems on the de-serializing end.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class LockSerializationTest {
    private NamedReentrantReadWriteLock lock;
    
    @BeforeEach
    public void setUp() {
        lock = new NamedReentrantReadWriteLock("A", /* fair */ false);
    }
    
    @Test
    public void testUnlockedSerialization() throws IOException, ClassNotFoundException {
        assertDeserializedUnlocked();
    }

    @Test
    public void testReadLockedSerialization() throws IOException, ClassNotFoundException {
        LockUtil.lockForRead(lock);
        try {
            assertDeserializedUnlocked();
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
    }

    @Test
    public void testWriteLockedSerialization() throws IOException, ClassNotFoundException {
        LockUtil.lockForWrite(lock);
        try {
            assertDeserializedUnlocked();
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }

    private void assertDeserializedUnlocked() throws IOException, ClassNotFoundException {
        final NamedReentrantReadWriteLock deserializedLock = serializeDeserialize();
        assertFalse(deserializedLock.isWriteLocked());
        assertEquals(0, deserializedLock.getReadHoldCount());
        assertEquals(0, deserializedLock.getReadLockCount());
    }

    private NamedReentrantReadWriteLock serializeDeserialize() throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(lock);
        oos.close();
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        return (NamedReentrantReadWriteLock) ois.readObject();
    }
}
