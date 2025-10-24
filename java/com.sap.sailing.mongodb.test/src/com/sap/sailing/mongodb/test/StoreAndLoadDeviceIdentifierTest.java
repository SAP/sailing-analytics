package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoRegattaLogStoreImpl;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockEmptyServiceFinderFactory;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.PlaceHolderDeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StoreAndLoadDeviceIdentifierTest extends AbstractMongoDBTest {
    public StoreAndLoadDeviceIdentifierTest() throws UnknownHostException, MongoException {
        super();
    }

    protected static final String group = "group";
    protected static final String column = "column";
    protected static final String fleet = "fleet";
    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("author", 0);
    protected RegattaLikeIdentifier logIdentifier;

    protected MongoObjectFactoryImpl mongoFactory;
    protected DomainObjectFactoryImpl domainFactory;
    protected RegattaLog regattaLog;
    
    private TimePoint now() {
        return MillisecondsTimePoint.now();
    }
    
    @BeforeEach
    public void setup() throws UnknownHostException, MongoException, InterruptedException {
        dropTestDB();

        logIdentifier = mock(RegattaLikeIdentifier.class);
    }
    
    private RegattaLog loadRegattaLog() {        
        return new MongoRegattaLogStoreImpl(mongoFactory, domainFactory).getRegattaLog(logIdentifier, true);
    }
    
    private void createFactories(TypeBasedServiceFinderFactory factory) {
        mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
                .getMongoObjectFactory(getMongoService(), factory);
        domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
                .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE, factory);
        regattaLog = loadRegattaLog();
    }
    
    private DeviceIdentifier storeAndLoad(DeviceIdentifier device, TypeBasedServiceFinderFactory forStoring, TypeBasedServiceFinderFactory forLoading) {
        createFactories(forStoring);
        Competitor c = DomainFactory.INSTANCE.getOrCreateCompetitor("a", "a", "a", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(now(), now(), author, 0,
                c, device, now(), now()));
        createFactories(forLoading);
        RegattaLog loadedRegattaLog = loadRegattaLog();
        loadedRegattaLog.lockForRead();
        assertEquals(1, Util.size(loadedRegattaLog.getRawFixes()));
        RegattaLogDeviceCompetitorMappingEvent mapping = (RegattaLogDeviceCompetitorMappingEvent) loadedRegattaLog.getRawFixes().iterator().next();
        loadedRegattaLog.unlockAfterRead();
        return mapping.getDevice();
    }
    
    @Test
    public void testNormal() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory factory = new MockSmartphoneImeiServiceFinderFactory();
        DeviceIdentifier smartphone = new SmartphoneImeiIdentifierImpl("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, factory, factory);
        assertEquals(smartphone, loaded);
    }
    
    @Test
    public void testNoAppropriateDeserializer() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory forStoring = new MockSmartphoneImeiServiceFinderFactory();
        TypeBasedServiceFinderFactory forLoading = new MockEmptyServiceFinderFactory();

        DeviceIdentifier smartphone = new SmartphoneImeiIdentifierImpl("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, forStoring, forLoading);
        
        assertTrue(loaded instanceof PlaceHolderDeviceIdentifier);
        assertEquals(smartphone.getIdentifierType(), loaded.getIdentifierType());
        assertEquals(smartphone.getStringRepresentation(), loaded.getStringRepresentation());
    }
    
    @Test
    public void testUseStringRepForDeserializing() throws TransformationException, NoCorrespondingServiceRegisteredException {
        TypeBasedServiceFinderFactory forLoading = new MockSmartphoneImeiServiceFinderFactory();
        TypeBasedServiceFinderFactory forStoring = new MockEmptyServiceFinderFactory();

        DeviceIdentifier smartphone = new SmartphoneImeiIdentifierImpl("abc");
        DeviceIdentifier loaded = storeAndLoad(smartphone, forStoring, forLoading);
        
        //smartphone imei identifier can be restored from string rep
        assertTrue(loaded instanceof SmartphoneImeiIdentifierImpl);
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
