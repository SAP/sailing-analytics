package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.RacingProcedureFactoryImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;

public class RacingProceduresWithStartModeFlagTest {
    private RacingProcedureFactoryImpl racingProcedureFactory;
    private RaceLogImpl raceLog;

    @BeforeEach
    public void setUp() {
        this.racingProcedureFactory = new RacingProcedureFactoryImpl(/* author */ null, new EmptyRegattaConfiguration());
        this.raceLog = new RaceLogImpl(UUID.randomUUID());
    }
    
    @Test
    public void testRacingProcedureWithStartModeFlagDetection() {
        for (final RacingProcedureType type : RacingProcedureType.values()) {
            if (type != RacingProcedureType.UNKNOWN) {
                final ReadonlyRacingProcedure racingProcedure = racingProcedureFactory.createRacingProcedure(type, raceLog, /* raceLogResolver */ null);
                assertEquals(RacingProcedureType.RRS26.equals(type) || RacingProcedureType.RRS26_3MIN.equals(type) || RacingProcedureType.SWC.equals(type) || RacingProcedureType.SWC_4MIN.equals(type),
                        racingProcedure instanceof ConfigurableStartModeFlagRacingProcedure,
                        "Racing procedure type "+type.name()+" was "+((racingProcedure instanceof ConfigurableStartModeFlagRacingProcedure) ? "" : "not ")+
                                "considered one with configurable start mode flag but should have");
            }
        }
    }
}
