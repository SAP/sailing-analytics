package com.sap.sailing.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.TransientCompetitorStoreImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.server.impl.PersistentCompetitorStore;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.RGBColor;

public class CompetitorStoreTest {
    @Test
    public void testAddingCompetitorsToTransientStore() {
        CompetitorStore transientStore = new TransientCompetitorStoreImpl();
        DynamicCompetitor template = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat("Test Competitor").getCompetitor();
        Competitor competitor = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.234, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(730), null);
        assertTrue(competitor != template);
        Competitor competitor2 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.345,
                /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(750.2), null);
        assertSame(competitor, competitor2);
        assertEquals(DomainFactory.INSTANCE.getOrCreateNationality("GER"), competitor.getTeam().getNationality());
        DynamicTeam differentTeam = (DynamicTeam) AbstractLeaderboardTest.createCompetitorAndBoat("Test Competitor").getCompetitor().getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null);
        assertSame(competitor, competitor3); // use existing competitor despite the different team
        assertSame(competitor.getTeam(), competitor3.getTeam());
        
        // now mark the competitor as to update from defaults
        transientStore.allowCompetitorResetToDefaults(competitor);
        Competitor competitor4 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(), 
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null);
        assertSame(competitor, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }
    
    @Test
    public void testPersistentCompetitorStore() {
        CompetitorStore persistentStore1 = new PersistentCompetitorStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */true, null, /* raceLogResolver */ (srlid)->null);
        DynamicCompetitor template = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat("Test Competitor").getCompetitor();
        Competitor competitor = persistentStore1.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), template.getTeam(),
                /* timeOnTimeFactor */ 1.234, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ Duration.ONE_SECOND.times(730), null);

        CompetitorStore persistentStore2 = new PersistentCompetitorStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */false, null, /* raceLogResolver */ (srlid)->null);
        Competitor competitor2 = persistentStore2.getExistingCompetitorById(template.getId());
        assertNotSame(competitor2, template); // the new store loads new instances from the database
        assertEquals(template.getId(), competitor2.getId());
        assertEquals(template.getTeam().getNationality(), competitor2.getTeam().getNationality());
        assertEquals(1.234, competitor2.getTimeOnTimeFactor(), 0.0000001);
        assertEquals(730, competitor2.getTimeOnDistanceAllowancePerNauticalMile().asSeconds(), 0.0000001);

        DynamicTeam differentTeam = (DynamicTeam) AbstractLeaderboardTest.createCompetitorAndBoat("Test Competitor").getCompetitor().getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null);
        assertSame(competitor2, competitor3); // use existing competitor despite the different team
        assertNotSame(differentTeam, competitor2.getTeam()); // team expected to remain unchanged
        assertEquals(competitor.getTeam().getNationality(), competitor3.getTeam().getNationality()); // no updatability requested; nationality
                                                                                                     // expected to remain unchanged
        // now mark the competitor as to update from defaults
        persistentStore2.allowCompetitorResetToDefaults(competitor2);
        Competitor competitor4 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), template.getShortName(),
                template.getColor(), template.getEmail(), template.getFlagImage(), differentTeam, /* timeOnTimeFactor */
                null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */null, null);
        assertSame(competitor2, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }
    
    @Test
    public void testPersistentBoatStore() {
        CompetitorStore persistentStore1 = new PersistentCompetitorStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */true, null, /* raceLogResolver */ (srlid)->null);
        DynamicBoat template = new BoatImpl("id-12345", "Morning Glory", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), 
                "GER 1234", new RGBColor(255, 0, 0));
        DynamicBoat boat = (DynamicBoat) persistentStore1.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                template.getSailID(), template.getColor());

        CompetitorStore persistentStore2 = new PersistentCompetitorStore(
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */false, null, /* raceLogResolver */ (srlid)->null);
        Boat boat2 = persistentStore2.getExistingBoatById(template.getId());
        assertNotSame(boat2, template); // the new store loads new instances from the database
        assertEquals(template.getId(), boat2.getId());

        String differentSailId = "USA 1234";
        Boat boat3 = persistentStore2.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                differentSailId, template.getColor());
        assertSame(boat2, boat3); // use existing boat despite the different sailID
        assertNotSame(differentSailId, boat2.getSailID()); // sailID expected to remain unchanged

        // now mark the boat as to update from defaults
        persistentStore2.allowBoatResetToDefaults(boat2);
        Boat boat4 = persistentStore2.getOrCreateBoat(template.getId(), template.getName(), template.getBoatClass(), 
                differentSailId, template.getColor());
        assertSame(boat2, boat4); // expecting an in-place update
        assertEquals(differentSailId, boat4.getSailID());
    }
}
