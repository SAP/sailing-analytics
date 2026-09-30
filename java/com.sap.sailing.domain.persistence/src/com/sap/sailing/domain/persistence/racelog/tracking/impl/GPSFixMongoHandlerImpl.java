package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import org.bson.Document;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import static com.sap.sailing.shared.persistence.impl.DomainObjectFactoryImpl.loadPosition;

import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class GPSFixMongoHandlerImpl implements FixMongoHandler<GPSFix> {
    private final MongoObjectFactoryImpl mof;
    private final DomainObjectFactoryImpl dof;

    public GPSFixMongoHandlerImpl(MongoObjectFactory mof, DomainObjectFactory dof) {
        this.mof = (MongoObjectFactoryImpl) mof;
        this.dof = (DomainObjectFactoryImpl) dof;
    }

    @Override
    public Document transformForth(GPSFix fix) throws IllegalArgumentException {
        Document result = new Document();
        mof.storeTimed(fix, result);
        mof.storePositioned(fix, result);
        return result;
    }

    @Override
    public GPSFix transformBack(Document object) {
        Document dbObject = (Document) object;
        TimePoint timePoint = dof.loadTimePoint(dbObject);
        Position position = loadPosition(dbObject);
        return new GPSFixImpl(position, timePoint);
    }

}
