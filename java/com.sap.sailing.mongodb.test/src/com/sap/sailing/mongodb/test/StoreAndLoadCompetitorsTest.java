package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sse.common.Util;

public class StoreAndLoadCompetitorsTest extends AbstractMongoDBTest {
    private DomainFactory domainFactory;
    
    public StoreAndLoadCompetitorsTest() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    public void setUp() {
        // clear the domainFactory competitor store for a clean start:
        domainFactory = new DomainFactoryImpl((srlid)->null);
    }
    
    private void dropCompetitorCollection() {
        DB db = getMongoService().getDB();
        DBCollection competitorCollection = db.getCollection(CollectionNames.COMPETITORS.name());
        competitorCollection.setWriteConcern(WriteConcern.SAFE); // ensure that the drop() has happened
        competitorCollection.drop();
    }
    
    @Test
    public void testStoreAndUpdateCompetitor() throws URISyntaxException {
        URI flagImageURI1 = new URI("http://www.sapsailing/flagimage1.jpg");
        URI flagImageURI2 = new URI("http://www.sapsailing/flagimage2.jpg");
        String competitorName1 = "Hasso";
        String competitorName2 = "Hasso Plattner";
        String competitorShortName1 = "H.";
        String competitorShortName2 = "H.P.";

        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorCollection();
        
        DynamicCompetitor c = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat(competitorName1).getCompetitor();
        c.setShortName(competitorShortName1);
        c.setFlagImage(flagImageURI1);
        mongoObjectFactory.storeCompetitor(c);
        
        Collection<CompetitorWithBoat> allCompetitors = domainObjectFactory.loadAllCompetitors();
        assertEquals(1, Util.size(allCompetitors));
        DynamicCompetitor loadedCompetitor = (DynamicCompetitor) allCompetitors.iterator().next();
        assertEquals(flagImageURI1, loadedCompetitor.getFlagImage());
        assertEquals(competitorName1, loadedCompetitor.getName());
        assertEquals(competitorShortName1, loadedCompetitor.getShortName());
        
        loadedCompetitor.setName(competitorName2);
        loadedCompetitor.setShortName(competitorShortName2);
        loadedCompetitor.setFlagImage(flagImageURI2);
        mongoObjectFactory.storeCompetitor(loadedCompetitor);

        allCompetitors = domainObjectFactory.loadAllCompetitors();
        assertEquals(1, Util.size(allCompetitors));
        loadedCompetitor = (DynamicCompetitor) allCompetitors.iterator().next();
        assertEquals(flagImageURI2, loadedCompetitor.getFlagImage());
        assertEquals(competitorName2, loadedCompetitor.getName());
        assertEquals(competitorShortName2, loadedCompetitor.getShortName());
    }

    @Test
    public void testStoreAndUpdateCompetitorWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorCollection();

        DynamicCompetitor c = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat("Hasso", UUID.randomUUID()).getCompetitor();
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));
        c.setName("Hasso Plattner");
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));
    }

    @Test
    public void testStoreAndRemoveCompetitor() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorCollection();
        
        DynamicCompetitor c = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat("Hasso").getCompetitor();
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));

        mongoObjectFactory.removeCompetitor(c);
        assertEquals(0, Util.size(domainObjectFactory.loadAllCompetitors()));
    }

    @Test
    public void testStoreAndRemoveCompetitorWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorCollection();
        
        DynamicCompetitor c = (DynamicCompetitor) AbstractLeaderboardTest.createCompetitorAndBoat("Hasso", UUID.randomUUID()).getCompetitor();
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));

        mongoObjectFactory.removeCompetitor(c);
        assertEquals(0, Util.size(domainObjectFactory.loadAllCompetitors()));
    }
}
