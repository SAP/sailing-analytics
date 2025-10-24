package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sse.common.Util;

public class EventLeaderboardGroupLinkTest {
    @Test
    public void testLivelinessAndIteratorStabilityOfUnmodifiableLeaderboardGroupCollection() {
        Event e = new EventImpl("E", null, null, "Venue", /* public */ true, UUID.randomUUID());
        LeaderboardGroupImpl lg1 = new LeaderboardGroupImpl("lg1", "LG1", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard>emptyList());
        LeaderboardGroupImpl lg2 = new LeaderboardGroupImpl("lg2", "LG2", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard>emptyList());
        LeaderboardGroupImpl lg3 = new LeaderboardGroupImpl("lg3", "LG3", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard>emptyList());
        LeaderboardGroupImpl lg4 = new LeaderboardGroupImpl("lg4", "LG4", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard>emptyList());
        e.addLeaderboardGroup(lg1);
        Iterable<LeaderboardGroup> lgs = e.getLeaderboardGroups();
        Iterator<LeaderboardGroup> iter = lgs.iterator();
        assertSame(lg1, iter.next());
        assertEquals(1, Util.size(lgs));
        e.addLeaderboardGroup(lg3);
        assertEquals(2, Util.size(lgs));
        assertEquals(lg1, Util.get(lgs, 0));
        assertEquals(lg3, Util.get(lgs, 1));
        assertFalse(iter.hasNext()); // insertion immediately after the current element is not recognized by the iterator
        e.removeLeaderboardGroup(lg3);
        e.addLeaderboardGroup(lg2);
        e.addLeaderboardGroup(lg3);
        e.addLeaderboardGroup(lg4);
    }
}
