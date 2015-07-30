package com.sap.sse.datamining.shared;

import java.io.Serializable;
import java.util.Map;

import com.sap.sse.datamining.shared.data.QueryResultState;
import com.sap.sse.datamining.shared.data.Unit;

public interface QueryResult<ResultType> extends Serializable {
    
    public QueryResultState getState();
    public Class<ResultType> getResultType();
    
    public int getRetrievedDataAmount();
    public double getCalculationTimeInSeconds();

    /**
     * @return a description what kind of results are contained.
     */
    public String getResultSignifier();
    
    public Unit getUnit();
    public String getUnitSignifier();
    public int getValueDecimals();

    public boolean isEmpty();
    public Map<GroupKey, ResultType> getResults();
    
}