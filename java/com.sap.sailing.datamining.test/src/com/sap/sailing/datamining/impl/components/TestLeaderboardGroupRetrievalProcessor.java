package com.sap.sailing.datamining.impl.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sailing.datamining.test.util.NullProcessor;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.datamining.components.Processor;

public class TestLeaderboardGroupRetrievalProcessor {
    
    private RacingEventService service;
    private Processor<RacingEventService, HasLeaderboardGroupContext> retriever;
    
    private Collection<LeaderboardGroup> retrievedGroups;
    
    @Before
    public void initializeComponents() {
        service = mock(RacingEventService.class);
        when(service.getLeaderboardGroups()).thenReturn(getGroupsInService());
        retrievedGroups = new HashSet<>();
        Processor<HasLeaderboardGroupContext, Void> receiver = new NullProcessor<HasLeaderboardGroupContext, Void>(HasLeaderboardGroupContext.class, Void.class) {
            @Override
            public void processElement(HasLeaderboardGroupContext element) {
                retrievedGroups.add(element.getLeaderboardGroup());
            }
        };
        
        Collection<Processor<HasLeaderboardGroupContext, ?>> resultReceivers = new ArrayList<>();
        resultReceivers.add(receiver);
        retriever = new LeaderboardGroupRetrievalProcessor(ConcurrencyTestsUtil.getExecutor(), resultReceivers, 0, "");
    }

    private Map<UUID, LeaderboardGroup> getGroupsInService() {
        Map<UUID, LeaderboardGroup> groups = new HashMap<>();
        for (int i=1; i<=5; i++) {
            final LeaderboardGroupImpl lg = new LeaderboardGroupImpl("LG"+i, "", /* displayName */ null, false, new ArrayList<Leaderboard>());
            groups.put(lg.getId(), lg);
        }
        return groups;
    }

    @Test
    public void testFilteringRetrieval() throws InterruptedException {
        retriever.processElement(service);
        retriever.finish();
        final Set<LeaderboardGroup> expectedGroups = new HashSet<>(service.getLeaderboardGroups().values());
        assertThat(new HashSet<>(retrievedGroups), is(expectedGroups));
    }
}
