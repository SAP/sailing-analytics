package com.sap.sailinig.domain.based.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.SharedDomainFactoryImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;

/**
 * Tests creation and caching of boat classes. See also bug 3347
 * (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3347).
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class BoatClassCacheTest {
    private SharedDomainFactory<RaceLogResolver> sharedDomainFactory;
    
    @BeforeEach
    public void setUp() {
        sharedDomainFactory = new SharedDomainFactoryImpl<>(/* raceLogResolver */ null);
    }
    
    @Test
    public void testQueryingBoatClassByDisplayNameFirstThenCanonicalizedAlternativeName() {
        final BoatClassMasterdata laserMasterData = BoatClassMasterdata.LASER_INT;
        final String laserAlternativeName = laserMasterData.getAlternativeNames()[0];
        final String unifiedAlternativeName = BoatClassMasterdata.unifyBoatClassName(laserAlternativeName);
        final BoatClass laserByDisplayName = sharedDomainFactory.getOrCreateBoatClass(laserMasterData.getDisplayName());
        final BoatClass laserByUnifiedAlternativeName = sharedDomainFactory.getOrCreateBoatClass(unifiedAlternativeName);
        assertSame(laserByDisplayName, laserByUnifiedAlternativeName);
    }

    @Test
    public void testQueryingBoatClassByAlternativeNameFirstThenCanonicalizedDisplayName() {
        final BoatClassMasterdata laserMasterData = BoatClassMasterdata.LASER_INT;
        final String laserAlternativeName = laserMasterData.getAlternativeNames()[0];
        final BoatClass laserByAlternativeName = sharedDomainFactory.getOrCreateBoatClass(laserAlternativeName);
        final String unifiedDisplayName = BoatClassMasterdata.unifyBoatClassName(laserMasterData.getDisplayName());
        final BoatClass laserByUnifiedDisplayName = sharedDomainFactory.getOrCreateBoatClass(unifiedDisplayName);
        assertSame(laserByAlternativeName, laserByUnifiedDisplayName);
    }

    @Test
    public void testQueryingBoatClassByDisplayNameFirstThenCanonicalizedDisplayName() {
        final BoatClassMasterdata laserMasterData = BoatClassMasterdata.LASER_INT;
        final BoatClass laserByDisplayName = sharedDomainFactory.getOrCreateBoatClass(laserMasterData.getDisplayName());
        final String unifiedDisplayName = BoatClassMasterdata.unifyBoatClassName(laserMasterData.getDisplayName());
        final BoatClass laserByUnifiedDisplayName = sharedDomainFactory.getOrCreateBoatClass(unifiedDisplayName);
        assertSame(laserByDisplayName, laserByUnifiedDisplayName);
    }

    @Test
    public void testQueryingBoatClassByAlternativeNameFirstThenCanonicalizedAlternativeName() {
        final BoatClassMasterdata laserMasterData = BoatClassMasterdata.LASER_INT;
        final String laserAlternativeName = laserMasterData.getAlternativeNames()[0];
        final BoatClass laserByAlternativeName = sharedDomainFactory.getOrCreateBoatClass(laserAlternativeName);
        final String unifiedAlternativeName = BoatClassMasterdata.unifyBoatClassName(laserAlternativeName);
        final BoatClass laserByUnifiedAlternativeName = sharedDomainFactory.getOrCreateBoatClass(unifiedAlternativeName);
        assertSame(laserByAlternativeName, laserByUnifiedAlternativeName);
    }
    
    @Test
    public void testEssMayHaveNonUpwindStart() {
        for (final String boatClassName : new String[] { "extreme40", "ess", "ess40" }) {
            final BoatClass ess40 = sharedDomainFactory.getOrCreateBoatClass(boatClassName);
            assertFalse(ess40.typicallyStartsUpwind(), "Boat class "+boatClassName+" expected to allow for non-upwind starts");
        }
    }
}
