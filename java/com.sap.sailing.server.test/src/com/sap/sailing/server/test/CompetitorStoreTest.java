package com.sap.sailing.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.TransientCompetitorStoreImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.server.impl.PersistentCompetitorStore;

public class CompetitorStoreTest {
    @Test
    public void testAddingToTransientStore() {
        CompetitorStore transientStore = new TransientCompetitorStoreImpl();
        DynamicCompetitor template = AbstractLeaderboardTest.createCompetitor("Test Competitor");
        Competitor competitor = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getTeam(), template.getBoat());
        assertTrue(competitor != template);
        Competitor competitor2 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), template.getTeam(), template.getBoat());
        assertSame(competitor, competitor2);
        assertEquals(DomainFactory.INSTANCE.getOrCreateNationality("GER"), competitor.getTeam().getNationality());
        DynamicTeam differentTeam = AbstractLeaderboardTest.createCompetitor("Test Competitor").getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), differentTeam, template.getBoat());
        assertSame(competitor, competitor3); // use existing competitor despite the different team
        assertSame(competitor.getTeam(), competitor3.getTeam());
        
        // now mark the competitor as to update from defaults
        transientStore.allowCompetitorResetToDefaults(competitor);
        Competitor competitor4 = transientStore.getOrCreateCompetitor(template.getId(), template.getName(), differentTeam, template.getBoat());
        assertSame(competitor, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }
    
    @Test
    public void testPersistentCompetitorStore() {
        CompetitorStore persistentStore1 = new PersistentCompetitorStore(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */ true);
        DynamicCompetitor template = AbstractLeaderboardTest.createCompetitor("Test Competitor");
        Competitor competitor = persistentStore1.getOrCreateCompetitor(template.getId(), template.getName(), template.getTeam(), template.getBoat());
        
        CompetitorStore persistentStore2 = new PersistentCompetitorStore(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* clearStore */ false);
        Competitor competitor2 = persistentStore2.getExistingCompetitorById(template.getId());
        assertTrue(competitor2 != template); // the new store loads new instances from the database
        assertEquals(template.getId(), competitor2.getId());
        assertEquals(template.getTeam().getNationality(), competitor2.getTeam().getNationality());
        DynamicTeam differentTeam = AbstractLeaderboardTest.createCompetitor("Test Competitor").getTeam();
        differentTeam.setNationality(DomainFactory.INSTANCE.getOrCreateNationality("GHA")); // Ghana
        Competitor competitor3 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), differentTeam, template.getBoat());
        assertSame(competitor2, competitor3); // use existing competitor despite the different team
        assertTrue(differentTeam != competitor2.getTeam()); // team expected to remain unchanged
        assertEquals(competitor.getTeam().getNationality(), competitor3.getTeam().getNationality()); // no updatability requested; nationality expected to remain unchanged
        
        // now mark the competitor as to update from defaults
        persistentStore2.allowCompetitorResetToDefaults(competitor2);
        Competitor competitor4 = persistentStore2.getOrCreateCompetitor(template.getId(), template.getName(), differentTeam, template.getBoat());
        assertSame(competitor2, competitor4); // expecting an in-place update
        assertEquals(differentTeam.getNationality(), competitor4.getTeam().getNationality());
    }
}
