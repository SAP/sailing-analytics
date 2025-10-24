package com.sap.sailing.landscape.procedures;

import java.util.Optional;
import java.util.logging.Logger;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.orchestration.StartAwsApplicationHost;
import com.sap.sse.landscape.orchestration.Procedure;

/**
 * This launches an EC2 instance with a {@link SailingAnalyticsProcess} automatically started on it. The port
 * configurations, especially for the {@link Builder#getPort() HTTP port}, the {@link Builder#getTelnetPort() telnet
 * port for OSGi console access} and the {@link Builder#getExpeditionPort() "Expedition" UDP port} for this default
 * process can be specified. They default to 8888, 14888, and 2010, respectively.
 * <p>
 *
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 */
public class StartSailingAnalyticsHost<ShardingKey>
extends StartAwsApplicationHost<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>, SailingAnalyticsHost<ShardingKey>>
implements Procedure<ShardingKey>, StartFromSailingAnalyticsImage {
    public static final Logger logger = Logger.getLogger(StartSailingAnalyticsHost.class.getName());
    public final static String INSTANCE_NAME_DEFAULT_PREFIX = "SL ";
    
    /**
     * The following defaults, in addition to the defaults implemented by the more general
     * {@link StartAwsApplicationHost.Builder}, are:
     * <ul>
     * <li>If no {@link #setInstanceName(String) instance name} is provided, the instance name is constructed from the
     * {@link #getServerName() server name} by pre-pending the prefix "SL " (see
     * {@link StartSailingAnalyticsHost#INSTANCE_NAME_DEFAULT_PREFIX}).</li>
     * <li>Uses the latest machine image of the type described by
     * {@link StartSailingAnalyticsHost#IMAGE_TYPE_TAG_VALUE_SAILING} if no explicit
     * {@link #setMachineImage(AmazonMachineImage) machine image is set} and no {@link #setImageType(String) image type
     * is set} of which the latest version would be used otherwise.</li>
     * <li>The {@link #getServerDirectory(Optional) server directory} defaults to {@code /home/sailing/servers/<server-name>}
     * (see {@link ApplicationProcessHost#DEFAULT_SERVER_PATH})</li>
     * <li>If the tag {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} is not set, it defaults to
     * the value equaling the {@link SailingAnalyticsApplicationConfiguration.Builder#setServerName(String) server name}
     * set in the application configuration.</li>
     * </ul>
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<BuilderT extends Builder<BuilderT, T, ShardingKey>,
    T extends StartSailingAnalyticsHost<ShardingKey>, ShardingKey>
    extends StartAwsApplicationHost.Builder<BuilderT, T, ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>, SailingAnalyticsHost<ShardingKey>> {
    }
    
    protected static class BuilderImpl<BuilderT extends Builder<BuilderT, T, ShardingKey>,
    T extends StartSailingAnalyticsHost<ShardingKey>, ShardingKey>
    extends StartAwsApplicationHost.BuilderImpl<BuilderT, T, ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>, SailingAnalyticsHost<ShardingKey>>
    implements Builder<BuilderT, T, ShardingKey> {
        protected BuilderImpl(SailingAnalyticsApplicationConfiguration.Builder<?, ?, ShardingKey> applicationConfigurationBuilder) {
            super(applicationConfigurationBuilder);
        }

        @Override
        protected SailingAnalyticsApplicationConfiguration.BuilderImpl<?, ?, ShardingKey> getApplicationConfigurationBuilder() {
            return (com.sap.sailing.landscape.procedures.SailingAnalyticsApplicationConfiguration.BuilderImpl<?, ?, ShardingKey>) super.getApplicationConfigurationBuilder();
        }

        @Override
        protected String getImageType() {
            return super.getImageType() == null ? SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING : super.getImageType();
        }
        
        @Override
        protected String getInstanceName() {
            return isInstanceNameSet() ? super.getInstanceName() : INSTANCE_NAME_DEFAULT_PREFIX+getApplicationConfigurationBuilder().getServerName();
        }

        @Override
        protected HostSupplier<ShardingKey, SailingAnalyticsHost<ShardingKey>> getHostSupplier() {
            return new SailingAnalyticsHostSupplier<>();
        }
        
        @Override
        protected Optional<Tags> getTags() {
            return Optional.of(
                    Tags.with(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, getApplicationConfigurationBuilder().getServerName())
                        .andAll(super.getTags().orElse(Tags.empty())));
        }

        @Override
        public T build() throws Exception {
            @SuppressWarnings("unchecked")
            final T result = (T) new StartSailingAnalyticsHost<ShardingKey>(this);
            return result;
        }
    }
    
    public static <BuilderT extends Builder<BuilderT, T, ShardingKey>, T extends StartSailingAnalyticsHost<ShardingKey>, ShardingKey>
    BuilderT builder(SailingAnalyticsApplicationConfiguration.Builder<?, ?, ShardingKey> applicationConfigurationBuilder) {
        @SuppressWarnings("unchecked")
        final BuilderT result = (BuilderT) new BuilderImpl<BuilderT, T, ShardingKey>(applicationConfigurationBuilder);
        return result;
    }

    protected StartSailingAnalyticsHost(BuilderImpl<?, ? extends StartSailingAnalyticsHost<ShardingKey>, ShardingKey> builder) throws Exception {
        super(builder);
    }
    
    @Override
    protected SailingAnalyticsApplicationConfiguration<ShardingKey>
    getApplicationConfiguration() {
        return (SailingAnalyticsApplicationConfiguration<ShardingKey>) super.getApplicationConfiguration();
    }

    public SailingAnalyticsProcess<ShardingKey> getSailingAnalyticsProcess() {
        return new SailingAnalyticsProcessImpl<>(getApplicationConfiguration().getPort(), getHost(),
                getApplicationConfiguration().getServerDirectory(), getApplicationConfiguration().getTelnetPort(),
                getApplicationConfiguration().getServerName(), getApplicationConfiguration().getExpeditionPort(),
                getApplicationConfiguration().getIgtimiRiotPort(), getLandscape());
    }
}
