package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.TransientCompetitorAndBoatStoreImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.server.impl.PersistentCompetitorAndBoatStore;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.RGBColor;

public class CompetitorStoreTest {
    
    @Test
    public void testAddingCompetitorsToTransientStore() {
        CompetitorAndBoatStore transientStore = new TransientCompetitorAndBoatStoreImpl();
        DynamicCompetitor template = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitor("Test Competitor");
        Competitor competitor = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.234, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(730), null, /* storePersistently */ true);
        assertTrue(competitor != template);
        Competitor competitor2 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.345,
                /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(750.2), null, /* storePersistently */ true);
        assertSame(competitor, competitor2);
        assertEquals(DomainFactory.INSTANCE.getOrCreateNationality("GER"), competitor.getTeam().getNationality());
        DynamicTeam differentTeam = (DynamicTeam) AbstractLeaderboardTest.createCompetitor("Test Competitor").getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, /* storePersistently */ true);
        assertSame(competitor, competitor3); // use existing competitor despite the different team
        assertSame(competitor.getTeam(), competitor3.getTeam());
        
        // now mark the competitor as to update from defaults
        transientStore.allowCompetitorResetToDefaults(competitor);
        Competitor competitor4 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, /* storePersistently */ true);
        assertSame(competitor, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }
    
    @Test
    public void testPersistentCompetitorStore() {
        CompetitorAndBoatStore persistentStore1 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */true, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        DynamicCompetitor template = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitor("Test Competitor");
        Competitor competitor = persistentStore1.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.234, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(730), null, /* storePersistently */ true);

        CompetitorAndBoatStore persistentStore2 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */false, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        Competitor competitor2 = persistentStore2.getExistingCompetitorById(template.getId());
        assertNotSame(competitor2, template); // the new store loads new instances from the database
        assertEquals(template.getId(), competitor2.getId());
        assertEquals(template.getTeam().getNationality(), competitor2.getTeam().getNationality());
        assertEquals(1.234, competitor2.getTimeOnTimeFactor(), 0.0000001);
        assertEquals(730, competitor2.getTimeOnDistanceAllowancePerNauticalMile().asSeconds(), 0.0000001);

        DynamicTeam differentTeam = (DynamicTeam) AbstractLeaderboardTest.createCompetitor("Test Competitor").getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, /* storePersistently */ true);
        assertSame(competitor2, competitor3); // use existing competitor despite the different team
        assertNotSame(differentTeam, competitor2.getTeam()); // team expected to remain unchanged
        assertEquals(competitor.getTeam().getNationality(), competitor3.getTeam().getNationality()); // no updatability requested; nationality
                                                                                                     // expected to remain unchanged
        // now mark the competitor as to update from defaults
        persistentStore2.allowCompetitorResetToDefaults(competitor2);
        Competitor competitor4 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, /* storePersistently */ true);
        assertSame(competitor2, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }

    @Test
    public void testPersistentCompetitorWithBoatStore() {
        CompetitorAndBoatStore persistentStore1 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */true, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        DynamicCompetitorWithBoat template = (DynamicCompetitorWithBoat) AbstractLeaderboardTest.createCompetitorWithBoat("Test Competitor");
        CompetitorWithBoat competitor = persistentStore1.getOrCreateCompetitorWithBoat(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.234, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(730), null, template.getBoat(), /* storePersistently */ true);

        CompetitorAndBoatStore persistentStore2 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */false, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        CompetitorWithBoat competitor2 = persistentStore2.getExistingCompetitorWithBoatById(template.getId());
        assertNotSame(competitor2, template); // the new store loads new instances from the database
        assertEquals(template.getId(), competitor2.getId());
        assertEquals(template.getTeam().getNationality(), competitor2.getTeam().getNationality());
        assertEquals(1.234, competitor2.getTimeOnTimeFactor(), 0.0000001);
        assertEquals(730, competitor2.getTimeOnDistanceAllowancePerNauticalMile().asSeconds(), 0.0000001);

        DynamicTeam differentTeam = (DynamicTeam) AbstractLeaderboardTest.createCompetitorWithBoat("Test Competitor").getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        DynamicCompetitorWithBoat competitor3 = persistentStore2.getOrCreateCompetitorWithBoat(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, template.getBoat(), /* storePersistently */ true);
        assertSame(competitor2, competitor3); // use existing competitor despite the different team
        assertNotSame(differentTeam, competitor2.getTeam()); // team expected to remain unchanged
        assertEquals(competitor.getTeam().getNationality(), competitor3.getTeam().getNationality()); // no updatability requested; nationality
                                                                                                     // expected to remain unchanged
        // now mark the competitor as to update from defaults
        persistentStore2.allowCompetitorResetToDefaults(competitor2);
        CompetitorWithBoat competitor4 = persistentStore2.getOrCreateCompetitorWithBoat(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null, competitor3.getBoat(), /* storePersistently */ true);
        assertSame(competitor2, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }

    @Test
    public void testPersistentBoatStore() {
        CompetitorAndBoatStore persistentStore1 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */true, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        DynamicBoat template = new BoatImpl("id-12345", "Morning Glory", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), 
                "GER 1234", new RGBColor(255, 0, 0));
        persistentStore1.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                template.getSailID(), template.getColor(), /* storePersistently */ true);

        CompetitorAndBoatStore persistentStore2 = new PersistentCompetitorAndBoatStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */false, null, /* raceLogResolver */ DomainFactory.TEST_RACE_LOG_RESOLVER);
        DynamicBoat boat2 = persistentStore2.getExistingBoatById(template.getId());
        assertNotSame(boat2, template); // the new store loads new instances from the database
        assertEquals(template.getId(), boat2.getId());

        String differentSailId = "USA 1234";
        Boat boat3 = persistentStore2.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                differentSailId, template.getColor(), /* storePersistently */ true);
        assertSame(boat2, boat3); // use existing boat despite the different sailID
        assertNotSame(differentSailId, boat2.getSailID()); // sailID expected to remain unchanged

        // now mark the boat as to update from defaults
        persistentStore2.allowBoatResetToDefaults(boat2);
        Boat boat4 = persistentStore2.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                differentSailId, template.getColor(), /* storePersistently */ true);
        assertSame(boat2, boat4); // expecting an in-place update
        assertEquals(differentSailId, boat4.getSailID());
    }
}
