package com.sap.sailing.domain.regattalog.tracking.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorDeregistrator;
import com.sap.sailing.domain.abstractlog.shared.analyzing.CompetitorsInLogAnalyzer;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CompetitorDeregistrationTest extends AbstractRegattaLogTrackingTest {
    private final static BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
    private final static DynamicBoat boat1 = new BoatImpl("id12345", "boat1", boatClass, /* sailID */ null);
    private final static DynamicBoat boat2 = new BoatImpl("id12345", "boat1", boatClass, /* sailID */ null);
    private final CompetitorWithBoat competitor = new CompetitorWithBoatImpl("comp", "Comp", "KYC", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, boat1);
    private final CompetitorWithBoat competitor2 = new CompetitorWithBoatImpl("comp2", "Comp2", "KYC", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, boat2);
    
    @Test
    public void testCompetitorDeregistration() {
        log.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), author, competitor));
        log.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), author, competitor2));
        final CompetitorDeregistrator<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> deregistrator = new CompetitorDeregistrator<>(log,
                Collections.singleton(competitor2), author);
        final Set<RegattaLogEvent> events = deregistrator.analyze();
        deregistrator.deregister(events);
        final Set<Competitor> competitors = new CompetitorsInLogAnalyzer<>(log).analyze();
        assertEquals(1, competitors.size());
        assertSame(competitor, competitors.iterator().next());
    }
}
