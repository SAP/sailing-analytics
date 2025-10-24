package com.sap.sailing.landscape;

import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;

/**
 * Determines or launches an {@link ApplicationProcessHost} that is
 * {@link AwsApplicationReplicaSet#isEligibleForDeployment(com.sap.sse.landscape.aws.ApplicationProcessHost, java.util.Optional, java.util.Optional, byte[])
 * eligible} for having a master or replica of a specific {@link AwsApplicationReplicaSet} deployed to it.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface EligibleInstanceForReplicaSetFindingStrategy {
    SailingAnalyticsHost<String> getInstanceToDeployTo(AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws Exception;
}
