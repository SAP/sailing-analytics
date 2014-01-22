package com.sap.sailing.datamining.factories;

import java.util.concurrent.ThreadPoolExecutor;

import com.sap.sailing.datamining.ParallelDataRetriever;
import com.sap.sailing.datamining.DataRetrievalWorker;
import com.sap.sailing.datamining.WorkerBuilder;
import com.sap.sailing.datamining.builders.DataRetrieverWorkerBuilder;
import com.sap.sailing.datamining.impl.GroupDividingParallelDataRetriever;
import com.sap.sailing.datamining.shared.DataTypes;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.RacingEventService;

public final class DataRetrieverFactory {

    private DataRetrieverFactory() {
    }

    /**
     * Creates a retriever for the given data type. Throws an exception, if the used <code>DataType</code> doesn't match
     * the <code>DataType</code> of the returning retriever.
     */
    public static <DataType> ParallelDataRetriever<DataType> createDataRetriever(DataTypes dataType, RacingEventService racingService, ThreadPoolExecutor executor) {
        WorkerBuilder<DataRetrievalWorker<LeaderboardGroup, DataType>> workerBuilder = new DataRetrieverWorkerBuilder<DataType>(dataType);
        return new GroupDividingParallelDataRetriever<DataType>(racingService, workerBuilder, executor);
    }

}
