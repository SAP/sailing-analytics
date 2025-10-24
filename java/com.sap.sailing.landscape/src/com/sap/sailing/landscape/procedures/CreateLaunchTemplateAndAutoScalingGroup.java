package com.sap.sailing.landscape.procedures;

import java.util.Optional;

import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.AwsApplicationProcess;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.orchestration.AbstractProcedureImpl;
import com.sap.sse.landscape.orchestration.Procedure;

import software.amazon.awssdk.services.ec2.model.InstanceType;

/**
 * For an {@link ApplicationReplicaSet} and a {@link TargetGroup} that represents the application replica set's public
 * target group creates an AWS EC2 Launch Configuration and a corresponding Auto-Scaling Group which by default produces
 * a minimum of one replica, a maximum of 30 replicas, scaling based on the number of requests per target which are
 * supposed to not exceed 15,000 per minute. An {@link AwsApplicationConfiguration} for the replica's config must be
 * provided and is expected to use the same {@link Release} as the master.
 * <p>
 * 
 * The builder for the procedure requires the bearer token for replication permissions and the target group. Other
 * properties are optional.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 */
public class CreateLaunchTemplateAndAutoScalingGroup<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey,MetricsT,ProcessT>>
extends AbstractProcedureImpl<ShardingKey>
implements Procedure<ShardingKey> {
    private static final int DEFAULT_MIN_REPLICAS = 1;
    private static final int DEFAULT_MAX_REPLICAS = 30;
    
    /**
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<ShardingKey, BuilderT extends Builder<ShardingKey, BuilderT, MetricsT, ProcessT>, MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey,MetricsT,ProcessT>>
    extends com.sap.sse.landscape.orchestration.Procedure.Builder<BuilderT, CreateLaunchTemplateAndAutoScalingGroup<ShardingKey, MetricsT, ProcessT>, ShardingKey> {
        BuilderT setKeyName(String keyName);

        BuilderT setReplicaConfiguration(AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> applicationConfiguration);

        BuilderT setInstanceType(InstanceType instanceType);

        BuilderT setImage(AmazonMachineImage<ShardingKey> image);
        
        BuilderT setTags(Tags tags);
        
        /**
         * Set the minimum number of replicas. Defaults to 1 (see {@link CreateLaunchTemplateAndAutoScalingGroup#DEFAULT_MIN_REPLICAS}).
         */
        BuilderT setMinReplicas(int minReplicas);

        /**
         * Set the maximum number of replicas. Defaults to 30 (see {@link CreateLaunchTemplateAndAutoScalingGroup#DEFAULT_MAX_REPLICAS}).
         */
        BuilderT setMaxReplicas(int maxReplicas);

        /**
         * Defines the scaling threshold based on the number of requests per target per minute. Defaults to 15,000 (see
         * {@link AwsAutoScalingGroup#DEFAULT_MAX_REQUESTS_PER_TARGET}).
         */
        BuilderT setMaxRequestsPerTarget(int maxRequestsPerTarget);
    }
    
    protected static class BuilderImpl<ShardingKey, BuilderT extends Builder<ShardingKey, BuilderT, MetricsT, ProcessT>, MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey,MetricsT,ProcessT>>
    extends AbstractProcedureImpl.BuilderImpl<BuilderT, CreateLaunchTemplateAndAutoScalingGroup<ShardingKey, MetricsT, ProcessT>, ShardingKey>
    implements Builder<ShardingKey, BuilderT, MetricsT, ProcessT> {
        private final String replicaSetName;
        private final TargetGroup<ShardingKey> targetGroup;
        private final Region region;
        private String keyName;
        private AmazonMachineImage<ShardingKey> image;
        private InstanceType instanceType;
        private AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration;
        private Optional<Tags> tags;
        private int minReplicas = DEFAULT_MIN_REPLICAS;
        private int maxReplicas = DEFAULT_MAX_REPLICAS;
        private int maxRequestsPerTarget = AwsAutoScalingGroup.DEFAULT_MAX_REQUESTS_PER_TARGET;

        public BuilderImpl(AwsLandscape<ShardingKey> landscape, Region region, String replicaSetName,
                TargetGroup<ShardingKey> targetGroup) {
            this.replicaSetName = replicaSetName;
            this.targetGroup = targetGroup;
            this.region = region;
            setLandscape(landscape);
        }
        
        @Override
        public BuilderT setKeyName(String keyName) {
            this.keyName = keyName;
            return self();
        }

        @Override
        public BuilderT setImage(AmazonMachineImage<ShardingKey> image) {
            this.image = image;
            return self();
        }

        @Override
        public BuilderT setInstanceType(InstanceType instanceType) {
            this.instanceType = instanceType;
            return self();
        }

        @Override
        public BuilderT setReplicaConfiguration(AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration) {
            this.replicaConfiguration = replicaConfiguration;
            return self();
        }

        protected String getReplicaSetName() {
            return replicaSetName;
        }

        protected int getMinReplicas() {
            return minReplicas;
        }

        @Override
        public BuilderT setMinReplicas(int minReplicas) {
            this.minReplicas = minReplicas;
            return self();
        }

        protected int getMaxReplicas() {
            return maxReplicas;
        }

        @Override
        public BuilderT setMaxReplicas(int maxReplicas) {
            this.maxReplicas = maxReplicas;
            return self();
        }

        protected int getMaxRequestsPerTarget() {
            return maxRequestsPerTarget;
        }

        @Override
        public BuilderT setMaxRequestsPerTarget(int maxRequestsPerTarget) {
            this.maxRequestsPerTarget = maxRequestsPerTarget;
            return self();
        }

        protected TargetGroup<ShardingKey> getTargetGroup() {
            return targetGroup;
        }

        protected Region getRegion() {
            return region;
        }

        protected String getKeyName() {
            return keyName;
        }

        protected AmazonMachineImage<ShardingKey> getImage() {
            return image;
        }
        
        protected InstanceType getInstanceType() {
            return instanceType;
        }
        
        protected AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> getReplicaConfiguration() {
            return replicaConfiguration;
        }

        protected Optional<Tags> getTags() {
            return tags;
        }

        @Override
        public BuilderT setTags(Tags tags) {
            this.tags = Optional.ofNullable(tags);
            return self();
        }

        @Override
        public CreateLaunchTemplateAndAutoScalingGroup<ShardingKey, MetricsT, ProcessT> build() throws Exception {
            return new CreateLaunchTemplateAndAutoScalingGroup<>(this);
        }
    }
    
    private final String replicaSetName;
    private final TargetGroup<ShardingKey> targetGroup;
    private final Region region;
    private final String keyName;
    private final InstanceType instanceType;
    private final String imageId;
    private final AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration;
    private final Optional<Tags> tags;
    private final int minReplicas;
    private final int maxReplicas;
    private final int maxRequestsPerTarget;
    
    public static <ShardingKey, BuilderT extends Builder<ShardingKey, BuilderT, MetricsT, ProcessT>, MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey,MetricsT,ProcessT>>
    Builder<ShardingKey, BuilderT, MetricsT, ProcessT> builder(
            AwsLandscape<ShardingKey> landscape, Region region,
            String replicaSetName,
            TargetGroup<ShardingKey> targetGroup) {
        return new BuilderImpl<>(landscape, region, replicaSetName, targetGroup);
    }
    
    protected <BuilderT extends Builder<ShardingKey, BuilderT, MetricsT, ProcessT>>
    CreateLaunchTemplateAndAutoScalingGroup(BuilderImpl<ShardingKey, BuilderT, MetricsT, ProcessT> builder) {
        super(builder);
        this.region = builder.getRegion();
        this.replicaSetName = builder.getReplicaSetName();
        this.targetGroup = builder.getTargetGroup();
        this.keyName = builder.getKeyName();
        this.imageId = builder.getImage().getId();
        this.instanceType = builder.getInstanceType();
        this.replicaConfiguration = builder.getReplicaConfiguration();
        this.tags = builder.getTags();
        this.minReplicas = builder.getMinReplicas();
        this.maxReplicas = builder.getMaxReplicas();
        this.maxRequestsPerTarget = builder.getMaxRequestsPerTarget();
    }
    
    @Override
    public AwsLandscape<ShardingKey> getLandscape() {
        return (AwsLandscape<ShardingKey>) super.getLandscape();
    }

    @Override
    public void run() throws Exception {
        getLandscape().createLaunchTemplateAndAutoScalingGroup(region, replicaSetName, tags,
                targetGroup, keyName, instanceType, imageId, replicaConfiguration, minReplicas, maxReplicas, maxRequestsPerTarget);
    }
}
