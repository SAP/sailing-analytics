package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonDouble;
import org.bson.Document;

import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sse.common.TimePoint;

public class DoubleVectorFixMongoHandlerImpl implements FixMongoHandler<DoubleVectorFix> {
    private final MongoObjectFactoryImpl mof;
    private final DomainObjectFactoryImpl dof;

    public DoubleVectorFixMongoHandlerImpl(MongoObjectFactory mof, DomainObjectFactory dof) {
        this.mof = (MongoObjectFactoryImpl) mof;
        this.dof = (DomainObjectFactoryImpl) dof;
    }

    @Override
    public Document transformForth(DoubleVectorFix fix) throws IllegalArgumentException {
        Document result = new Document();
        mof.storeTimed(fix, result);
        result.put(FieldNames.FIX.name(), toDBObject(fix.get()));
        return result;
    }

    @Override
    public DoubleVectorFix transformBack(Document dbObject) {
        TimePoint timePoint = dof.loadTimePoint(dbObject);
        return new DoubleVectorFixImpl(timePoint, fromDBObject(dbObject.get(FieldNames.FIX.name())));
    }
    
    private BsonArray toDBObject(Double[] data) {
        BsonArray result = new BsonArray();
        for (Double value : data) {
            result.add(value==null?new BsonDouble(0.0):new BsonDouble(value));
        }
        return result;
    }
    
    private Double[] fromDBObject(Object dbObject) {
        @SuppressWarnings("unchecked")
        List<Number> dbValues = (List<Number>) dbObject;
        Double[] result = new Double[dbValues.size()];
        for (int i = 0 ; i < dbValues.size() ; i++) {
            // for backward compatibility, check for null; starting with MongoDB Java driver 4.4.2
            // it became impossible to *write* null values in a BsonArray, however.
            result[i] = dbValues.get(i) == null ? null : dbValues.get(i).doubleValue();
        }
        return result;
    }
}
