package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.FlexibleRaceColumn;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardDTOCacheInvalidationTest {
    @Test
    public void testFlexibleLeaderboardNameChangeInvalidatesCache()
            throws NoWindException, InterruptedException, ExecutionException {
        final String oldName = "My Flexible Leaderboard";
        final String newName = "My New Leaderboard";
        FlexibleLeaderboard l = new FlexibleLeaderboardImpl(oldName,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(),
                new CourseAreaImpl("My Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        final TimePoint now = MillisecondsTimePoint.now();
        LeaderboardDTO dto = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(oldName, dto.getName());
        l.setDisplayName(newName);
        LeaderboardDTO dtoNew = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(newName, dtoNew.getDisplayName());
    }

    @Test
    public void testFlexibleLeaderboardDisplayNameChangeInvalidatesCache()
            throws NoWindException, InterruptedException, ExecutionException {
        final String name = "My Flexible Leaderboard";
        final String oldDisplayName = "D1";
        final String newDisplayName = "D2";
        final TimePoint now = MillisecondsTimePoint.now();
        FlexibleLeaderboard l = new FlexibleLeaderboardImpl(name,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(),
                new CourseAreaImpl("My Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        final LeaderboardGroup lg = new LeaderboardGroupImpl("LG", "LG", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Arrays.asList(l));
        final LeaderboardGroupMetaLeaderboard seriesLb = new LeaderboardGroupMetaLeaderboard(lg, new LowPoint(), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        LeaderboardDTO seriesLbDtoBeforeDisplayNameIsSet = seriesLb.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(name, seriesLbDtoBeforeDisplayNameIsSet.getRaceList().iterator().next().getName());
        l.setDisplayName(oldDisplayName);
        LeaderboardDTO dto = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(oldDisplayName, dto.getDisplayName());
        LeaderboardDTO seriesLbDto = seriesLb.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(oldDisplayName, seriesLbDto.getRaceList().iterator().next().getName());
        l.setDisplayName(newDisplayName);
        LeaderboardDTO dtoNew = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(newDisplayName, dtoNew.getDisplayName());
        LeaderboardDTO seriesLbDtoNew = seriesLb.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(newDisplayName, seriesLbDtoNew.getRaceList().iterator().next().getName());
    }

    @Test
    public void testFlexibleLeaderboardRaceColumnNameChangeInvalidatesCache()
            throws NoWindException, InterruptedException, ExecutionException {
        final String name = "My Flexible Leaderboard";
        FlexibleLeaderboard l = new FlexibleLeaderboardImpl(name,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(),
                new CourseAreaImpl("My Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        final String oldRaceColumnName = "Old";
        FlexibleRaceColumn rc = l.addRaceColumn(oldRaceColumnName, /* medalRace */ false);
        final TimePoint now = MillisecondsTimePoint.now();
        LeaderboardDTO dto = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(oldRaceColumnName, dto.getRaceList().iterator().next().getName());
        final String newRaceColumnName = "New";
        rc.setName(newRaceColumnName);
        LeaderboardDTO dtoNew = l.getLeaderboardDTO(now, Collections.<String> emptySet(),
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ null, DomainFactory.INSTANCE,
                /* fillTotalPointsUncorrected */ false);
        assertEquals(newRaceColumnName, dtoNew.getRaceList().iterator().next().getName());
    }

}
