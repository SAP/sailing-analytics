package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import static com.sap.sailing.shared.persistence.impl.DomainObjectFactoryImpl.loadPosition;

import org.bson.Document;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public class GPSFixMovingMongoHandlerImpl implements FixMongoHandler<GPSFixMoving> {
    private final MongoObjectFactoryImpl mof;
    private final DomainObjectFactoryImpl dof;

    public GPSFixMovingMongoHandlerImpl(MongoObjectFactory mof, DomainObjectFactory dof) {
        this.mof = (MongoObjectFactoryImpl) mof;
        this.dof = (DomainObjectFactoryImpl) dof;
    }

    @Override
    public Document transformForth(GPSFixMoving fix) throws IllegalArgumentException {
        Document result = new Document();
        mof.storeTimed(fix, result);
        mof.storePositioned(fix, result);
        mof.storeSpeedWithBearing(fix.getSpeed(), result);
        mof.storeOptionalTrueHeading(fix.getOptionalTrueHeading(), result);
        return result;
    }

    @Override
    public GPSFixMoving transformBack(Document dbObject) {
        final TimePoint timePoint = dof.loadTimePoint(dbObject);
        final Position position = loadPosition(dbObject);
        final SpeedWithBearing speed = dof.loadSpeedWithBearing(dbObject);
        final Bearing optionalTrueHeading = dof.loadOptionalTrueHeading(dbObject);
        return new GPSFixMovingImpl(position, timePoint, speed, optionalTrueHeading);
    }

}
