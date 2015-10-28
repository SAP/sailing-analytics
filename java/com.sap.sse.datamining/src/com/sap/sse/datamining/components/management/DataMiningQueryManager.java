package com.sap.sse.datamining.components.management;

import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.impl.components.management.NullDataMiningQueryManager;
import com.sap.sse.datamining.shared.DataMiningSession;

public interface DataMiningQueryManager {
    
    public static final DataMiningQueryManager NULL = new NullDataMiningQueryManager(); 
    
    <ResultType> QueryResult<ResultType> runNewAndAbortPrevious(DataMiningSession session, Query<ResultType> query);
    
    void abortRandomQuery();
    
    void abortAllQueries();

    int getNumberOfRunningQueries();

}
