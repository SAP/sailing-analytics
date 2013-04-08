package com.sap.sailing.util.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sap.sailing.domain.common.Named;

public class NamedReentrantReadWriteLock extends ReentrantReadWriteLock implements Named {
    private static final long serialVersionUID = 2906084982209339774L;
    private final String name;
    private final String readLockName;
    private final String writeLockName;
    private final WriteLockWrapper writeLockWrapper;
    private final ReadLockWrapper readLockWrapper;
    private transient List<Thread> readers;
    
    private class WriteLockWrapper extends WriteLock {
        private static final long serialVersionUID = -4234819025137348944L;
        private final WriteLock writeLock;
        
        protected WriteLockWrapper(WriteLock writeLock) {
            super(NamedReentrantReadWriteLock.this);
            this.writeLock = writeLock;
        }

        @Override
        public void lock() {
            writeLock.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            writeLock.lockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return writeLock.tryLock();
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return writeLock.tryLock(timeout, unit);
        }

        @Override
        public void unlock() {
            writeLock.unlock();
        }

        @Override
        public Condition newCondition() {
            return writeLock.newCondition();
        }

        @Override
        public String toString() {
            return writeLock.toString();
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return writeLock.isHeldByCurrentThread();
        }

        @Override
        public int getHoldCount() {
            return writeLock.getHoldCount();
        }
    }
    
    private class ReadLockWrapper extends ReadLock {
        private static final long serialVersionUID = -4232071609936663619L;
        private final ReadLock readLock;
        
        protected ReadLockWrapper(ReadLock readLock) {
            super(NamedReentrantReadWriteLock.this);
            this.readLock = readLock;
        }

        @Override
        public void lock() {
            readLock.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            try {
                readLock.lockInterruptibly();
                readers.add(Thread.currentThread());
            } catch (InterruptedException ie) {
                throw ie;
            }
        }

        @Override
        public boolean tryLock() {
            boolean result = readLock.tryLock();
            if (result) {
                readers.add(Thread.currentThread());
            }
            return result;
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            boolean result = readLock.tryLock(timeout, unit);
            if (result) {
                readers.add(Thread.currentThread());
            }
            return result;
        }

        @Override
        public void unlock() {
            readLock.unlock();
            readers.remove(Thread.currentThread());
        }

        @Override
        public Condition newCondition() {
            return readLock.newCondition();
        }

        @Override
        public String toString() {
            return readLock.toString();
        }
        
    }
    
    public NamedReentrantReadWriteLock(String name, boolean fair) {
        super(fair);
        this.name = name;
        this.readLockName = "readLock "+name;
        this.writeLockName = "writeLock "+name;
        this.writeLockWrapper = new WriteLockWrapper(super.writeLock());
        this.readLockWrapper = new ReadLockWrapper(super.readLock());
        this.readers = Collections.synchronizedList(new ArrayList<Thread>());
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.readers = Collections.synchronizedList(new ArrayList<Thread>());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public WriteLock writeLock() {
        return writeLockWrapper;
    }

    @Override
    public ReadLock readLock() {
        return readLockWrapper;
    }

    public List<Thread> getReaders() {
        return new ArrayList<Thread>(readers);
    }
    
    public Thread getWriter() {
        return getOwner();
    }
    
    @Override
    public String toString() {
        return "ReentrantReadWriteLock "+getName()+" ("+(isFair()?"fair":"unfair")+")";
    }

    protected String getReadLockName() {
        return readLockName;
    }

    protected String getWriteLockName() {
        return writeLockName;
    }
}
