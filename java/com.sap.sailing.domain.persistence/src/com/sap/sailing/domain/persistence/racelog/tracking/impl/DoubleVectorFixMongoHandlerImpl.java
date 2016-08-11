package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import java.util.List;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.FieldNames;
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
    public DBObject transformForth(DoubleVectorFix fix) throws IllegalArgumentException {
        DBObject result = new BasicDBObject();
        mof.storeTimed(fix, result);
        result.put(FieldNames.FIX.name(), toDBObject(fix.get()));
        return result;
    }

    @Override
    public DoubleVectorFix transformBack(DBObject dbObject) {
        TimePoint timePoint = dof.loadTimePoint(dbObject);
        return new DoubleVectorFixImpl(timePoint, fromDBObject((DBObject) dbObject.get(FieldNames.FIX.name())));
    }
    
    private DBObject toDBObject(double[] data) {
        BasicDBList result = new BasicDBList();
        for (double value : data) {
            result.add(value);
        }
        return result;
    }
    
    private double[] fromDBObject(DBObject dbObject) {
        @SuppressWarnings("unchecked")
        List<Double> dbValues = (List<Double>) dbObject;
        double[] result = new double[dbValues.size()];
        for(int i = 0 ; i < dbValues.size() ; i++) {
            result[i] = dbValues.get(i);
        }
        return result;
    }
}
