package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.racelog.tracking.NoCorrespondingServiceRegisteredException;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.racelog.tracking.TypeBasedServiceFinderFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoRaceLogStoreImpl;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.impl.RaceLogEventAuthorImpl;
import com.sap.sailing.domain.racelog.tracking.DeviceCompetitorMappingEvent;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.PlaceHolderDeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockEmptyServiceFinderFactory;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelog.tracking.test.mock.SmartphoneImeiIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;

public class StoreAndLoadDeviceIdentifierTest extends AbstractMongoDBTest {
    public StoreAndLoadDeviceIdentifierTest() throws UnknownHostException, MongoException {
        super();
    }

    protected static final String group = "group";
    protected static final String column = "column";
    protected static final String fleet = "fleet";
    protected final RaceLogEventAuthor author = new RaceLogEventAuthorImpl("author", 0);
    protected RaceLogIdentifier logIdentifier;

    protected MongoObjectFactoryImpl mongoFactory;
    protected DomainObjectFactoryImpl domainFactory;
    protected RaceLog raceLog;
    
    private TimePoint now() {
        return MillisecondsTimePoint.now();
    }
    
    @Before
    public void setup() throws UnknownHostException, MongoException, InterruptedException {
        dropTestDB();

        logIdentifier = mock(RaceLogIdentifier.class);
        Triple<String, String, String> triple = new Triple<String, String, String>("a", "b", UUID.randomUUID().toString());
        when(logIdentifier.getIdentifier()).thenReturn(triple);
        when(logIdentifier.getDeprecatedIdentifier()).thenReturn("");
    }
    
    private RaceLog loadRaceLog() {        
        return new MongoRaceLogStoreImpl(mongoFactory, domainFactory).getRaceLog(logIdentifier, true);
    }
    
    private void createFactories(TypeBasedServiceFinderFactory factory) {
        mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService(), factory);
        domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
                .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE, factory);

        raceLog = loadRaceLog();
    }
    
    private DeviceIdentifier storeAndLoad(DeviceIdentifier device, TypeBasedServiceFinderFactory forStoring, TypeBasedServiceFinderFactory forLoading) {
        createFactories(forStoring);
        Competitor c = new CompetitorImpl("a", "a", null, null, null);
        
        raceLog.add(RaceLogEventFactory.INSTANCE.createDeviceCompetitorMappingEvent(now(), author, device,
                c, 0, now(), now()));
        
        createFactories(forLoading);
        
        RaceLog loadedRaceLog = loadRaceLog();
        loadedRaceLog.lockForRead();
        assertEquals(1, Util.size(loadedRaceLog.getRawFixes()));
        DeviceCompetitorMappingEvent mapping = (DeviceCompetitorMappingEvent) loadedRaceLog.getRawFixes().iterator().next();
        loadedRaceLog.unlockAfterRead();
        
        return mapping.getDevice();
    }
    
    @Test
    public void testNormal() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory factory = new MockSmartphoneImeiServiceFinderFactory();

        DeviceIdentifier smartphone = new SmartphoneImeiIdentifier("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, factory, factory);
        
        assertEquals(smartphone, loaded);
    }
    
    @Test
    public void testNoAppropriateDeserializer() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory forStoring = new MockSmartphoneImeiServiceFinderFactory();
        TypeBasedServiceFinderFactory forLoading = new MockEmptyServiceFinderFactory();

        DeviceIdentifier smartphone = new SmartphoneImeiIdentifier("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, forStoring, forLoading);
        
        assertTrue(loaded instanceof PlaceHolderDeviceIdentifier);
        assertEquals(smartphone.getIdentifierType(), loaded.getIdentifierType());
        assertEquals(smartphone.getStringRepresentation(), loaded.getStringRepresentation());
    }
    
    @Test
    public void testUseStringRepForDeserializing() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory forLoading = new MockSmartphoneImeiServiceFinderFactory();
        TypeBasedServiceFinderFactory forStoring = new MockEmptyServiceFinderFactory();

        DeviceIdentifier smartphone = new SmartphoneImeiIdentifier("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, forStoring, forLoading);
        
        //smartphone imei identifier can be restored from string rep
        assertTrue(loaded instanceof SmartphoneImeiIdentifier);
        assertEquals(smartphone.getIdentifierType(), loaded.getIdentifierType());
        assertEquals(smartphone.getStringRepresentation(), loaded.getStringRepresentation());
    }
    
    @Test
    public void testNoAppropriateSerializer() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory forLoading = new MockSmartphoneImeiServiceFinderFactory();
        TypeBasedServiceFinderFactory forStoring = new MockEmptyServiceFinderFactory();
        
        DeviceIdentifier trackFile = new TrackFileImportDeviceIdentifierImpl("FILE", "track");
        DeviceIdentifier loaded = storeAndLoad(trackFile, forStoring, forLoading);
        assertTrue(loaded instanceof PlaceHolderDeviceIdentifier);
        assertEquals(trackFile.getIdentifierType(), loaded.getIdentifierType());
        assertEquals(trackFile.getStringRepresentation(), loaded.getStringRepresentation());
    }
}
