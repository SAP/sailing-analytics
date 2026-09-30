package com.sap.sailing.domain.test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import com.sap.sailing.domain.base.DomainFactory;

public abstract class AbstractSerializationTest {
    protected static <T extends Serializable> T cloneBySerialization(final T s, DomainFactory resolveAgainst) throws IOException, ClassNotFoundException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        final ObjectOutputStream dos = new ObjectOutputStream(pos);
        new Thread("clone writer") {
            public void run() {
                try {
                    dos.writeObject(s);
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.currentThread().setContextClassLoader(AbstractSerializationTest.class.getClassLoader());
        ObjectInputStream dis = resolveAgainst == null ? new ObjectInputStream(pis) :
            resolveAgainst.createObjectInputStreamResolvingAgainstThisFactory(pis, /* resolve listener */ null, /* classLoaderCache */ new HashMap<>());
        @SuppressWarnings("unchecked")
        T result = (T) dis.readObject();
        dis.close();
        return result;
    }

    static Object[] cloneManyBySerialization(DomainFactory resolveAgainst, final Serializable... objects) throws IOException, ClassNotFoundException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        final ObjectOutputStream dos = new ObjectOutputStream(pos);
        new Thread("clone writer") {
            public void run() {
                try {
                    for (Serializable s : objects) {
                        dos.writeObject(s);
                    }
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }.start();
        ObjectInputStream dis = resolveAgainst.createObjectInputStreamResolvingAgainstThisFactory(pis, /* resolve listener */ null, /* classLoaderCache */ new HashMap<>());
        Object[] result = new Object[objects.length];
        for (int i=0; i<objects.length; i++) {
            result[i] = dis.readObject();
        }
        dis.close();
        return result;
    }
}
