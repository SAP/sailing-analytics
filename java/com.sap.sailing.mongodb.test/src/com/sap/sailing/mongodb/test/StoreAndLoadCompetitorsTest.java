package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;

public class StoreAndLoadCompetitorsTest extends AbstractMongoDBTest {
    private DomainFactory domainFactory;

    private final static BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
    
    public static Competitor createCompetitor(String competitorName) {
        return createCompetitor(competitorName, competitorName);
    }

    public static Competitor createCompetitor(String competitorName, Serializable id) {
        return new CompetitorImpl(id, competitorName, "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                        new PersonImpl(competitorName, new NationalityImpl("GER"),
                        /* dateOfBirth */ new Date(), "This is famous "+competitorName)),
                        new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                        /* dateOfBirth */new Date(), "This is Rigo, the coach")),
                        /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
    }

    public static CompetitorWithBoat createCompetitorWithBoat(String competitorName) {
        Competitor c = createCompetitor(competitorName);
        DynamicBoat b = new BoatImpl("id12345", competitorName + "'s boat", boatClass, /* sailID */ null);
        return new CompetitorWithBoatImpl(c, b);
    }

    public StoreAndLoadCompetitorsTest() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        // clear the domainFactory competitor store for a clean start:
        domainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
    }
    
    private void dropCompetitorAndBoatsCollection() {
        MongoDatabase db = getMongoService().getDB();
        MongoCollection<org.bson.Document> competitorCollection = db.getCollection(CollectionNames.COMPETITORS.name());
        competitorCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop();
        MongoCollection<org.bson.Document> boatsCollection = db.getCollection(CollectionNames.BOATS.name());
        boatsCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop(); // ensure that the drop() has happened
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
        dropCompetitorAndBoatsCollection();
        
        DynamicCompetitor c = (DynamicCompetitor) createCompetitor(competitorName1);
        c.setShortName(competitorShortName1);
        c.setFlagImage(flagImageURI1);
        mongoObjectFactory.storeCompetitor(c);
        
        Collection<DynamicCompetitor> allCompetitors = domainObjectFactory.loadAllCompetitors();
        assertEquals(1, Util.size(allCompetitors));
        DynamicCompetitor loadedCompetitor = (DynamicCompetitor) allCompetitors.iterator().next();
        assertEquals(flagImageURI1, loadedCompetitor.getFlagImage());
        assertEquals(competitorName1, loadedCompetitor.getName());
        assertEquals(competitorShortName1, loadedCompetitor.getShortName());
        assertEquals(c.getTeam().getCoach().getDateOfBirth(), loadedCompetitor.getTeam().getCoach().getDateOfBirth());
        
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
    public void testCompetitorDoesNotAcceptInfiniteTimeOnTimeFactor() {
        final DynamicCompetitor c = (DynamicCompetitor) createCompetitor("Hasso", UUID.randomUUID());
        final double infinity = 100./0.;
        try {
            c.setTimeOnTimeFactor(infinity); // see how we handle a 0 Yardstick number; see bug 5297
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testStoreAndUpdateCompetitorWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorAndBoatsCollection();
        DynamicCompetitor c = (DynamicCompetitor) createCompetitor("Hasso", UUID.randomUUID());
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));
        c.setName("Hasso Plattner");
        final Collection<DynamicCompetitor> reloadedCompetitors = domainObjectFactory.loadAllCompetitors();
        assertEquals(1, reloadedCompetitors.size());
    }

    @Test
    public void testStoreAndRemoveCompetitor() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorAndBoatsCollection();
        DynamicCompetitor c = (DynamicCompetitor) createCompetitor("Hasso");
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));
        mongoObjectFactory.removeCompetitor(c);
        assertEquals(0, Util.size(domainObjectFactory.loadAllCompetitors()));
    }

    @Test
    public void testStoreAndRemoveCompetitorWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropCompetitorAndBoatsCollection();
        
        DynamicCompetitor c = (DynamicCompetitor) createCompetitor("Hasso", UUID.randomUUID());
        mongoObjectFactory.storeCompetitor(c);
        assertEquals(1, Util.size(domainObjectFactory.loadAllCompetitors()));

        mongoObjectFactory.removeCompetitor(c);
        assertEquals(0, Util.size(domainObjectFactory.loadAllCompetitors()));
    }
}
