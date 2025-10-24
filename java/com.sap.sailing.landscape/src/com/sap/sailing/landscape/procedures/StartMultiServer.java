package com.sap.sailing.landscape.procedures;

import java.util.Optional;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Duration;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.orchestration.StartEmptyServer;
import com.sap.sse.shared.util.Wait;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;

/**
 * Starts an empty multi-server. The image will cause the {@code /home/sailing/servers/} directory to exist and be
 * empty. A {@link DeployProcessOnMultiServer} procedure needs to be run with the {@link #getHost()} of this procedure
 * telling the host on which to deploy the process.
 * <p>
 * 
 * The implementation specializes the {@link StartEmptyServer} procedure, tags it as a sailing application instance
 * and names it as a "multi-instance" by default.
 * <p>
 * 
 * You want to at least specify an {@link Builder#setInstanceName(String) instance name} and
 * {@link Builder#setInstanceType(InstanceType) instance type}.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 * @param <SailingAnalyticsHost<ShardingKey>>
 */
public class StartMultiServer<ShardingKey>
extends StartEmptyServer<StartMultiServer<ShardingKey>, ShardingKey, SailingAnalyticsHost<ShardingKey>>
implements StartFromSailingAnalyticsImage {
    private Optional<Duration> optionalTimeout;
    
    /**
     * Under all circumstances, this builder will return {@code true} for {@link #isNoShutdown()}, making sure that
     * after the upgrade progress the server does not try to re-boot. Defaults:
     * <ul>
     * <li>The instance name defaults to "Multi-Server"</li>
     * <li>The instance type defaults to {@link InstanceType#I3_XLARGE}</li>
     * <li>The {@link #setImageType(String) image type} defaults to
     * {@link StartFromSailingAnalyticsImage#IMAGE_TYPE_TAG_VALUE_SAILING} ({@code "sailing-analytics-server"}).
     * <li>The tag {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} is set, with the value set to
     * {@code "___multi___"} if no </li>
     * </ul>
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<BuilderT extends Builder<BuilderT, ShardingKey>, ShardingKey>
    extends StartEmptyServer.Builder<BuilderT, StartMultiServer<ShardingKey>, ShardingKey, SailingAnalyticsHost<ShardingKey>> {
    }
    
    protected static class BuilderImpl<BuilderT extends Builder<BuilderT, ShardingKey>, ShardingKey>
    extends StartEmptyServer.BuilderImpl<BuilderT, StartMultiServer<ShardingKey>,
    ShardingKey, SailingAnalyticsHost<ShardingKey>>
    implements Builder<BuilderT, ShardingKey> {
        @Override
        public StartMultiServer<ShardingKey> build() {
            return new StartMultiServer<>(this);
        }

        @Override
        protected String getImageType() {
            return super.getImageType() == null ? SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING : super.getImageType();
        }
        
        @Override
        protected String getInstanceName() {
            final String result;
            if (isInstanceNameSet()) {
                result = super.getInstanceName();
            } else {
                result = SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_DEFAULT_NAME;
            }
            return result;
        }
        
        @Override
        protected HostSupplier<ShardingKey, SailingAnalyticsHost<ShardingKey>> getHostSupplier() {
            final HostSupplier<ShardingKey, SailingAnalyticsHost<ShardingKey>> result;
            if (super.getHostSupplier() == null) {
                result = new SailingAnalyticsHostSupplier<>();
            } else {
                result = super.getHostSupplier();
            }
            return result;
        }
        
        @Override
        protected InstanceType getInstanceType() {
            final InstanceType result;
            if (super.getInstanceType() == null) {
                result = InstanceType.I3_XLARGE;
            } else {
                result = super.getInstanceType();
            }
            return result;
        }
        
        @Override
        protected Optional<Tags> getTags() {
            return Optional.of(super.getTags().orElse(Tags.empty()).and(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE));
        }
        
        /**
         * Make visible in package
         */
        @Override
        protected Optional<Duration> getOptionalTimeout() {
            return super.getOptionalTimeout();
        }
    }
    
    public static <BuilderT extends Builder<BuilderT, ShardingKey>, ShardingKey> Builder<BuilderT, ShardingKey> builder() {
        return new BuilderImpl<>();
    }

    protected StartMultiServer(BuilderImpl<?, ShardingKey> builder) {
        super(builder);
        this.optionalTimeout = builder.getOptionalTimeout();
    }
    
    @Override
    public void run() throws Exception {
        super.run(); // this will trigger the "sailing" init.d script running in the background
        Wait.wait(() -> getHost().getInstance().state().name().equals(InstanceStateName.RUNNING), result->result,
                /* retry on exception */ true, optionalTimeout, Duration.ONE_SECOND.times(10),
                /* no logging */ null, /* no log message */ null); 
    }
}
