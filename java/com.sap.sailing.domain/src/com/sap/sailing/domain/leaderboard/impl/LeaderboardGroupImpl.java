package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sap.sailing.domain.base.impl.LeaderboardGroupBaseImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupListener;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

public class LeaderboardGroupImpl extends LeaderboardGroupBaseImpl implements LeaderboardGroup {
    
    private static final long serialVersionUID = 2035927369446736934L;
    private boolean displayGroupsInReverseOrder;
    
    /**
     * The lock that guards access to the {@link #leaderboards} list.
     */
    private final NamedReentrantReadWriteLock leaderboardsLock;
    
    /**
     * The list of leaderboards grouped in this leaderboard group. Access to this list is guarded by
     * {@link #leaderboardsLock}. All access has to acquire the corresponding read or write lock.
     */
    private final List<Leaderboard> leaderboards;
    
    private final Set<LeaderboardGroupListener> listeners;
    
    /**
     * An optional meta-leaderboard that shows overall results computed from the {@link #leaderboards} aggregated by
     * this group. The meta-leaderboard currently has no own state that requires any persistence but can be re-constructed
     * entirely from this leaderboard group.
     */
    private Leaderboard overallLeaderboard;

    /**
     * Creates a new leaderboard group with a new UUID as its ID.
     */
    public LeaderboardGroupImpl(String name, String description, String displayName,
            boolean displayGroupsInReverseOrder, List<? extends Leaderboard> leaderboards) {
        this(UUID.randomUUID(), name, description, displayName, displayGroupsInReverseOrder, leaderboards);
    }
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        LockUtil.lockForRead(leaderboardsLock);
        try {
            oos.defaultWriteObject();
        } finally {
            LockUtil.unlockAfterRead(leaderboardsLock);
        }
    }

    /**
     * Use this constructor when loading or deserializing a leaderboard group and the ID is known and is provided to the constructor.
     */
    public LeaderboardGroupImpl(UUID id, String name, String description, String displayName,
            boolean displayGroupsInReverseOrder, List<? extends Leaderboard> leaderboards) {
        super(id, name, description, displayName);
        this.leaderboardsLock = new NamedReentrantReadWriteLock("leaderboards lock fo rleaderboard group "+name, /* fair */ false);
        this.displayGroupsInReverseOrder = displayGroupsInReverseOrder;
        this.leaderboards = new ArrayList<>(leaderboards);
        this.listeners = new HashSet<>();
    }
    
    @Override
    public void addLeaderboardGroupListener(LeaderboardGroupListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLeaderboardGroupListener(LeaderboardGroupListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private Set<LeaderboardGroupListener> getLeaderboardGroupListeners() {
        synchronized (listeners) {
            return new HashSet<LeaderboardGroupListener>(listeners);
        }
    }
    
    private void notifyLeaderboardGroupListenersAboutLeaderboardAdded(Leaderboard leaderboard) {
        for (LeaderboardGroupListener listener : getLeaderboardGroupListeners()) {
            listener.leaderboardAdded(this, leaderboard);
        }
    }

    private void notifyLeaderboardGroupListenersAboutLeaderboardRemoved(Leaderboard leaderboard) {
        for (LeaderboardGroupListener listener : getLeaderboardGroupListeners()) {
            listener.leaderboardRemoved(this, leaderboard);
        }
    }

    @Override
    public Leaderboard getOverallLeaderboard() {
        return overallLeaderboard;
    }

    @Override
    public boolean hasOverallLeaderboard() {
        return getOverallLeaderboard() != null;
    }

    @Override
    public String getOverallLeaderboardName() {
        return hasOverallLeaderboard() ? getOverallLeaderboard().getName() : null;
    }

    @Override
    public void setOverallLeaderboard(Leaderboard overallLeaderboard) {
        this.overallLeaderboard = overallLeaderboard;
    }

    @Override
    public Iterable<Leaderboard> getLeaderboards() {
        LockUtil.lockForRead(leaderboardsLock);
        try {
            return new ArrayList<Leaderboard>(leaderboards);
        } finally {
            LockUtil.unlockAfterRead(leaderboardsLock);
        }
    }

    @Override
    public int getIndexOf(Leaderboard leaderboard) {
        LockUtil.lockForRead(leaderboardsLock);
        try {
            return leaderboards.indexOf(leaderboard);
        } finally {
            LockUtil.unlockAfterRead(leaderboardsLock);
        }
    }

    @Override
    public void addLeaderboard(Leaderboard leaderboard) {
        if (leaderboard == overallLeaderboard) {
            throw new IllegalArgumentException("Cannot insert the overall leaderboard into its own leaderboard group");
        }
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            addLeaderboardAt(leaderboard, leaderboards.size());
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
    }
    
    @Override
    public void addLeaderboardAt(Leaderboard leaderboard, int index) {
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            leaderboards.add(index, leaderboard);
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
        notifyLeaderboardGroupListenersAboutLeaderboardAdded(leaderboard);
    }

    @Override
    public void addAllLeaderboards(Collection<Leaderboard> leaderboards) {
        for (Leaderboard leaderboard : leaderboards) {
            addLeaderboard(leaderboard);
        }
    }

    @Override
    public void removeLeaderboard(Leaderboard leaderboard) {
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            leaderboards.remove(leaderboard);
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
        notifyLeaderboardGroupListenersAboutLeaderboardRemoved(leaderboard);
    }

    @Override
    public void removeAllLeaderboards(Collection<Leaderboard> leaderboards) {
        for (Leaderboard leaderboard : leaderboards) {
            removeLeaderboard(leaderboard);
        }
    }

    @Override
    public void clearLeaderboards() {
        for (Leaderboard leaderboard : getLeaderboards()) {
            removeLeaderboard(leaderboard);
        }
    }

    @Override
    public boolean isDisplayGroupsInReverseOrder() {
        return displayGroupsInReverseOrder;
    }
    
    public String toString() {
        return getName() + " " + getDescription();
    }
}
