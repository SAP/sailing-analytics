package com.sap.sailing.landscape.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.landscape.EligibleInstanceForReplicaSetFindingStrategy;
import com.sap.sailing.landscape.LandscapeService;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.AvailabilityZone;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.impl.AwsRegion;

import software.amazon.awssdk.services.ec2.model.InstanceType;

/**
 * A strategy for finding or launching a host that is eligible for receiving an application process deployment belonging
 * to an {@link AwsApplicationReplicaSet application replica set}. The
 * {@link #EligbleInstanceForReplicaSetFindingStrategyImpl(LandscapeService, AwsRegion, String, byte[], boolean, boolean, Optional, Optional)
 * constructor} accepts a few options that control the selection / creation process.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class EligibleInstanceForReplicaSetFindingStrategyImpl implements EligibleInstanceForReplicaSetFindingStrategy {
    private static final Logger logger = Logger.getLogger(EligibleInstanceForReplicaSetFindingStrategyImpl.class.getName());

    private final LandscapeService landscapeService;
    private final AwsRegion region;
    private final String optionalKeyName;
    private final byte[] privateKeyEncryptionPassphrase;
    
    /**
     * If {@code true}, means that we're looking for an instance eligible for master deployment. This may
     * influence the ranking based on availability zone (AZ) because we should prefer hosts in an AZ different
     * from that of replicas, especially unmanaged replicas.
     */
    private final boolean master;
    
    /**
     * If {@code true}, hosts that otherwise seem eligible will be rejected if they are in the same availability
     * zone (AZ) as the respective master/replica counterpart. A master then may not be deployed to an AZ if
     * there is only one replica running and it is an unmanaged replica and runs in that AZ. Conversely, a replica
     * then may not be deployed to an AZ where the replica set's master is currently running.
     */
    private final boolean mustBeDifferentAvailabilityZone;
    
    /**
     * If a new instance must be launched in order to obtain an eligible instance for process deployment for a
     * given replica set, this is the instance type to use.
     */
    private final InstanceType instanceType;
    
    /**
     * If {@link Optional#isPresent() present}, specifies a preferred host for the answer given by
     * {@link #getInstanceToDeployTo(AwsApplicationReplicaSet)}. However, if the instance turns out not to be eligible
     * by the rules regarding general
     * {@link AwsApplicationReplicaSet#isEligibleForDeployment(ApplicationProcessHost, Optional, Optional, byte[])
     * eligibility} (based mainly on port and directory available as well as not being managed by an auto-scaling group)
     * and additional rules regarding availability zone "anti-affinity" as defined here (see {@link #master}), the
     * default rules for selecting or launching an eligible instance apply.
     */
    private final Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo;
    
    /**
     * @param region
     *            the region in which to look for or launch an eligible instance
     * @param master
     *            if {@code true} then we're searching for an instance eligible for deployment of a master process,
     *            otherwise of a replica process; this influences how this strategy scans for the availability zones
     *            already used and hence which instances to prefer or where to launch a new one
     * @param mustBeDifferentAvailabilityZone
     *            if {@ode true}, an instance will only be considered eligible for deploying a process to it if it is in
     *            a different availability zone than its master/replica counterpart; for example, when looking for an
     *            instance to which to deploy an unmanaged replica and this parameter is set to {@code true} then
     *            instances are only eligible if they are not in the same availability zone as the instance hosting the
     *            replica set's master process.
     * @param optionalInstanceType
     *            if a new instance must be launched because no eligible one is found, this parameter can be used to
     *            specify its instance type. It defaults to
     *            {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME} which is reasonably suited for a
     *            multi-process set-up.
     * @param optionalPreferredInstanceToDeployTo
     *            If {@link Optional#isPresent() present}, specifies a preferred host for the answer given by
     *            {@link #getInstanceToDeployTo(AwsApplicationReplicaSet)}. However, if the instance turns out not to be
     *            eligible by the rules regarding general
     *            {@link AwsApplicationReplicaSet#isEligibleForDeployment(ApplicationProcessHost, Optional, Optional, byte[])
     *            eligibility} (based mainly on port and directory available as well as not being managed by an
     *            auto-scaling group) and additional rules regarding availability zone "anti-affinity" as defined here
     *            (see {@link #master}), the default rules for selecting or launching an eligible instance apply.
     */
    public EligibleInstanceForReplicaSetFindingStrategyImpl(LandscapeService landscapeService, AwsRegion region,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, boolean master,
            boolean mustBeDifferentAvailabilityZone, Optional<InstanceType> optionalInstanceType,
            Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo) {
        super();
        this.landscapeService = landscapeService;
        this.region = region;
        this.optionalKeyName = optionalKeyName;
        this.privateKeyEncryptionPassphrase = privateKeyEncryptionPassphrase;
        this.master = master;
        this.mustBeDifferentAvailabilityZone = mustBeDifferentAvailabilityZone;
        this.instanceType = optionalInstanceType.orElse(InstanceType.valueOf(SharedLandscapeConstants.DEFAULT_SHARED_INSTANCE_TYPE_NAME));
        this.optionalPreferredInstanceToDeployTo = optionalPreferredInstanceToDeployTo;
    }

    @Override
    public SailingAnalyticsHost<String> getInstanceToDeployTo(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws Exception {
        final Integer optionalIgtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        return optionalPreferredInstanceToDeployTo.map(host->{
            logger.info("Checking preferred instance "+host+" for eligibility");
            try {
                return landscapeService.isEligibleForDeployment(host, replicaSet.getServerName(), replicaSet.getPort(), optionalIgtimiRiotPort,
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase)
                    && isAcceptableAvailabilityZone(host.getAvailabilityZone(), replicaSet) ? host : null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).orElseGet(()->{
            logger.info("Preferred instance not specified or not eligible. Computing default...");
            final Optional<SailingAnalyticsHost<String>> bestExistingCandidate =
                Util.stream(landscapeService.getEligibleSharedHostsForReplicaSet(region, replicaSet, optionalKeyName, privateKeyEncryptionPassphrase))
                    .filter(host->{
                        try {
                            return !isArchiveServer(host) && isAcceptableAvailabilityZone(host.getAvailabilityZone(), replicaSet);
                        } catch (InterruptedException | ExecutionException e) { 
                            logger.log(Level.SEVERE, "Exception while trying to filter eligible hosts", e);
                            return false;
                        }
                    })
                    .sorted(getEligibleHostRanking(replicaSet)).findFirst();
            bestExistingCandidate.ifPresent(bec->logger.info("Found best existing candidate "+bec));
            return bestExistingCandidate.orElseGet(()->{
                try {
                    logger.info("No existing candidate found. Launching new instance of type "+instanceType+" for replica set "+replicaSet.getName());
                    return landscapeService.createEmptyMultiServer(region, Optional.of(instanceType), getPreferredAvailabilityZone(replicaSet),
                            Optional.of(SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_DEFAULT_NAME), Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private Optional<AwsAvailabilityZone> getPreferredAvailabilityZone(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) {
        return Util.stream(Arrays.asList(region.getAvailabilityZones())).filter(az->{
            try {
                return isAcceptableAvailabilityZone(az, replicaSet);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).findAny();
    }

    /**
     * Returns a comparator for hosts that considers "better-suited" ones lesser than "worse-suited" ones. Therefore,
     * the first element of a sequence ordered by this comparator will be the best-suited ("most eligible") host of the
     * sequence.
     * <p>
     * 
     * The comparator considers the availability zones as follows: if {@link #mustBeDifferentAvailabilityZone} is not
     * set, one of the two instances to compare may be in an AZ that would during a strict check not be considered
     * eligible. Such an instance is then considered worse than one that is in an eligible AZ.
     * <p>
     * 
     * Next, the comparator considers the number of application processes already running on the hosts. The host with
     * fewer processes is considered better. For performance reasons, the number of
     * {@link ApplicationProcessHost#getApplicationProcesses(Optional, Optional, byte[]) processes}
     * is cached by the comparator.
     */
    private Comparator<? super SailingAnalyticsHost<String>> getEligibleHostRanking(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) {
        final ConcurrentMap<SailingAnalyticsHost<String>, Integer> numberOfProcesses = new ConcurrentHashMap<>();
        return (h1, h2)->{
            try {
                final boolean h1AcceptableAvailabilityZoneIfStrict = isAcceptableAvailabilityZoneIfStrict(h1.getAvailabilityZone(), replicaSet);
                final boolean h2AcceptableAvailabilityZoneIfStrict = isAcceptableAvailabilityZoneIfStrict(h2.getAvailabilityZone(), replicaSet);
                if (h1AcceptableAvailabilityZoneIfStrict != h2AcceptableAvailabilityZoneIfStrict) {
                    return h1AcceptableAvailabilityZoneIfStrict ? -1 : 1;
                } else {
                    return numberOfProcesses.computeIfAbsent(h1, this::getNumberOfProcesses).compareTo(numberOfProcesses.computeIfAbsent(h2, this::getNumberOfProcesses));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    private int getNumberOfProcesses(SailingAnalyticsHost<String> host) {
        try {
            return Util.size(host.getApplicationProcesses(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception trying to obtain number of processes on host "+host, e);
            return Integer.MAX_VALUE; // probably not a good idea to deploy to this host, so make it look full
        }
    }

    private boolean isAcceptableAvailabilityZone(AvailabilityZone availabilityZone,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws InterruptedException, ExecutionException {
        return !mustBeDifferentAvailabilityZone || isAcceptableAvailabilityZoneIfStrict(availabilityZone, replicaSet);
    }
    
    /**
     * For a replica it is acceptable to be deployed to any AZ other than the master's. For a master, an
     * AZ is acceptable either if no replica exists at all, or no auto-scaling group-managed replicas exist at all and at least
     * one unmanaged replica is in a different AZ, or at least one of the managed replicas is in a different AZ.
     */
    private boolean isAcceptableAvailabilityZoneIfStrict(AvailabilityZone availabilityZone,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws InterruptedException, ExecutionException {
        return (master && isAcceptableAvailabilityZoneForMaster(availabilityZone, replicaSet)) ||
               (!master && !replicaSet.getMaster().getHost().getAvailabilityZone().equals(availabilityZone));
    }

    /**
     * For a master, an AZ is acceptable either if no replica exists at all, or no auto-scaling group-managed replicas
     * exist at all and at least one unmanaged replica is in a different AZ, or at least one of the managed replicas is
     * in a different AZ.
     */
    private boolean isAcceptableAvailabilityZoneForMaster(AvailabilityZone availabilityZone,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws InterruptedException, ExecutionException {
        final boolean result;
        if (Util.isEmpty(replicaSet.getReplicas())) {
            result = true;
        } else {
            boolean hasManagedReplicas = false;
            boolean hasManagedReplicaInDifferentAZ = false;
            boolean hasUnmanagedReplicaInDifferentAZ = false;
            for (final SailingAnalyticsProcess<String> replica : replicaSet.getReplicas()) {
                final boolean isManaged = replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(replicaSet.getAutoScalingGroup()));
                final boolean isInDifferentAZ = !replica.getHost().getAvailabilityZone().equals(availabilityZone);
                hasManagedReplicas = hasManagedReplicas || isManaged;
                hasManagedReplicaInDifferentAZ = hasManagedReplicaInDifferentAZ || (isManaged && isInDifferentAZ);
                hasUnmanagedReplicaInDifferentAZ = hasUnmanagedReplicaInDifferentAZ || (!isManaged && isInDifferentAZ);
            }
            result = hasManagedReplicaInDifferentAZ || (!hasManagedReplicas && hasUnmanagedReplicaInDifferentAZ);
        }
        return result;
    }

    private boolean isArchiveServer(SailingAnalyticsHost<String> host) {
        return host.getInstance().tags().stream().filter(tag->tag.key().equals(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG)
                && tag.value().equals(SharedLandscapeConstants.ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME)).findAny().isPresent();
    }
}
