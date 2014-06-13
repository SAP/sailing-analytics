package com.sap.sailing.gwt.ui.test;

import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.server.RacingEventService;

public class PingMarkViaRaceLogTest {
    private SailingServiceImplMock sailingService;
    private RacingEventService service;
    private final String columnName = "column";
    private final Fleet fleet = new FleetImpl("fleet");
    
    @Before
    public void prepare() {
        sailingService = new SailingServiceImplMock();
        service = sailingService.getRacingEventService();
    }
    
    @Test
    public void testPinging() {
        service.getMongoObjectFactory().getDatabase().dropDatabase();
        Series series = new SeriesImpl("series", false, Collections.singletonList(fleet),
                Collections.singletonList(columnName), service);
        Regatta regatta = service.createRegatta("regatta", "Laser", UUID.randomUUID(), Collections.<Series>singletonList(series),
                false, new HighPoint(), UUID.randomUUID());
        RegattaLeaderboard leaderboard = service.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "RegattaLeaderboard", new int[] {});
        
        MarkDTO mark = new MarkDTO("mark", "mark");
        PositionDTO position = new PositionDTO(30, 40);
        
        sailingService.pingMarkViaRaceLogTracking(leaderboard.getName(), columnName, fleet.getName(), mark, position);
    }
}
